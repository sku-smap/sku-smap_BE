package com.smap.domain.graph.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.entity.PhotoType;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.place.dto.response.PlaceResponseDTO;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 둘러보기용 전체 사진 노드·방향 간선·장소 정보를 한 번에 반환한다. */
public record CampusMapResponseDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) List<MapNodeDTO> nodes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) List<MapEdgeDTO> edges,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) int totalNodes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) int totalEdges,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) int totalPlaces
) {
    /** 캐시 원본을 수정하지 않고 현재 요청의 사진 URL을 붙인다. */
    public CampusMapResponseDTO withImageUrls(Function<String, String> imageUrls,
                                              Function<String, String> thumbnailUrls) {
        var signedNodes = nodes.stream().map(node -> new MapNodeDTO(node.nodeId(), node.building(),
                node.floorLabel(), node.title(), node.originalFilename(), imageUrls.apply(node.originalFilename()),
                thumbnailUrls.apply(node.originalFilename()), node.places(), node.photoType(),
                node.buildingId())).toList();
        return new CampusMapResponseDTO(signedNodes, edges, totalNodes, totalEdges, totalPlaces);
    }

    /** 경로 요약 없이 원본 그래프를 사용하며 사진 URL은 노드당 한 번 발급한다. */
    public static CampusMapResponseDTO from(GraphSnapshot graph, List<PlaceResponseDTO> places,
                                            Function<String, String> imageUrls) {
        var byNode = places.stream()
                .filter(place -> place.arriveNodeId() != null && graph.nodes().containsKey(place.arriveNodeId()))
                .sorted(Comparator.comparing(PlaceResponseDTO::name).thenComparing(PlaceResponseDTO::placeId))
                .collect(Collectors.groupingBy(PlaceResponseDTO::arriveNodeId));
        var nodes = graph.nodes().values().stream().sorted(Comparator.comparing(GraphSnapshot.Point::nodeId))
                .map(node -> new MapNodeDTO(node.nodeId(), node.building(), node.floorLabel(), node.title(),
                        node.originalFilename(), imageUrls.apply(node.originalFilename()), null,
                        List.copyOf(byNode.getOrDefault(node.nodeId(), List.of())), PhotoType.PANORAMA,
                        node.buildingId())).toList();
        var edges = graph.outgoing().values().stream().flatMap(List::stream)
                .sorted(Comparator.comparing(GraphSnapshot.Segment::fromNodeId)
                        .thenComparing(GraphSnapshot.Segment::toNodeId)
                        .thenComparing(edge -> edge.moveType() == null ? "" : edge.moveType().name()))
                .map(MapEdgeDTO::from)
                .toList();
        int totalPlaces = nodes.stream().mapToInt(node -> node.places().size()).sum();
        return new CampusMapResponseDTO(nodes, edges, nodes.size(), edges.size(), totalPlaces);
    }

    /** 안내 문구는 이동 방향에 따라 달라지므로 노드가 아닌 간선에 제공한다. */
    public record MapNodeDTO(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String nodeId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String building,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String floorLabel,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String title,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String originalFilename,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String imageUrl,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                    description = "목록용 썸네일(320×213 JPG) 임시 URL. 썸네일이 없으면 null") String thumbnailUrl,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) List<PlaceResponseDTO> places,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) PhotoType photoType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String buildingId
    ) {}

    /** 같은 노드 쌍의 계단·엘리베이터는 별도 유지하며 역방향을 추가하지 않는다. */
    public record MapEdgeDTO(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String fromNodeId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String toNodeId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) MoveType moveType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String description,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "출발 사진 기준 화살표 수평 각도. 0 이상 360 미만이며 미지정은 null") Double arrowYawDeg
    ) {
        public static MapEdgeDTO from(GraphSnapshot.Segment edge) {
            return new MapEdgeDTO(edge.fromNodeId(), edge.toNodeId(), edge.moveType(), edge.description(), edge.arrowYawDeg());
        }
    }
}
