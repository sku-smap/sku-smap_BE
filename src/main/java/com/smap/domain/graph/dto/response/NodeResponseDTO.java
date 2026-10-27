package com.smap.domain.graph.dto.response;

import com.smap.domain.graph.entity.Node;

/** 전체 촬영 지점 조회용 정보이며 사진 URL은 경로 조회 시 발급한다. */
public record NodeResponseDTO(String nodeId, String building, String floorLabel,
                              String title, String originalFilename) {
    /** 경로 응답과 동일한 방식으로 원본 사진명에서 표시 이름을 구성한다. */
    public static NodeResponseDTO from(Node node) {
        String title = node.getOriginalFilename().replaceFirst("(?i)\\.jpe?g$", "").replace('_', ' ');
        return new NodeResponseDTO(node.getNodeId(), node.getBuilding(), node.getFloorLabel(),
                title, node.getOriginalFilename());
    }
}
