package com.smap.domain.place.dto.response;

import java.util.Arrays;
import java.util.List;

/** 원본의 파이프 구분 별명을 API용 배열로 변환한다. DB 값은 변경하지 않는다. */
public final class PlaceAliases {
    private PlaceAliases() {}

    /** 앞뒤 공백·빈 항목·중복을 제거하고 원본 순서를 유지한다. 미지정 값은 빈 배열이다. */
    public static List<String> parse(String aliases) {
        if (aliases == null || aliases.isBlank()) return List.of();
        return Arrays.stream(aliases.split("\\|"))
                .map(String::strip).filter(value -> !value.isEmpty()).distinct().toList();
    }
}
