package com.smap.domain.search.service;

import com.smap.domain.search.config.SearchProperties;
import com.smap.domain.search.dto.response.EmbeddingJobResponseDTO;
import com.smap.domain.search.model.DocType;
import com.smap.domain.search.model.EmbeddingTarget;
import com.smap.domain.search.model.SearchDocument;
import com.smap.domain.search.repository.SearchDocumentRepository;
import com.smap.global.external.ai.properties.AiProperties;
import com.smap.global.external.ai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingJobService {

    private final SearchDocumentBuilder searchDocumentBuilder;
    private final SearchDocumentRepository searchDocumentRepository;
    private final EmbeddingService embeddingService;
    private final AiProperties aiProperties;
    private final SearchProperties searchProperties;

    public EmbeddingJobResponseDTO run(EmbeddingTarget target, boolean force) {
        long startedAt = System.currentTimeMillis();
        refresh(DocType.PLACE, searchDocumentBuilder.buildPlaces());
        refresh(DocType.NODE, searchDocumentBuilder.buildNodes());
        refresh(DocType.SERVICE, searchDocumentBuilder.buildServices());

        Map<DocType, Integer> updated = new EnumMap<>(DocType.class);
        if (aiProperties.hasApiKey()) {
            List<DocType> types = target == EmbeddingTarget.ALL
                    ? List.of(DocType.values())
                    : List.of(DocType.valueOf(target.name()));
            List<Map<String, Object>> pending = searchDocumentRepository.findPending(types, force);
            int batchSize = searchProperties.getEmbeddingBatchSize();
            for (int from = 0; from < pending.size(); from += batchSize) {
                List<Map<String, Object>> batch = pending.subList(from, Math.min(from + batchSize, pending.size()));
                List<float[]> vectors = embeddingService.embed(batch.stream().map(row -> (String) row.get("content")).toList());
                searchDocumentRepository.updateEmbeddings(
                        batch.stream().map(row -> ((Number) row.get("doc_id")).longValue()).toList(),
                        vectors, aiProperties.getEmbeddingModel());
                batch.forEach(row -> updated.merge(DocType.valueOf((String) row.get("doc_type")), 1, Integer::sum));
            }
        }

        return EmbeddingJobResponseDTO.builder()
                .documentCount(searchDocumentRepository.count())
                .serviceUpdated(updated.getOrDefault(DocType.SERVICE, 0))
                .placeUpdated(updated.getOrDefault(DocType.PLACE, 0))
                .nodeUpdated(updated.getOrDefault(DocType.NODE, 0))
                .model(aiProperties.hasApiKey()
                        ? aiProperties.getEmbeddingModel() + " (" + aiProperties.getEmbeddingDimensions() + "차원)"
                        : null)
                .elapsedMs(System.currentTimeMillis() - startedAt)
                .build();
    }

    private void refresh(DocType type, List<SearchDocument> documents) {
        searchDocumentRepository.upsert(documents);
        searchDocumentRepository.deleteMissing(type, documents.stream().map(SearchDocument::refId).toList());
    }
}
