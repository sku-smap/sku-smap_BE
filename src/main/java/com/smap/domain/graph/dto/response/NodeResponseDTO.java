package com.smap.domain.graph.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.smap.domain.graph.entity.Node;
import com.smap.domain.graph.entity.PhotoType;

/** 전체 촬영 지점 조회용 정보이며 사진 URL은 경로 조회 시 발급한다. */
public record NodeResponseDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String nodeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String building,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String floorLabel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String originalFilename,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) PhotoType photoType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String buildingId
) {
    /** 경로 응답과 동일한 방식으로 원본 사진명에서 표시 이름을 구성한다. */
    public static NodeResponseDTO from(Node node) {
        String title = node.getOriginalFilename().replaceFirst("(?i)\\.jpe?g$", "").replace('_', ' ');
        return new NodeResponseDTO(node.getNodeId(), node.getBuilding(), node.getFloorLabel(),
                title, node.getOriginalFilename(), PhotoType.PANORAMA, node.getBuildingId());
    }
}
