package com.smap.domain.route.service;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.graph.service.GraphSnapshot.Point;
import com.smap.domain.graph.service.GraphSnapshot.Segment;
import com.smap.domain.route.model.RouteOption;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 조건별 경로 차이와 지하층의 조건별 분류를 검증한다. */
class RouteWeightPolicyTests {
    private final DijkstraPathFinder finder = new DijkstraPathFinder();

    /** 편한길의 실내·야외·지하·출입 전환 및 계단 비용의 중첩을 확인한다. */
    @Test
    void comfortableCombinesOutdoorAndStairsCosts() {
        var graph = graph(List.of(point("i", "본관", "1"), point("o", "야외", ""),
                point("b", "본관", "B1")), List.of());
        assertEquals(1, RouteWeightPolicy.cost(graph, edge("i", "i", MoveType.WALK), RouteOption.COMFORTABLE));
        assertEquals(2, RouteWeightPolicy.cost(graph, edge("i", "o", MoveType.WALK), RouteOption.COMFORTABLE));
        assertEquals(3, RouteWeightPolicy.cost(graph, edge("o", "b", MoveType.WALK), RouteOption.COMFORTABLE));
        assertEquals(8, RouteWeightPolicy.cost(graph, edge("o", "b", MoveType.STAIRS), RouteOption.COMFORTABLE));
        assertEquals(1, RouteWeightPolicy.cost(graph, edge("o", "b", MoveType.STAIRS), RouteOption.SHORTEST));
    }

    /** 계단 없는 야외 지름길보다 비용이 적은 실내 우회로를 선택한다. */
    @Test
    void comfortablePrefersIndoorDetourOverOutdoorShortcut() {
        var graph = graph(List.of(point("a", "본관", "1"), point("o", "야외", ""),
                point("b", "본관", "1"), point("c", "본관", "1"), point("d", "본관", "1")),
                List.of(edge("a", "o", MoveType.WALK), edge("o", "d", MoveType.WALK),
                        edge("a", "b", MoveType.WALK), edge("b", "c", MoveType.WALK), edge("c", "d", MoveType.WALK)));
        assertEquals(List.of("a", "o", "d"), ids(finder.find(graph, "a", "d", RouteOption.SHORTEST)));
        assertEquals(List.of("a", "b", "c", "d"), ids(finder.find(graph, "a", "d", RouteOption.COMFORTABLE)));
    }

    @Test
    void outdoorIsDefaultAndIndoorPreferenceChoosesDifferentRoute() {
        var graph = graph(List.of(point("a", "야외", ""), point("b", "본관", "1"),
                point("c", "야외", ""), point("e", "야외", ""), point("d", "야외", "")),
                List.of(edge("a", "b", MoveType.WALK), edge("b", "d", MoveType.WALK),
                        edge("a", "c", MoveType.WALK), edge("c", "e", MoveType.WALK), edge("e", "d", MoveType.WALK)));
        assertEquals(List.of("a", "c", "e", "d"), ids(finder.find(graph, "a", "d")));
        assertEquals(List.of("a", "b", "d"), ids(finder.find(graph, "a", "d", RouteOption.SHORTEST)));
        assertEquals(List.of("a", "b", "d"), ids(finder.find(graph, "a", "d", RouteOption.INDOOR_UNDERGROUND_FIRST)));
        assertEquals(ids(finder.find(graph, "a", "d")), ids(finder.find(graph, "a", "d", null)));
    }

    @Test
    void comfortableAvoidsStairsButDoesNotForbidThem() {
        var graph = graph(List.of(point("a", "본관", "1"), point("b", "본관", "2"), point("d", "본관", "3")),
                List.of(edge("a", "d", MoveType.STAIRS), edge("a", "b", MoveType.ELEVATOR), edge("b", "d", MoveType.WALK)));
        assertEquals(1, finder.find(graph, "a", "d", RouteOption.SHORTEST).totalSteps());
        assertEquals(List.of("a", "b", "d"), ids(finder.find(graph, "a", "d", RouteOption.COMFORTABLE)));
        var stairsOnly = graph(List.of(point("a", "본관", "1"), point("d", "본관", "2")), List.of(edge("a", "d", MoveType.STAIRS)));
        assertEquals(1, finder.find(stairsOnly, "a", "d", RouteOption.COMFORTABLE).totalSteps());
    }

    @Test
    void basementClassificationDependsOnOption() {
        for (String floor : List.of("B1", "B2", "지하1층", "-1")) {
            var point = point("basement", "야외", floor);
            assertEquals(RouteWeightPolicy.Space.INDOOR, RouteWeightPolicy.classify(point, RouteOption.INDOOR_UNDERGROUND_FIRST));
            assertEquals(RouteWeightPolicy.Space.OUTDOOR, RouteWeightPolicy.classify(point, RouteOption.COMFORTABLE));
        }
        var parking = new Point("parking", "야외", null, "주차장", "지하주차장_본관통로.jpg");
        assertEquals(RouteWeightPolicy.Space.INDOOR, RouteWeightPolicy.classify(parking, RouteOption.INDOOR_UNDERGROUND_FIRST));
        assertEquals(RouteWeightPolicy.Space.OUTDOOR, RouteWeightPolicy.classify(parking, RouteOption.COMFORTABLE));
    }

    private Point point(String id, String building, String floor) { return new Point(id, building, floor, id, id + ".jpg"); }
    private Segment edge(String from, String to, MoveType type) { return new Segment(from, to, type, "이동합니다."); }
    private List<String> ids(RouteResult route) { return route.nodes().stream().map(Point::nodeId).toList(); }
    private GraphSnapshot graph(List<Point> points, List<Segment> edges) {
        Map<String, Point> nodes = new HashMap<>();
        points.forEach(point -> nodes.put(point.nodeId(), point));
        Map<String, List<Segment>> outgoing = new HashMap<>();
        edges.forEach(edge -> outgoing.computeIfAbsent(edge.fromNodeId(), key -> new ArrayList<>()).add(edge));
        return new GraphSnapshot(nodes, outgoing);
    }
}
