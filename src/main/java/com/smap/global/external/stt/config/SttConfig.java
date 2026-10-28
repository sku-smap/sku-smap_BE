package com.smap.global.external.stt.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class SttConfig {

    @Value("${stt.oracle.token:}")
    private String token;

    @Bean
    public RequestInterceptor sttTokenInterceptor() {
        return template -> template.header("X-STT-Token", token);
    }
}
