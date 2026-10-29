package com.smap.domain.graph.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.smap.domain.graph.entity.PhotoType;
import com.smap.domain.graph.service.GraphSnapshot;
import java.util.List;

/** 전체 촬영 지점과 이동 선택지를 제공하며 사진 URL은 포함하지 않는다. */
public record NodeResponseDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String nodeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String building,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String floorLabel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String originalFilename,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) PhotoType photoType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String buildingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "현재 노드에서 출발하는 방향 간선. 없으면 빈 배열") List<CampusMapResponseDTO.MapEdgeDTO> outgoingEdges
) {
    /** 경로 응답과 동일한 방식으로 원본 사진명에서 표시 이름을 구성한다. */
    public static NodeResponseDTO from(GraphSnapshot.Point node, List<GraphSnapshot.Segment> edges) {
        return new NodeResponseDTO(node.nodeId(), node.building(), node.floorLabel(),
                node.title(), node.originalFilename(), PhotoType.PANORAMA, node.buildingId(),
                edges.stream().map(CampusMapResponseDTO.MapEdgeDTO::from).toList());
    }
}
