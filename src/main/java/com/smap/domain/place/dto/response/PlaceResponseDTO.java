package com.smap.domain.place.dto.response;

import com.smap.domain.place.entity.Place;
import com.smap.domain.place.entity.PlaceKind;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/** 장소 선택과 길찾기 요청에 사용할 도착 노드 매핑을 함께 반환한다. */
public record PlaceResponseDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String placeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) PlaceKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String building,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String floorLabel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String roomNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "길찾기의 fromNodeId 또는 toNodeId로 사용할 노드 ID", example = "EJ1_02_E") String arriveNodeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String arriveNodeTitle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) List<String> aliases,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String sub,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String buildingId
) {
    /** 원본 장소 정보를 유지하며 내부 검수용 매핑 근거는 노출하지 않는다. */
    public static PlaceResponseDTO from(Place place) {
        return new PlaceResponseDTO(place.getPlaceId(), place.getName(), PlaceKind.fromValue(place.getKind()),
                place.getBuilding(), place.getFloorLabel(), place.getRoomNo(),
                place.getArriveNodeId(), place.getArriveNodeTitle(),
                PlaceAliases.parse(place.getAliases()), place.getSub(), place.getBuildingId());
    }
}
