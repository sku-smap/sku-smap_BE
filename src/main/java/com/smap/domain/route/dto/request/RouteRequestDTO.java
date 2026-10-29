package com.smap.domain.route.dto.request;
import jakarta.validation.constraints.NotBlank;
import com.smap.domain.route.model.RouteOption;
import io.swagger.v3.oas.annotations.media.Schema;
/** 출발·도착 노드 ID와 단일 이동 조건을 받는다. */
public record RouteRequestDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank(message = "출발 노드 ID는 필수입니다.") String fromNodeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank(message = "도착 노드 ID는 필수입니다.") String toNodeId,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "OUTDOOR_FIRST")
        RouteOption option) {
    /** 조건 생략 또는 null은 야외경로 우선이다. */
    public RouteRequestDTO {
        option = option == null ? RouteOption.OUTDOOR_FIRST : option;
    }
}
