package com.smap.domain.route.service;

import com.smap.domain.graph.service.GraphSnapshot;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.smap.domain.place.entity.Place;

/** 출발부터 도착까지 정렬된 촬영 지점과 이동 구간이다. 예상 시간은 계산하지 않는다. */
public record RouteResult(List<GraphSnapshot.Point> nodes, List<GraphSnapshot.Segment> segments,
                          Map<String, List<Place>> placesByNode) {
    /** 탐색기는 장소 조회 없이 순수 경로 결과를 구성한다. */
    public RouteResult(List<GraphSnapshot.Point> nodes, List<GraphSnapshot.Segment> segments) {
        this(nodes, segments, Map.of());
    }
    /** 결과 목록이 외부에서 수정되지 않도록 복사한다. */
    public RouteResult {
        nodes = List.copyOf(nodes);
        segments = List.copyOf(segments);
        placesByNode = placesByNode.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    /** 사진 사이의 실제 이동 구간 수이다. */
    public int totalSteps() {
        return segments.size();
    }
}
