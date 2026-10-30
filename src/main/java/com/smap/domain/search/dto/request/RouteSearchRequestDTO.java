package com.smap.domain.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "RouteSearchRequestDTO: 문장 길찾기 검색 요청 DTO")
public class RouteSearchRequestDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "사용자가 입력한 문장 (1~200자). 출발지·목적지를 함께 말해도 되고 목적지만 말해도 됨", example = "북악관에서 청운관 어떻게 가?")
    private String query;

    @Min(1)
    @Max(10)
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, description = "출발지·목적지 각각 받을 후보 최대 개수 (1~10, 생략 시 1 = 점수 1등만)", example = "1")
    private Integer limit;

    public int limitOrDefault() {
        return limit == null ? 1 : limit;
    }
}
