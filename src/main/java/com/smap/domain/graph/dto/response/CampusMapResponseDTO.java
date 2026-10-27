package com.smap.domain.graph.dto.response;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.place.dto.response.PlaceResponseDTO;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 둘러보기용 전체 사진 노드·방향 간선·장소 정보를 한 번에 반환한다. */
public record CampusMapResponseDTO(List<MapNodeDTO> nodes, List<MapEdgeDTO> edges,
                                   int totalNodes, int totalEdges, int totalPlaces) {
    /** 캐시 원본을 수정하지 않고 현재 요청의 사진 URL을 붙인다. */
    public CampusMapResponseDTO withImageUrls(Function<String, String> imageUrls) {
        var signedNodes = nodes.stream().map(node -> new MapNodeDTO(node.nodeId(), node.building(),
                node.floorLabel(), node.title(), node.originalFilename(), imageUrls.apply(node.originalFilename()),
                node.places())).toList();
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
                        node.originalFilename(), imageUrls.apply(node.originalFilename()),
                        List.copyOf(byNode.getOrDefault(node.nodeId(), List.of())))).toList();
        var edges = graph.outgoing().values().stream().flatMap(List::stream)
                .sorted(Comparator.comparing(GraphSnapshot.Segment::fromNodeId)
                        .thenComparing(GraphSnapshot.Segment::toNodeId)
                        .thenComparing(edge -> edge.moveType() == null ? "" : edge.moveType().name()))
                .map(edge -> new MapEdgeDTO(edge.fromNodeId(), edge.toNodeId(), edge.moveType(), edge.description()))
                .toList();
        int totalPlaces = nodes.stream().mapToInt(node -> node.places().size()).sum();
        return new CampusMapResponseDTO(nodes, edges, nodes.size(), edges.size(), totalPlaces);
    }

    /** 안내 문구는 이동 방향에 따라 달라지므로 노드가 아닌 간선에 제공한다. */
    public record MapNodeDTO(String nodeId, String building, String floorLabel, String title,
                              String originalFilename, String imageUrl, List<PlaceResponseDTO> places) {}

    /** 같은 노드 쌍의 계단·엘리베이터는 별도 유지하며 역방향을 추가하지 않는다. */
    public record MapEdgeDTO(String fromNodeId, String toNodeId, MoveType moveType, String description) {}
}
