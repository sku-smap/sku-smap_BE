package com.smap.global.cache;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 전체 목록 캐시 설정. URL은 저장하지 않으므로 사진 만료 시간과 독립적이다. */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "campus.cache")
public class CampusCacheProperties {
    private boolean enabled = true;
    @Min(1)
    private long ttlSeconds = 300;
}
