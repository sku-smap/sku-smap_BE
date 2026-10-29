package com.smap.domain.search.dto.request;

import com.smap.domain.search.model.SearchTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "VoiceSearchRequestDTO: 음성 검색 요청 DTO (multipart/form-data)")
public class VoiceSearchRequestDTO {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "찾는 대상. ORIGIN=지금 있는 곳(출발지), DESTINATION=가고 싶은 곳(목적지)", example = "DESTINATION")
    private SearchTarget target;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "녹음 파일 (m4a·wav·webm 등, 1MB 이하)", type = "string", format = "binary")
    private MultipartFile audio;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, description = "녹음 길이(ms, 15000 이하). STT 대기 시간 계산에 사용", example = "3200")
    private Integer durationMs;

    @Min(1)
    @Max(10)
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, description = "받을 후보 최대 개수 (1~10, 생략 시 5)", example = "5")
    private Integer limit;

    public int limitOrDefault() {
        return limit == null ? 5 : limit;
    }
}
