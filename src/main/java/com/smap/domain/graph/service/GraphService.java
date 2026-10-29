package com.smap.domain.graph.service;

import com.smap.domain.graph.repository.EdgeRepository;
import com.smap.domain.graph.repository.NodeRepository;
import com.smap.domain.graph.entity.MoveType;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** DB의 노드와 방향 간선을 읽어 탐색용 인접 목록을 구성한다. */
@Service
@RequiredArgsConstructor
public class GraphService {
    /** 촬영 지점 조회 저장소. */
    private final NodeRepository nodeRepository;
    /** 방향 간선 조회 저장소. */
    private final EdgeRepository edgeRepository;

    /**
     * 노드와 간선을 같은 읽기 스냅샷에서 조회하여 도중의 데이터 변경 영향을 막는다.
     * 캐시를 사용하지 않으므로 다음 호출에는 최신 DB 상태가 반영된다.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public GraphSnapshot load() {
        // 노드 ID로 사진과 위치 정보를 바로 찾을 수 있도록 구성한다.
        Map<String, GraphSnapshot.Point> nodes = new HashMap<>();
        nodeRepository.findAll().forEach(node -> {
            // 새 CSV에서 확정한 노드만 사용한다. 이전 route_scope 등은 이관하지 않는다.
            nodes.put(node.getNodeId(),
                new GraphSnapshot.Point(node.getNodeId(), node.getBuilding(), node.getFloorLabel(),
                        node.getOriginalFilename().replaceFirst("(?i)\\.jpe?g$", "").replace('_', ' '),
                        node.getOriginalFilename(), node.getBuildingId()));
        });
        // 출발 노드별로 방향 간선을 묶는다. 역방향 간선은 임의로 생성하지 않는다.
        Map<String, List<GraphSnapshot.Segment>> outgoing = new HashMap<>();
        edgeRepository.findAll().stream()
                .forEach(edge -> outgoing
                .computeIfAbsent(edge.getFromNodeId(), ignored -> new ArrayList<>())
                .add(new GraphSnapshot.Segment(edge.getFromNodeId(), edge.getToNodeId(),
                        edge.getMoveType().isBlank() ? null : MoveType.valueOf(edge.getMoveType()),
                        edge.getDescription())));
        // 동률 경로의 선택이 DB 조회 순서에 따라 달라지지 않도록 간선을 정렬한다.
        outgoing.values().forEach(edges -> edges.sort(Comparator
                .comparing(GraphSnapshot.Segment::toNodeId)
                .thenComparing(edge -> edge.moveType() == null ? "" : edge.moveType().name())));
        return new GraphSnapshot(nodes, outgoing);
    }
}
