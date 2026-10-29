package com.smap.domain.graph.controller;

import org.springframework.http.MediaType;

import com.smap.domain.graph.dto.response.NodeResponseDTO;
import com.smap.domain.graph.service.NodeService;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 전체 촬영 노드 목록을 제공한다. */
@RestController
@RequestMapping(value = "/api/v1/nodes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NodeController {
    private final NodeService nodeService;

    /** 반환한 nodeId를 출발·도착 ID로 선택할 수 있다. */
    @GetMapping
    @Operation(summary = "전체 노드 조회", description = "노드 ID 오름차순으로 전체 촬영 지점을 반환합니다. 사진 URL은 길찾기 API에서 발급합니다.")
    public ResponseEntity<ApiResponse<List<NodeResponseDTO>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(nodeService.findAll()));
    }
}
