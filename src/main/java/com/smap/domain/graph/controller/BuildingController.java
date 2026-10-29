package com.smap.domain.graph.controller;

import org.springframework.http.MediaType;
import com.smap.domain.graph.dto.response.BuildingResponseDTO;
import com.smap.domain.graph.service.BuildingService;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프론트의 건물 ID·표시명 대응표 조회 API이다. */
@RestController
@RequestMapping(value = "/api/v1/buildings", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingService buildingService;

    @GetMapping
    @Operation(summary = "건물 ID·표시명 목록 조회",
            description = "노드·장소에 등록된 건물 및 야외 구역을 ID 순으로 반환합니다. 같은 ID·표시명은 중복 제거하며, 데이터가 없으면 빈 배열입니다.")
    public ResponseEntity<ApiResponse<List<BuildingResponseDTO>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(buildingService.findAll()));
    }
}
