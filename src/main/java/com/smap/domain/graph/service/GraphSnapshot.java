package com.smap.domain.graph.service;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.exception.GraphErrorCode;
import com.smap.domain.graph.exception.GraphException;
import java.util.*;

/**
 * 한 번의 탐색에서 사용하는 변경 불가능한 방향 그래프이다.
 * nodes는 ID별 촬영 지점, outgoing은 출발 노드별 간선 목록이다.
 * 같은 노드 사이의 다른 이동 유형은 각각의 간선으로 유지한다.
 */
public record GraphSnapshot(Map<String, Point> nodes, Map<String, List<Segment>> outgoing) {
    /** 외부 컬렉션을 복사하고 간선의 노드 참조를 검증한다. */
    public GraphSnapshot {
        nodes = Map.copyOf(nodes);
        Map<String, Point> nodeCopy = nodes;
        Map<String, List<Segment>> copy = new HashMap<>();
        outgoing.forEach((id, edges) -> {
            if (!nodesContain(id, edges, nodeCopy)) {
                throw new GraphException(GraphErrorCode.INVALID_NODE_REFERENCE);
            }
            copy.put(id, List.copyOf(edges));
        });
        outgoing = Map.copyOf(copy);
    }

    /** 인접 목록의 키와 간선 출발지가 일치하며 양 끝 노드가 존재하는지 검사한다. */
    private static boolean nodesContain(String id, List<Segment> edges, Map<String, Point> nodes) {
        return nodes.containsKey(id) && edges.stream().allMatch(edge ->
                id.equals(edge.fromNodeId()) && nodes.containsKey(edge.toNodeId()));
    }

    /** DB의 촬영 지점 정보이며 공간 분류나 선호 비용은 계산하지 않는다. */
    public record Point(String nodeId, String building, String floorLabel,
                        String title, String originalFilename) {}

    /** CSV의 양 끝 노드·이동 유형·안내 문구만 사용하며 시간은 포함하지 않는다. */
    public record Segment(String fromNodeId, String toNodeId, MoveType moveType, String description) {
        /** 원본 이동 유형의 빈 값은 허용하며 양 끝 노드만 필수로 검사한다. */
        public Segment {
            Objects.requireNonNull(fromNodeId);
            Objects.requireNonNull(toNodeId);
        }
    }
}
