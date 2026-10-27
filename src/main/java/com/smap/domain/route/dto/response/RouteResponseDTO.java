package com.smap.domain.route.dto.response;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.route.service.RouteResult;
import com.smap.domain.route.service.RouteGuidance;
import java.util.List;
import java.util.stream.IntStream;
import com.smap.domain.place.entity.Place;
import java.util.function.Function;

/** 순서대로 정렬된 사진 지점·이동 구간·이동 횟수만 반환하며 시간은 반환하지 않는다. */
public record RouteResponseDTO(List<NodeDTO> nodes, List<SegmentDTO> segments, int totalSteps) {
    /** DB에 저장된 안내 문구를 포함해 내부 경로 결과를 API 응답으로 변환한다. */
    public static RouteResponseDTO from(RouteResult result) {
        return from(result, filename -> null);
    }

    /** 응답용 사진 URL을 발급하며 순수 경로 계산과 AWS 연동을 분리한다. */
    public static RouteResponseDTO from(RouteResult result, Function<String, String> imageUrls) {
        var descriptions = RouteGuidance.descriptions(result);
        return new RouteResponseDTO(
                IntStream.range(0, result.nodes().size()).mapToObj(index -> {
                    var node = result.nodes().get(index);
                    // 현재 노드에서 다음 노드로 가는 안내이며 도착 노드는 다음 구간이 없다.
                    String description = index < result.segments().size()
                            ? descriptions.get(index) : null;
                    var places = result.placesByNode().getOrDefault(node.nodeId(), List.of()).stream()
                            .map(PlaceDTO::from).toList();
                    return new NodeDTO(node.nodeId(), node.building(), node.floorLabel(),
                            node.title(), node.originalFilename(), imageUrls.apply(node.originalFilename()), description, places);
                }).toList(),
                RouteGuidance.segments(result).stream().map(segment -> new SegmentDTO(
                        segment.fromNodeId(), segment.toNodeId(), segment.moveType(),
                        segment.description())).toList(),
                result.totalSteps());
    }

    /** 경로상의 촬영 지점과 원본 사진 파일명이다. */
    public record NodeDTO(String nodeId, String building, String floorLabel,
                          String title, String originalFilename, String imageUrl, String description,
                          List<PlaceDTO> places) {}

    /** 해당 촬영 지점에 도착하도록 매핑된 장소 정보이다. */
    public record PlaceDTO(String placeId, String name, String kind, String building,
                           String floorLabel, String roomNo, String arriveNodeId,
                           String arriveNodeTitle, String doorPhoto, String aliases, String sub) {
        /** DB의 장소 정보를 사용자에게 필요한 응답 필드로 변환한다. */
        public static PlaceDTO from(Place place) {
            return new PlaceDTO(place.getPlaceId(), place.getName(), place.getKind(),
                    place.getBuilding(), place.getFloorLabel(), place.getRoomNo(),
                    place.getArriveNodeId(), place.getArriveNodeTitle(), place.getDoorPhoto(),
                    place.getAliases(), place.getSub());
        }
    }

    /** 이동 유형이 비어 있는 원본 행은 NULL로 전달하고 임의의 이동수단을 생성하지 않는다. */
    public record SegmentDTO(String fromNodeId, String toNodeId, MoveType moveType, String description) {}
}
