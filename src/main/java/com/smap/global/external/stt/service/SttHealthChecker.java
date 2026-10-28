package com.smap.global.external.stt.service;

import com.smap.global.external.stt.client.SttClient;
import com.smap.global.external.stt.properties.SttProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stt.oracle.health-check-enabled", havingValue = "true", matchIfMissing = true)
public class SttHealthChecker {

    private final SttClient sttClient;
    private final SttService sttService;
    private final SttProperties sttProperties;

    @Scheduled(fixedDelayString = "${stt.oracle.health-interval-ms:30000}", initialDelay = 5000)
    public void check() {
        if (!sttProperties.hasToken()) {
            return;
        }
        try {
            Map<String, String> health = sttClient.health();
            sttService.updateHealth(health != null && "ok".equals(health.get("status")));
        } catch (RuntimeException e) {
            sttService.updateHealth(false);
        }
    }
}
