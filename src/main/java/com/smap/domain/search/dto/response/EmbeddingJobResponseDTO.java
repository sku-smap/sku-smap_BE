package com.smap.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "EmbeddingJobResponseDTO: 검색 문서 갱신 결과 응답 DTO")
public class EmbeddingJobResponseDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "갱신 후 전체 검색 문서 수", example = "1326")
    private int documentCount;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "이번에 임베딩한 업무 문서 수", example = "29")
    private int serviceUpdated;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "이번에 임베딩한 장소 문서 수", example = "959")
    private int placeUpdated;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "이번에 임베딩한 노드 문서 수", example = "338")
    private int nodeUpdated;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "사용한 임베딩 모델 (OpenAI 키가 없으면 null)", example = "text-embedding-3-small (1024차원)")
    private String model;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "걸린 시간(ms)", example = "41250")
    private long elapsedMs;
}
