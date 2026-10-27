package com.smap.domain.route.service;
import com.smap.domain.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.smap.domain.place.entity.Place;
import com.smap.domain.place.repository.PlaceRepository;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.smap.domain.route.model.RouteOption;
/** 최신 DB 그래프에 선택한 조건의 비용을 적용하여 경로와 장소를 조회한다. */
@Service
@RequiredArgsConstructor
public class RouteService {
    /** 최신 노드와 방향 간선 조회 서비스. */
    private final GraphService graphService;
    /** 최소 이동 구간 수를 계산하는 탐색기. */
    private final DijkstraPathFinder pathFinder;
    /** 경로상의 각 노드에 매핑된 장소를 조회하는 저장소. */
    private final PlaceRepository placeRepository;
    /** 조건을 생략하면 야외경로 우선으로 탐색한다. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RouteResult find(String fromNodeId, String toNodeId) {
        return find(fromNodeId, toNodeId, RouteOption.OUTDOOR_FIRST);
    }

    /** 선택한 단일 조건으로 탐색하고 경로상의 장소를 일괄 조회한다. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RouteResult find(String fromNodeId, String toNodeId, RouteOption option) {
        RouteResult route = pathFinder.find(graphService.load(), fromNodeId, toNodeId, option);
        var nodeIds = route.nodes().stream().map(node -> node.nodeId()).toList();
        var places = placeRepository.findByArriveNodeIdInOrderByNameAscPlaceIdAsc(nodeIds).stream()
                .collect(Collectors.groupingBy(Place::getArriveNodeId));
        return new RouteResult(route.nodes(), route.segments(), places);
    }
}
