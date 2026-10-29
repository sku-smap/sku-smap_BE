package com.smap.domain.graph.controller;

import org.springframework.http.MediaType;

import com.smap.domain.graph.dto.response.CampusMapResponseDTO;
import com.smap.domain.graph.service.CampusMapService;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 출발·도착 선택 없이 실제 사진 연결을 따라 이동할 캠퍼스 둘러보기 API이다. */
@RestController
@RequestMapping(value = "/api/v1/campus-map", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CampusMapController {
    private final CampusMapService campusMapService;

    /** 사진 자체를 다운로드하지 않고 임시 URL과 전체 그래프 메타데이터를 반환한다. */
    @GetMapping
    @Operation(summary = "캠퍼스 전체 둘러보기", description = "전체 노드·사진 URL·노드별 장소·방향 간선을 반환합니다. 엘리베이터 구간도 통합하지 않으며 노드 ID로 사진을 찾아 이동합니다.")
    public ResponseEntity<ApiResponse<CampusMapResponseDTO>> load() {
        return ResponseEntity.ok(ApiResponse.success(campusMapService.load()));
    }
}
