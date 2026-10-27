package com.smap.domain.route;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.graph.service.GraphSnapshot.*;
import com.smap.domain.route.service.DijkstraPathFinder;
import com.smap.domain.route.exception.RouteErrorCode;
import com.smap.domain.route.exception.RouteException;
import com.smap.domain.graph.exception.GraphException;
import com.smap.domain.graph.exception.GraphErrorCode;
import java.util.*;
import com.smap.domain.route.model.RouteOption;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 시간 없이 이동 횟수·방향·복수 이동수단·잘못된 참조를 검증한다. */
class DijkstraPathFinderTests {
    private final DijkstraPathFinder finder = new DijkstraPathFinder();

    /** 계단도 비용 1이므로 두 구간의 엘리베이터 경로보다 한 구간의 계단을 선택한다. */
    @Test
    void everyTransportHasUnitWeight() {
        var result = finder.find(graph(List.of(edge("a", "b", MoveType.ELEVATOR),
                edge("b", "d", MoveType.ELEVATOR), edge("a", "d", MoveType.STAIRS))), "a", "d", RouteOption.SHORTEST);
        assertEquals(1, result.totalSteps());
        assertEquals(MoveType.STAIRS, result.segments().get(0).moveType());
    }

    @Test
    void findsFewestSegmentsAndPreservesSelectedTransport() {
        var graph = graph(List.of(edge("a", "b", MoveType.ELEVATOR),
                edge("a", "b", MoveType.STAIRS), edge("a", "c", MoveType.WALK),
                edge("c", "b", MoveType.WALK), edge("b", "d", MoveType.DOOR)));
        var result = finder.find(graph, "a", "d");
        assertEquals(List.of("a", "b", "d"), result.nodes().stream().map(Point::nodeId).toList());
        assertEquals(2, result.totalSteps());
        assertEquals("a.jpg", result.nodes().get(0).originalFilename());
        var preferred = finder.find(graph, "a", "b");
        assertEquals(MoveType.ELEVATOR, preferred.segments().get(0).moveType());
    }

    @Test
    void doesNotInventReverseConnections() {
        var error = assertThrows(RouteException.class,
                () -> finder.find(graph(List.of(edge("a", "b", MoveType.WALK))), "b", "a"));
        assertEquals(RouteErrorCode.PATH_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void sameNodeHasOnePhotoAndNoSegments() {
        var result = finder.find(graph(List.of()), "a", "a");
        assertEquals(1, result.nodes().size());
        assertTrue(result.segments().isEmpty());
        assertEquals(0, result.totalSteps());
    }

    @Test
    void unknownNodeIsNotReportedAsDisconnected() {
        var error = assertThrows(RouteException.class,
                () -> finder.find(graph(List.of()), "a", "unknown"));
        assertEquals(RouteErrorCode.NODE_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void brokenReferencesAreRejected() {
        var error = assertThrows(GraphException.class,
                () -> graph(List.of(edge("a", "unknown", MoveType.WALK))));
        assertEquals(GraphErrorCode.INVALID_NODE_REFERENCE, error.getErrorCode());
    }

    @Test
    void missingTransportIsNotInvented() {
        var result = finder.find(graph(List.of(edge("a", "b", null))), "a", "b");
        assertNull(result.segments().get(0).moveType());
        assertEquals("이동합니다.", result.segments().get(0).description());
    }

    private Segment edge(String from, String to, MoveType type) {
        return new Segment(from, to, type, "이동합니다.");
    }

    private GraphSnapshot graph(List<Segment> edges) {
        Map<String, Point> nodes = new HashMap<>();
        for (String id : List.of("a", "b", "c", "d")) {
            nodes.put(id, new Point(id, "건물", "1", id, id + ".jpg"));
        }
        Map<String, List<Segment>> outgoing = new HashMap<>();
        edges.forEach(edge -> outgoing.computeIfAbsent(edge.fromNodeId(),
                ignored -> new ArrayList<>()).add(edge));
        return new GraphSnapshot(nodes, outgoing);
    }
}
