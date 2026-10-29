package com.smap.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.env.MockEnvironment;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 외부 환경변수와 분리하여 실제 CORS 설정의 기본값을 검증한다. */
class WebConfigTests {
    @Test
    void defaultOriginsIncludeProductionFrontend() throws Exception {
        var value = WebConfig.class.getDeclaredField("allowedOrigins").getAnnotation(Value.class);
        // MockEnvironment는 시스템 환경변수·시스템 프로퍼티를 읽지 않는다.
        String defaults = new MockEnvironment().resolveRequiredPlaceholders(value.value());
        assertTrue(Arrays.stream(defaults.split(",")).map(String::strip)
                .anyMatch("https://sku-smap.vercel.app"::equals));
    }
}
