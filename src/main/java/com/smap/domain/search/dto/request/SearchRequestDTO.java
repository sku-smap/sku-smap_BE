package com.smap.domain.search.dto.request;

import com.smap.domain.search.model.SearchTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "SearchRequestDTO: 텍스트 검색 요청 DTO")
public class SearchRequestDTO {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "찾는 대상. ORIGIN=지금 있는 곳(출발지), DESTINATION=가고 싶은 곳(목적지)", example = "ORIGIN")
    private SearchTarget target;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "사용자가 입력한 문장 (1~200자)", example = "나 지금 카페 앞이야")
    private String query;

    @Min(1)
    @Max(10)
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, description = "받을 후보 최대 개수 (1~10, 생략 시 5)", example = "5")
    private Integer limit;

    public int limitOrDefault() {
        return limit == null ? 5 : limit;
    }
}
