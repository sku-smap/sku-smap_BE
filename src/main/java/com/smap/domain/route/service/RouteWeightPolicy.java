package com.smap.domain.route.service;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot;
import com.smap.domain.route.model.RouteOption;
import java.text.Normalizer;
import java.util.Set;

/** 거리나 시간을 추정하지 않고 이동 횟수와 선호 구간의 추가 비용만 계산한다. */
public final class RouteWeightPolicy {
    /** 비선호 공간의 추가 비용. 출입 전환은 양 끝 공간의 비용을 절반씩 반영한다. */
    private static final int SPACE_PENALTY = 2;
    /** 계단은 금지하지 않으며 우회가 지나치게 길면 선택될 수 있다. */
    private static final int STAIRS_PENALTY = 5;
    /** 실제 메타데이터로 확인한 건물 밖 출입 지점이다. */
    private static final Set<String> OUTDOOR_ENTRIES = Set.of(
            "bon_1f_front", "bon_2f_front", "daeil_front",
            "CW_ENTRY_UPPER_OUT", "HI01_ENTRY_UPPER_OUT");

    private RouteWeightPolicy() {}

    /** 최단거리는 항상 1이며 나머지 조건도 양수 비용만 사용한다. */
    public static int cost(GraphSnapshot graph, GraphSnapshot.Segment edge, RouteOption option) {
        if (option == RouteOption.SHORTEST) return 1;
        // 편한길은 야외 비용과 계단 비용을 함께 누적한다.
        int stairs = option == RouteOption.COMFORTABLE && edge.moveType() == MoveType.STAIRS
                ? STAIRS_PENALTY : 0;
        return 1 + stairs + penalty(graph.nodes().get(edge.fromNodeId()), option)
                + penalty(graph.nodes().get(edge.toNodeId()), option);
    }

    /** 불명확한 공간은 중립으로 두며, 층 표기만으로 야외를 지하 통로로 바꾸지 않는다. */
    private static int penalty(GraphSnapshot.Point point, RouteOption option) {
        Space space = classify(point, option);
        if (space == Space.UNKNOWN) return 0;
        boolean preferred = option == RouteOption.OUTDOOR_FIRST
                ? space == Space.OUTDOOR : space == Space.INDOOR;
        return preferred ? 0 : SPACE_PENALTY / 2;
    }

    /** 지하주차장·지하 연결구간의 명시적 사진 이름을 건물 구분보다 우선한다. */
    static Space classify(GraphSnapshot.Point point, RouteOption option) {
        String filename = Normalizer.normalize(
                point.originalFilename() == null ? "" : point.originalFilename(), Normalizer.Form.NFC);
        String floor = point.floorLabel() == null ? "" : point.floorLabel().strip().toUpperCase(java.util.Locale.ROOT);
        // 사용자가 확인한 공간 표기를 사진명 추정보다 우선한다.
        if ("지하".equals(floor)) {
            return option == RouteOption.COMFORTABLE ? Space.OUTDOOR : Space.INDOOR;
        }
        boolean basement = floor.matches("B\\d+(?:F|층)?") || floor.matches("지하\\s*\\d+(?:층)?")
                || floor.matches("-\\d+");
        if (!"지상".equals(floor) && (basement || filename.startsWith("지하주차장_") || filename.startsWith("지하1_")
                || filename.startsWith("지하2_") || point.nodeId().equals("s1014_101709"))) {
            // 같은 지하층도 사용자가 선택한 조건에 따라 공간 취급을 다르게 한다.
            return option == RouteOption.COMFORTABLE ? Space.OUTDOOR : Space.INDOOR;
        }
        if (OUTDOOR_ENTRIES.contains(point.nodeId()) || "야외".equals(point.building())) {
            return Space.OUTDOOR;
        }
        return point.building() == null || point.building().isBlank() ? Space.UNKNOWN : Space.INDOOR;
    }

    /** 실내와 지하는 동일하게 선호하므로 하나의 분류로 취급한다. */
    enum Space { OUTDOOR, INDOOR, UNKNOWN }
}
