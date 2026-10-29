package com.smap.domain.graph.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 고정 건물·구역 ID와 표시명을 함께 제공한다. 야외 지상·지하는 별도 ID이다. */
public record BuildingResponseDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "고정 건물·구역 ID", example = "HYEIN") String buildingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "표시용 이름", example = "혜인관") String building
) {}
