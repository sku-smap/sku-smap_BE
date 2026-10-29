package com.smap.domain.search.dto.response;

import com.smap.domain.route.model.RouteOption;
import com.smap.domain.search.entity.SearchLog;
import com.smap.domain.search.model.InputType;
import com.smap.domain.search.model.SearchStatus;
import com.smap.domain.search.model.SearchTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "SearchResponseDTO: 검색 결과 응답 DTO")
public class SearchResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "검색 ID. 후보 선택 API 경로에 사용", example = "12")
    private Long searchId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "요청한 검색 대상", example = "ORIGIN")
    private SearchTarget target;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "입력 방식. TEXT 또는 VOICE", example = "TEXT")
    private InputType inputType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "음성 인식 결과 문장 (텍스트 검색이면 null). 화면에 '이렇게 들었어요'로 보여 주기 좋음", example = "나 지금 카페 앞이야")
    private String transcript;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "사용한 STT 엔진. WHISPER 또는 OPENAI (텍스트 검색이면 null)", example = "WHISPER")
    private String sttEngine;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "결과 상태. CANDIDATES=후보 있음, NOT_FOUND=후보 없음", example = "CANDIDATES")
    private SearchStatus status;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "화면에 그대로 보여 줄 안내 문구", example = "어디에 계신지 골라 주세요.")
    private String message;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "문장에서 찾은 경로 옵션 (목적지 검색에서만, 없으면 null). 경로 조회 API의 option으로 전달", example = "COMFORTABLE")
    private RouteOption routeOption;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "후보 목록 (점수 높은 순)")
    private List<CandidateDTO> candidates;

    public static SearchResponseDTO of(SearchLog log, String message, List<CandidateDTO> candidates) {
        return SearchResponseDTO.builder()
                .searchId(log.getId())
                .target(log.getTarget())
                .inputType(log.getInputType())
                .transcript(log.getTranscript())
                .sttEngine(log.getSttEngine())
                .status(log.getStatus())
                .message(message)
                .routeOption(log.getRouteOption())
                .candidates(candidates)
                .build();
    }
}
