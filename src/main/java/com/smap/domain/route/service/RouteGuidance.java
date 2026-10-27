package com.smap.domain.route.service;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot.Point;
import com.smap.domain.graph.service.GraphSnapshot.Segment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 실제 경로는 유지하면서 응답용 연속 엘리베이터 구간과 안내를 통합한다. */
public final class RouteGuidance {
    private RouteGuidance() {}

    /** 승차 노드에 최종 하차 층을 안내하고 중간 구간의 안내는 null로 둔다. */
    public static List<String> descriptions(RouteResult route) {
        List<String> result = new ArrayList<>(java.util.Collections.nCopies(route.segments().size(), null));
        for (var group : groups(route)) result.set(group.startIndex(), group.segment().description());
        return result;
    }

    /** DB의 실제 간선과 구분되는 화면용 이동 구간이다. 출발·도착 ID는 실제 승하차 노드이다. */
    public static List<Segment> segments(RouteResult route) {
        return groups(route).stream().map(Group::segment).toList();
    }

    /** 구간과 노드 안내가 동일한 승하차 범위를 사용하도록 한 곳에서 묶는다. */
    private static List<Group> groups(RouteResult route) {
        List<Group> result = new ArrayList<>();
        for (int start = 0; start < route.segments().size(); start++) {
            int end = start;
            while (isElevator(route, end) && end + 1 < route.segments().size() && isElevator(route, end + 1)
                    && sameDirection(route, end, end + 1)) {
                end++;
            }
            if (end == start) {
                result.add(new Group(start, route.segments().get(start)));
                continue;
            }
            Point from = route.nodes().get(start);
            Point to = route.nodes().get(end + 1);
            String description = "엘리베이터를 타고 " + floor(from.floorLabel())
                    + "에서 " + floor(to.floorLabel()) + "으로 이동합니다.";
            result.add(new Group(start, new Segment(from.nodeId(), to.nodeId(), MoveType.ELEVATOR, description)));
            start = end;
        }
        return result;
    }

    /** 안내를 표시할 원본 노드의 위치와 통합 구간을 함께 보존한다. */
    private record Group(int startIndex, Segment segment) {}

    /** 층·건물이 불명확하거나 같은 층의 구간은 임의로 승강 이동으로 합치지 않는다. */
    private static boolean isElevator(RouteResult route, int index) {
        Point from = route.nodes().get(index);
        Point to = route.nodes().get(index + 1);
        return route.segments().get(index).moveType() == MoveType.ELEVATOR
                && from.building() != null && !from.building().isBlank()
                && Objects.equals(from.building(), to.building())
                && from.floorLabel() != null && !from.floorLabel().isBlank()
                && to.floorLabel() != null && !to.floorLabel().isBlank()
                && !from.floorLabel().equals(to.floorLabel());
    }

    /** 숫자로 판단 가능한 층에서는 상승·하강 방향이 바뀌는 구간을 분리한다. */
    private static boolean sameDirection(RouteResult route, int left, int right) {
        Integer a = level(route.nodes().get(left).floorLabel());
        Integer b = level(route.nodes().get(right).floorLabel());
        Integer c = level(route.nodes().get(right + 1).floorLabel());
        return a == null || b == null || c == null
                || Integer.signum(b - a) == Integer.signum(c - b);
    }

    /** L층의 실제 높이는 추정하지 않고 원본 표기를 사용한다. */
    private static Integer level(String label) {
        String value = label.strip().toUpperCase(java.util.Locale.ROOT).replace("층", "");
        try {
            if (value.matches("B\\d+")) return -Integer.parseInt(value.substring(1));
            if (value.matches("-?\\d+")) return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // 수치 범위를 벗어난 층 표기도 원본 안내를 유지한다.
        }
        return null;
    }

    /** 이미 층 단위가 있는 원본 표기에 층을 중복해서 붙이지 않는다. */
    private static String floor(String label) {
        String value = label.strip();
        return value.endsWith("층") ? value : value + "층";
    }
}
