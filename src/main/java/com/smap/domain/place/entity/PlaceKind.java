package com.smap.domain.place.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;

/** DB 원본 문자열은 유지하고 API에서는 정해진 장소 분류만 제공한다. */
@Schema(enumAsRef = true, description = "장소 분류: room=강의실·호실, professor=교수 연구실, office=사무실·행정실, facility=편의·공용 시설, building=건물 대표 장소")
public enum PlaceKind {
    ROOM, PROFESSOR, OFFICE, FACILITY, BUILDING;

    /** 기존 프론트 값과 호환되도록 JSON에는 소문자를 사용한다. */
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 미지정 값은 null이며, 등록되지 않은 분류를 임의로 다른 종류에 대입하지 않는다. */
    @JsonCreator
    public static PlaceKind fromValue(String value) {
        return value == null || value.isBlank() ? null : valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
