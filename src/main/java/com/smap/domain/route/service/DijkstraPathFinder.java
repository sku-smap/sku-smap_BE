package com.smap.domain.route.service;

import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.graph.service.GraphSnapshot.Segment;
import com.smap.domain.route.exception.RouteException;
import com.smap.domain.route.exception.RouteErrorCode;
import java.util.*;
import org.springframework.stereotype.Component;
import com.smap.domain.route.model.RouteOption;

/** 우선순위 큐 기반 다익스트라 알고리즘으로 조건별 최소 비용 경로를 구한다. */
@Component
public class DijkstraPathFinder {
    /**
     * 조건 생략 시 야외경로 우선으로 탐색하며 시간은 사용하지 않는다.
     * 존재하지 않는 노드와 연결되지 않은 경로는 별도의 도메인 오류로 구분한다.
     */
    public RouteResult find(GraphSnapshot graph, String fromNodeId, String toNodeId) {
        return find(graph, fromNodeId, toNodeId, RouteOption.OUTDOOR_FIRST);
    }

    /** 조건별 양수 비용을 적용하며 미지정 조건은 야외경로 우선으로 처리한다. */
    public RouteResult find(GraphSnapshot graph, String fromNodeId, String toNodeId, RouteOption option) {
        RouteOption selected = option == null ? RouteOption.OUTDOOR_FIRST : option;
        if (fromNodeId == null || toNodeId == null
                || !graph.nodes().containsKey(fromNodeId) || !graph.nodes().containsKey(toNodeId)) {
            throw new RouteException(RouteErrorCode.NODE_NOT_FOUND);
        }
        // 노드별 최소 누적 비용과 해당 노드로 들어온 마지막 간선을 기록한다.
        // 간선 자체를 저장하므로 이동 유형과 안내 문구가 보존된다.
        Map<String, Integer> distances = new HashMap<>();
        Map<String, Segment> previous = new HashMap<>();
        // 이동 횟수가 작은 후보부터 탐색하고 동률이면 노드 ID 순서로 처리한다.
        PriorityQueue<Visit> queue = new PriorityQueue<>(Comparator.comparing(Visit::cost)
                .thenComparing(Visit::nodeId));
        distances.put(fromNodeId, 0);
        queue.add(new Visit(fromNodeId, 0));
        while (!queue.isEmpty()) {
            Visit current = queue.poll();
            // 더 짧은 경로가 발견된 뒤 큐에 남아 있는 오래된 후보는 무시한다.
            if (current.cost() != distances.get(current.nodeId())) {
                continue;
            }
            // 모든 간선의 비용 항목이 음수가 아니므로 도착 노드를 꺼내면 최적 경로가 확정된다.
            if (current.nodeId().equals(toNodeId)) {
                break;
            }
            for (Segment edge : graph.outgoing().getOrDefault(current.nodeId(), List.of())) {
                // DB의 방향을 유지하고 기본 비용 1에 선택 조건의 추가 비용을 반영한다.
                int candidate = current.cost() + RouteWeightPolicy.cost(graph, edge, selected);
                Integer known = distances.get(edge.toNodeId());
                if (known == null || candidate < known) {
                    distances.put(edge.toNodeId(), candidate);
                    previous.put(edge.toNodeId(), edge);
                    queue.add(new Visit(edge.toNodeId(), candidate));
                }
            }
        }
        // 도달 가능한 후보를 모두 처리해도 도착하지 못했다면 단절된 경로이다.
        if (!distances.containsKey(toNodeId)) {
            throw new RouteException(RouteErrorCode.PATH_NOT_FOUND);
        }
        // 도착지부터 이전 간선을 역추적하고 앞에 삽입하여 이동 순서를 복원한다.
        LinkedList<Segment> segments = new LinkedList<>();
        String cursor = toNodeId;
        while (!cursor.equals(fromNodeId)) {
            Segment edge = previous.get(cursor);
            segments.addFirst(edge);
            cursor = edge.fromNodeId();
        }
        // 출발 사진 뒤에 각 구간의 도착 사진을 이어 붙인다.
        List<GraphSnapshot.Point> nodes = new ArrayList<>();
        nodes.add(graph.nodes().get(fromNodeId));
        segments.forEach(edge -> nodes.add(graph.nodes().get(edge.toNodeId())));
        return new RouteResult(nodes, segments);
    }

    /** 큐에 저장하는 탐색 후보와 출발지로부터의 누적 이동 횟수이다. */
    private record Visit(String nodeId, int cost) {}
}
