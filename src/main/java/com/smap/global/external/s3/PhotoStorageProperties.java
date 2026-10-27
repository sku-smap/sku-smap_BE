package com.smap.global.external.s3;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 비공개 사진 설정이며 자격 증명 값은 외부 환경에서 주입한다. */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "storage.photos")
public class PhotoStorageProperties {
    private boolean enabled = true;
    @NotBlank
    private String bucket = "smap-photos-450974103987-ap-northeast-2-an";
    @NotBlank
    private String region = "ap-northeast-2";
    /** 현재 사진은 버킷 최상위에 있으므로 기본 경로는 빈 문자열이다. */
    private String prefix = "";
    /** 로컬 .env 또는 프로세스 환경에서 읽으며 소스에 실제 키를 기록하지 않는다. */
    private String accessKeyId = "";
    private String secretAccessKey = "";
    @Min(1)
    @Max(604800)
    private long urlTtlSeconds = 3600;
}
