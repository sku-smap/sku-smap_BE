package com.smap.domain.search.service;

import com.smap.domain.search.config.SearchProperties;
import com.smap.domain.search.dto.request.SearchRequestDTO;
import com.smap.domain.search.dto.request.SelectionRequestDTO;
import com.smap.domain.search.dto.request.VoiceSearchRequestDTO;
import com.smap.domain.search.dto.response.CandidateDTO;
import com.smap.domain.search.dto.response.SearchResponseDTO;
import com.smap.domain.search.entity.SearchLog;
import com.smap.domain.search.exception.SearchErrorCode;
import com.smap.domain.search.model.CandidateType;
import com.smap.domain.search.model.DocType;
import com.smap.domain.search.model.DocumentHit;
import com.smap.domain.search.model.InputType;
import com.smap.domain.search.model.MatchedBy;
import com.smap.domain.search.model.ParsedQuery;
import com.smap.domain.search.model.SearchStatus;
import com.smap.domain.search.model.SearchTarget;
import com.smap.domain.search.model.ServicePlace;
import com.smap.domain.search.repository.SearchDocumentRepository;
import com.smap.domain.search.repository.SearchLogRepository;
import com.smap.global.common.exception.GeneralException;
import com.smap.global.external.ai.properties.AiProperties;
import com.smap.global.external.ai.service.EmbeddingService;
import com.smap.global.external.stt.dto.SttResult;
import com.smap.global.external.stt.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_QUERY_LENGTH = 200;

    private final QueryParser queryParser;
    private final SearchDocumentRepository searchDocumentRepository;
    private final SearchLogRepository searchLogRepository;
    private final EmbeddingService embeddingService;
    private final AiProperties aiProperties;
    private final SearchProperties searchProperties;
    private final SttService sttService;
    private final SttPromptProvider sttPromptProvider;

    public SearchResponseDTO search(SearchRequestDTO request) {
        String query = request.getQuery();
        if (query == null || query.isBlank() || query.strip().length() > MAX_QUERY_LENGTH) {
            throw new GeneralException(SearchErrorCode.INVALID_QUERY);
        }
        return search(request.getTarget(), query, request.limitOrDefault(), null, System.currentTimeMillis());
    }

    public SearchResponseDTO searchVoice(VoiceSearchRequestDTO request) {
        long startedAt = System.currentTimeMillis();
        SttResult stt = sttService.transcribe(request.getAudio(), request.getDurationMs(), sttPromptProvider.prompt());
        return search(request.getTarget(), stt.text(), request.limitOrDefault(), stt, startedAt);
    }

    private SearchResponseDTO search(SearchTarget target, String query, int limit, SttResult stt, long startedAt) {
        ParsedQuery parsed = queryParser.parse(query, target);

        List<DocType> types = target == SearchTarget.ORIGIN
                ? List.of(DocType.PLACE, DocType.NODE, DocType.SERVICE)
                : List.of(DocType.PLACE, DocType.SERVICE);
        List<DocumentHit> hits = searchDocumentRepository.findHits(types, parsed.normalized(), parsed.core(),
                embedQuietly(parsed.normalized()), searchProperties.getSemanticWeight(),
                searchProperties.getKeywordWeight(), searchProperties.getPoolSize()).stream()
                .filter(hit -> hit.score() >= searchProperties.getThreshold())
                .toList();
        List<CandidateDTO> candidates = toCandidates(target, hits).stream()
                .limit(limit)
                .toList();

        SearchStatus status = candidates.isEmpty() ? SearchStatus.NOT_FOUND : SearchStatus.CANDIDATES;
        SearchLog searchLog = searchLogRepository.save(SearchLog.builder()
                .target(target)
                .inputType(stt == null ? InputType.TEXT : InputType.VOICE)
                .queryText(query)
                .normalizedQuery(parsed.normalized())
                .transcript(stt == null ? null : stt.text())
                .sttEngine(stt == null ? null : stt.engine().name())
                .routeOption(parsed.routeOption())
                .status(status)
                .candidateIds(candidates.stream().map(CandidateDTO::getCandidateId).toList())
                .latencyMs((int) (System.currentTimeMillis() - startedAt))
                .build());
        return SearchResponseDTO.of(searchLog, message(target, status), candidates);
    }

    @Transactional
    public void select(Long searchId, SelectionRequestDTO request) {
        SearchLog searchLog = searchLogRepository.findById(searchId)
                .orElseThrow(() -> new GeneralException(SearchErrorCode.SEARCH_NOT_FOUND));
        if (!searchLog.hasCandidate(request.getCandidateId())) {
            throw new GeneralException(SearchErrorCode.NOT_A_CANDIDATE);
        }
        searchLog.select(request.getCandidateId());
    }

    private List<CandidateDTO> toCandidates(SearchTarget target, List<DocumentHit> hits) {
        Map<String, List<ServicePlace>> servicePlaces = searchDocumentRepository.findServicePlaces(hits.stream()
                        .filter(hit -> hit.docType() == DocType.SERVICE)
                        .map(DocumentHit::refId)
                        .toList()).stream()
                .collect(Collectors.groupingBy(ServicePlace::serviceId));

        Map<String, CandidateDTO> candidates = new LinkedHashMap<>();
        for (DocumentHit hit : hits) {
            MatchedBy matchedBy = matchedBy(hit);
            double score = Math.round(hit.score() * 1000) / 1000.0;
            if (hit.docType() == DocType.SERVICE) {
                for (ServicePlace place : servicePlaces.getOrDefault(hit.refId(), List.of())) {
                    put(target, candidates, CandidateDTO.builder()
                            .candidateId("place:" + place.placeId())
                            .type(CandidateType.PLACE)
                            .placeId(place.placeId())
                            .nodeId(place.nodeId())
                            .name(place.name())
                            .displaySub(place.displaySub())
                            .building(place.building())
                            .floorLabel(place.floorLabel())
                            .kind(place.kind())
                            .reason((target == SearchTarget.ORIGIN ? "근처 시설: " : "업무: ") + place.serviceName())
                            .score(score)
                            .matchedBy(matchedBy)
                            .build());
                }
            } else if (hit.nodeId() != null) {
                boolean isPlace = hit.docType() == DocType.PLACE;
                put(target, candidates, CandidateDTO.builder()
                        .candidateId((isPlace ? "place:" : "node:") + hit.refId())
                        .type(isPlace ? CandidateType.PLACE : CandidateType.NODE)
                        .placeId(isPlace ? hit.refId() : null)
                        .nodeId(hit.nodeId())
                        .name(hit.displayName())
                        .displaySub(hit.displaySub())
                        .building(hit.building())
                        .floorLabel(hit.floorLabel())
                        .kind(hit.kind())
                        .reason(isPlace ? null : "사진 지점")
                        .score(score)
                        .matchedBy(matchedBy)
                        .build());
            }
        }
        return candidates.values().stream()
                .sorted(Comparator.comparingDouble(CandidateDTO::getScore).reversed())
                .toList();
    }

    private void put(SearchTarget target, Map<String, CandidateDTO> candidates, CandidateDTO candidate) {
        String key = target == SearchTarget.ORIGIN ? candidate.getNodeId() : candidate.getCandidateId();
        CandidateDTO existing = candidates.get(key);
        if (existing == null || existing.getScore() < candidate.getScore()) {
            candidates.put(key, candidate);
        }
    }

    private MatchedBy matchedBy(DocumentHit hit) {
        boolean keyword = hit.keywordScore() >= searchProperties.getKeywordHit();
        boolean semantic = hit.semanticScore() != null && hit.semanticScore() >= searchProperties.getSemanticHit();
        if (keyword && semantic) {
            return MatchedBy.BOTH;
        }
        return semantic ? MatchedBy.SEMANTIC : MatchedBy.KEYWORD;
    }

    private float[] embedQuietly(String text) {
        if (!aiProperties.hasApiKey()) {
            return null;
        }
        try {
            return embeddingService.embed(text);
        } catch (RuntimeException e) {
            log.warn("질문 임베딩 실패, 글자 점수만 사용: {}", e.getMessage());
            return null;
        }
    }

    private String message(SearchTarget target, SearchStatus status) {
        if (status == SearchStatus.NOT_FOUND) {
            return "찾지 못했어요. 다른 말로 다시 말해 주세요.";
        }
        return target == SearchTarget.ORIGIN ? "어디에 계신지 골라 주세요." : "어디로 갈지 골라 주세요.";
    }
}
