package com.smap.global.external.stt.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stt")
public class SttProperties {

    private Oracle oracle = new Oracle();
    private OpenAi openai = new OpenAi();
    private long maxAudioBytes = 1024 * 1024;
    private int maxDurationMs = 15000;

    public boolean hasToken() {
        return oracle.token != null && !oracle.token.isBlank();
    }

    public long readTimeoutMs(Integer durationMs) {
        int duration = durationMs == null ? maxDurationMs : durationMs;
        long timeout = oracle.baseTimeoutMs + Math.round(duration * oracle.perAudioMs);
        return Math.min(timeout, oracle.maxTimeoutMs);
    }

    @Getter
    @Setter
    public static class Oracle {
        private String baseUrl = "https://stt.eunhasu03.cloud";
        private String token;
        private long connectTimeoutMs = 2000;
        private long baseTimeoutMs = 2000;
        private double perAudioMs = 1.5;
        private long maxTimeoutMs = 15000;
        private boolean healthCheckEnabled = true;
    }

    @Getter
    @Setter
    public static class OpenAi {
        private String model = "gpt-4o-mini-transcribe";
    }
}
