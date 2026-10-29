package com.smap.domain.route.service;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot.Point;
import com.smap.domain.graph.service.GraphSnapshot.Segment;
import com.smap.domain.route.dto.response.RouteResponseDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 안내 통합이 실제 노드·간선과 이동 횟수를 바꾸지 않는지 검증한다. */
class RouteGuidanceTests {
    /** 사용자 사례의 L층→9층→10층은 L층→10층 하나로 묶고 하차 후 도보는 유지한다. */
    @Test
    void mergesLobbyToTenWithCorrectIdsAndKeepsFollowingWalk() {
        var route = new RouteResult(List.of(
                new Point("s1013_103540", "유담관", "L", "", "l.jpg"),
                new Point("s1013_102615", "유담관", "9", "", "9.jpg"),
                new Point("s1013_105513", "유담관", "10", "", "10.jpg"),
                new Point("s1013_105549", "유담관", "10", "", "walk.jpg")),
                List.of(new Segment("s1013_103540", "s1013_102615", MoveType.ELEVATOR, "9층으로 이동합니다.", 123.5),
                        new Segment("s1013_102615", "s1013_105513", MoveType.ELEVATOR, "10층으로 이동합니다."),
                        new Segment("s1013_105513", "s1013_105549", MoveType.WALK, "복도로 이동합니다.")));
        var response = RouteResponseDTO.from(route);
        assertEquals(123.5, response.segments().get(0).arrowYawDeg());
        assertEquals(2, response.segments().size());
        var elevator = response.segments().get(0);
        assertEquals("s1013_103540", elevator.fromNodeId());
        assertEquals("s1013_105513", elevator.toNodeId());
        assertEquals("엘리베이터를 타고 L층에서 10층으로 이동합니다.", elevator.description());
        assertEquals(elevator.toNodeId(), response.segments().get(1).fromNodeId());
        assertEquals("복도로 이동합니다.", response.segments().get(1).description());
        assertEquals(4, response.nodes().size());
        assertEquals(3, response.totalSteps());
        assertEquals(3, route.segments().size());
    }

    @Test
    void combinesOneToSixAndPreservesRoute() {
        var route = route(List.of("1", "2", "3", "4", "5", "6"),
                List.of(MoveType.ELEVATOR, MoveType.ELEVATOR, MoveType.ELEVATOR, MoveType.ELEVATOR, MoveType.ELEVATOR));
        var response = RouteResponseDTO.from(route);
        assertEquals(6, response.nodes().size());
        assertEquals(1, response.segments().size());
        assertEquals("n0", response.segments().get(0).fromNodeId());
        assertEquals("n5", response.segments().get(0).toNodeId());
        assertEquals(5, response.totalSteps());
        assertEquals("엘리베이터를 타고 1층에서 6층으로 이동합니다.", response.nodes().get(0).description());
        assertEquals(response.nodes().get(0).description(), response.segments().get(0).description());
        for (int index = 1; index < 5; index++) {
            assertNull(response.nodes().get(index).description());
        }
        assertNull(response.nodes().get(5).description());
        assertTrue(route.segments().stream().allMatch(segment -> "원본 안내".equals(segment.description())));
    }

    @Test
    void walkingTransferSeparatesRides() {
        var result = RouteGuidance.descriptions(route(List.of("1", "2", "3", "3", "4", "5"),
                List.of(MoveType.ELEVATOR, MoveType.ELEVATOR, MoveType.WALK, MoveType.ELEVATOR, MoveType.ELEVATOR)));
        assertEquals("엘리베이터를 타고 1층에서 3층으로 이동합니다.", result.get(0));
        assertNull(result.get(1));
        assertEquals("원본 안내", result.get(2));
        assertEquals("엘리베이터를 타고 3층에서 5층으로 이동합니다.", result.get(3));
        var segments = RouteGuidance.segments(route(List.of("1", "2", "3", "3", "4", "5"),
                List.of(MoveType.ELEVATOR, MoveType.ELEVATOR, MoveType.WALK, MoveType.ELEVATOR, MoveType.ELEVATOR)));
        assertEquals(3, segments.size());
        assertEquals("n2", segments.get(0).toNodeId());
        assertEquals(MoveType.WALK, segments.get(1).moveType());
        assertEquals("n3", segments.get(2).fromNodeId());
    }

    @Test
    void separatesDirectionChangesAndHandlesDescent() {
        var result = RouteGuidance.descriptions(route(List.of("6", "5", "4", "5"),
                List.of(MoveType.ELEVATOR, MoveType.ELEVATOR, MoveType.ELEVATOR)));
        assertEquals("엘리베이터를 타고 6층에서 4층으로 이동합니다.", result.get(0));
        assertNull(result.get(1));
        assertEquals("원본 안내", result.get(2));
    }

    @Test
    void unknownFloorsAndDifferentBuildingsKeepOriginalGuidance() {
        var original = route(java.util.Arrays.asList("1", null, "3"), List.of(MoveType.ELEVATOR, MoveType.ELEVATOR));
        assertEquals(List.of("원본 안내", "원본 안내"), RouteGuidance.descriptions(original));
        var crossBuilding = new RouteResult(List.of(new Point("a", "본관", "1", "", "a.jpg"),
                new Point("b", "본관", "2", "", "b.jpg"), new Point("c", "한림관", "3", "", "c.jpg")),
                List.of(new Segment("a", "b", MoveType.ELEVATOR, "원본 안내"),
                        new Segment("b", "c", MoveType.ELEVATOR, "원본 안내")));
        assertEquals(List.of("원본 안내", "원본 안내"), RouteGuidance.descriptions(crossBuilding));
    }

    private RouteResult route(List<String> floors, List<MoveType> types) {
        List<Point> nodes = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();
        for (int index = 0; index < floors.size(); index++) {
            nodes.add(new Point("n" + index, "본관", floors.get(index), "", index + ".jpg"));
            if (index < types.size()) segments.add(new Segment("n" + index, "n" + (index + 1), types.get(index), "원본 안내"));
        }
        return new RouteResult(nodes, segments);
    }
}
