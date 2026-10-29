package com.smap.domain.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "SelectionRequestDTO: 후보 선택 요청 DTO")
public class SelectionRequestDTO {

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "검색 응답 candidates에서 고른 후보의 candidateId", example = "place:r-yudam-907")
    private String candidateId;
}
