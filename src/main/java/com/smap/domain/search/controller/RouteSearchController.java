package com.smap.domain.search.controller;

import com.smap.domain.search.dto.request.RouteSearchRequestDTO;
import com.smap.domain.search.dto.request.VoiceRouteSearchRequestDTO;
import com.smap.domain.search.dto.response.RouteSearchResponseDTO;
import com.smap.domain.search.service.SearchService;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/route-searches", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Search", description = "AI 장소 검색 API (텍스트·음성 → 출발지/목적지 후보)")
public class RouteSearchController {

    private static final String ROUTE_SEARCH_SUMMARY = "문장 길찾기 검색 API (텍스트·음성)";
    private static final String ROUTE_SEARCH_DESCRIPTION = "\"북악관에서 청운관 어떻게 가?\"처럼 한 문장에서 출발지와 목적지를 나눠, "
            + "각각의 후보를 점수 순으로 반환하는 API.\n\n"
            + "- 텍스트: Content-Type application/json, RouteSearchRequestDTO\n"
            + "- 음성: Content-Type multipart/form-data, VoiceRouteSearchRequestDTO (audio 1MB·15초 이하)\n\n"
            + "origin·destination은 기존 장소 검색 API 응답과 같은 형식이며, 각자의 searchId로 후보 선택 API를 호출합니다. "
            + "출발지를 못 찾은 문장(mode=DESTINATION_ONLY)은 origin이 null이므로, 기존 장소 검색 API(target=ORIGIN)로 따로 물어봐 주세요.";

    private final SearchService searchService;

    @Operation(summary = ROUTE_SEARCH_SUMMARY, description = ROUTE_SEARCH_DESCRIPTION)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RouteSearchResponseDTO>> search(@Valid @RequestBody RouteSearchRequestDTO request) {
        return created(searchService.searchRoute(request));
    }

    @Operation(summary = ROUTE_SEARCH_SUMMARY, description = ROUTE_SEARCH_DESCRIPTION)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RouteSearchResponseDTO>> searchVoice(
            @Valid @ModelAttribute VoiceRouteSearchRequestDTO request) {
        return created(searchService.searchRouteVoice(request));
    }

    private ResponseEntity<ApiResponse<RouteSearchResponseDTO>> created(RouteSearchResponseDTO response) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("201", "검색 후보를 만들었습니다.", response));
    }
}
