package com.smap.domain.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "VoiceRouteSearchRequestDTO: 음성 길찾기 검색 요청 DTO (multipart/form-data)")
public class VoiceRouteSearchRequestDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "녹음 파일 (m4a·wav·webm 등, 1MB 이하)", type = "string", format = "binary")
    private MultipartFile audio;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, description = "녹음 길이(ms, 15000 이하). STT 대기 시간 계산에 사용", example = "3200")
    private Integer durationMs;

    @Min(1)
    @Max(10)
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, description = "출발지·목적지 각각 받을 후보 최대 개수 (1~10, 생략 시 1 = 점수 1등만)", example = "1")
    private Integer limit;

    public int limitOrDefault() {
        return limit == null ? 1 : limit;
    }
}
