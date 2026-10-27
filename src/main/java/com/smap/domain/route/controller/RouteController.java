package com.smap.domain.route.controller;

import com.smap.domain.route.dto.request.RouteRequestDTO;
import com.smap.domain.route.dto.response.RouteResponseDTO;
import com.smap.domain.route.service.RouteService;
import com.smap.global.common.response.ApiResponse;
import com.smap.global.external.s3.PhotoUrlService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 입력 검증 후 길찾기 서비스에 위임하고 공통 응답 형식으로 결과를 반환한다. */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {
    /** 그래프 조회와 최단경로 계산을 담당하는 서비스이다. */
    private final RouteService routeService;
    /** 비공개 사진 조회용 임시 URL 발급 서비스. */
    private final PhotoUrlService photoUrlService;

    /** 기본 야외경로 우선 또는 선택 조건에 맞는 경로와 장소 정보를 반환한다. */
    @Operation(summary = "경로 조회", description = "option은 OUTDOOR_FIRST(기본), COMFORTABLE, SHORTEST, INDOOR_UNDERGROUND_FIRST 중 하나입니다. 최단거리는 모든 간선 비용 1입니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponseDTO>> find(@Valid @RequestBody RouteRequestDTO request) {
        RouteResponseDTO response = RouteResponseDTO.from(
                routeService.find(request.fromNodeId(), request.toNodeId(), request.option()), photoUrlService::createUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
