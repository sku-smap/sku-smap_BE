package com.smap.domain.search.dto.response;

import com.smap.domain.search.model.RouteSearchMode;
import com.smap.global.external.stt.dto.SttResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "RouteSearchResponseDTO: 문장 길찾기 검색 결과 응답 DTO")
public class RouteSearchResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "문장에서 찾은 범위. ROUTE=출발지·목적지 둘 다, DESTINATION_ONLY=목적지만(출발지는 기존 검색 API로 따로 물어보기), ORIGIN_ONLY=출발지만(목적지를 따로 물어보기)", example = "ROUTE")
    private RouteSearchMode mode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "화면에 그대로 보여 줄 안내 문구", example = "출발지와 목적지를 골라 주세요.")
    private String message;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "음성 인식 결과 문장 (텍스트 검색이면 null)", example = "북악관에서 청운관 어떻게 가")
    private String transcript;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "사용한 STT 엔진. WHISPER 또는 OPENAI (텍스트 검색이면 null)", example = "WHISPER")
    private String sttEngine;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "출발지 검색 결과 (mode=DESTINATION_ONLY면 null). 기존 검색 API 응답과 같은 형식이며 searchId로 후보 선택 API를 호출")
    private SearchResponseDTO origin;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "목적지 검색 결과 (mode=ORIGIN_ONLY면 null). routeOption이 있으면 경로 조회 API의 option으로 전달")
    private SearchResponseDTO destination;

    public static RouteSearchResponseDTO of(RouteSearchMode mode, SttResult stt,
                                            SearchResponseDTO origin, SearchResponseDTO destination) {
        return RouteSearchResponseDTO.builder()
                .mode(mode)
                .message(message(mode))
                .transcript(stt == null ? null : stt.text())
                .sttEngine(stt == null ? null : stt.engine().name())
                .origin(origin)
                .destination(destination)
                .build();
    }

    private static String message(RouteSearchMode mode) {
        return switch (mode) {
            case ROUTE -> "출발지와 목적지를 골라 주세요.";
            case ORIGIN_ONLY -> "어디로 가실지 알려 주세요.";
            case DESTINATION_ONLY -> "어디로 갈지 고르고, 지금 계신 곳도 알려 주세요.";
        };
    }
}
