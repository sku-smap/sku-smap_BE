package com.smap.domain.place.controller;

import com.smap.domain.place.dto.response.PlaceResponseDTO;
import com.smap.domain.place.service.PlaceService;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 출발·도착 장소 선택에 사용할 검색 API이다. */
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceService placeService;

    /** 실제 노드가 연결된 장소만 반환하며 결과가 없으면 빈 배열이다. */
    @GetMapping
    @Operation(summary = "장소 검색", description = "이름·별명·건물·호실의 부분 일치 검색. 검색어 생략 시 전체 조회. arriveNodeId를 길찾기에 사용합니다.")
    public ResponseEntity<ApiResponse<List<PlaceResponseDTO>>> search(
            @Parameter(description = "검색어. 생략하거나 공백이면 전체 장소", example = "도시공학과 실습실")
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(placeService.search(keyword)));
    }
}
