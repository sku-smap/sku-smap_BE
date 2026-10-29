package com.smap.domain.search.controller;

import com.smap.domain.search.dto.request.SearchRequestDTO;
import com.smap.domain.search.dto.request.SelectionRequestDTO;
import com.smap.domain.search.dto.request.VoiceSearchRequestDTO;
import com.smap.domain.search.dto.response.SearchResponseDTO;
import com.smap.domain.search.service.SearchService;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/v1/searches", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Search", description = "AI 장소 검색 API (텍스트·음성 → 출발지/목적지 후보)")
public class SearchController {

    private static final String SEARCH_SUMMARY = "장소 검색 API (텍스트·음성)";
    private static final String SEARCH_DESCRIPTION = "문장 또는 녹음을 기반으로, 출발지(ORIGIN) 또는 목적지(DESTINATION) 후보를 점수 순으로 반환하는 API.\n\n"
            + "- 텍스트: Content-Type application/json, SearchRequestDTO\n"
            + "- 음성: Content-Type multipart/form-data, VoiceSearchRequestDTO (audio 1MB·15초 이하). "
            + "인식된 문장은 transcript, 사용한 엔진은 sttEngine에 담깁니다.\n\n"
            + "후보가 없으면 에러가 아니라 status=NOT_FOUND와 빈 candidates를 반환합니다. "
            + "고른 후보의 nodeId를 경로 조회 API의 fromNodeId/toNodeId로 사용하고, "
            + "routeOption이 있으면 경로 조회 API의 option으로 넘겨 주세요.";

    private final SearchService searchService;

    @Operation(summary = SEARCH_SUMMARY, description = SEARCH_DESCRIPTION)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SearchResponseDTO>> search(@Valid @RequestBody SearchRequestDTO request) {
        return created(searchService.search(request));
    }

    @Operation(summary = SEARCH_SUMMARY, description = SEARCH_DESCRIPTION)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SearchResponseDTO>> searchVoice(
            @Valid @ModelAttribute VoiceSearchRequestDTO request) {
        return created(searchService.searchVoice(request));
    }

    @Operation(summary = "후보 선택 API",
            description = "사용자가 검색 결과에서 고른 후보를 기록하는 API. "
                    + "candidateId는 해당 검색 응답의 candidates 안에 있던 값이어야 합니다.")
    @PutMapping("/{searchId}/selection")
    public ResponseEntity<ApiResponse<Void>> select(
            @Parameter(description = "검색 응답의 searchId", example = "12") @PathVariable Long searchId,
            @Valid @RequestBody SelectionRequestDTO request) {
        searchService.select(searchId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<SearchResponseDTO>> created(SearchResponseDTO response) {
        return ResponseEntity.created(URI.create("/api/v1/searches/" + response.getSearchId()))
                .body(ApiResponse.success("201", "검색 후보를 만들었습니다.", response));
    }
}
