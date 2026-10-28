package com.smap.global.external.stt.service;

import com.smap.global.common.exception.GeneralException;
import com.smap.global.external.ai.client.TranscriptionClient;
import com.smap.global.external.ai.dto.response.TranscriptionResponseDTO;
import com.smap.global.external.ai.properties.AiProperties;
import com.smap.global.external.stt.client.SttClient;
import com.smap.global.external.stt.dto.SttEngine;
import com.smap.global.external.stt.dto.SttResult;
import com.smap.global.external.stt.dto.TranscribeResponseDTO;
import com.smap.global.external.stt.exception.SttErrorCode;
import com.smap.global.external.stt.properties.SttProperties;
import feign.FeignException;
import feign.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

    private static final String LANGUAGE = "ko";

    private final SttClient sttClient;
    private final TranscriptionClient transcriptionClient;
    private final SttProperties sttProperties;
    private final AiProperties aiProperties;
    private final AtomicBoolean oracleUp = new AtomicBoolean(true);

    public SttResult transcribe(MultipartFile audio, Integer durationMs, String prompt) {
        validate(audio, durationMs);
        long startedAt = System.currentTimeMillis();
        if (sttProperties.hasToken() && oracleUp.get()) {
            try {
                TranscribeResponseDTO response = sttClient.transcribe(audio, LANGUAGE, prompt, options(durationMs));
                if (response == null || response.isNoSpeech() || isBlank(response.getText())) {
                    throw new GeneralException(SttErrorCode.EMPTY_TRANSCRIPT);
                }
                return new SttResult(response.getText().strip(), SttEngine.WHISPER,
                        System.currentTimeMillis() - startedAt);
            } catch (FeignException e) {
                if (e.status() == 413) {
                    throw new GeneralException(SttErrorCode.AUDIO_TOO_LARGE);
                }
                if (e.status() == 415 || e.status() == 400) {
                    throw new GeneralException(SttErrorCode.INVALID_AUDIO);
                }
                if (e.status() == 401) {
                    log.error("STT 토큰이 맞지 않습니다. STT_TOKEN 설정을 확인하세요.");
                } else {
                    log.warn("Oracle STT 실패, OpenAI로 대체: status={} {}", e.status(), e.getMessage());
                }
                if (e.status() < 0 && sttProperties.getOracle().isHealthCheckEnabled()) {
                    oracleUp.set(false);
                }
            }
        }
        return transcribeWithOpenAi(audio, prompt, startedAt);
    }

    public void updateHealth(boolean up) {
        if (oracleUp.getAndSet(up) != up) {
            log.info("Oracle STT 상태 변경: {}", up ? "정상" : "중단");
        }
    }

    public boolean isOracleUp() {
        return oracleUp.get();
    }

    private SttResult transcribeWithOpenAi(MultipartFile audio, String prompt, long startedAt) {
        if (!aiProperties.hasApiKey()) {
            throw new GeneralException(SttErrorCode.UNAVAILABLE);
        }
        TranscriptionResponseDTO response;
        try {
            response = transcriptionClient.transcribe(audio, sttProperties.getOpenai().getModel(), LANGUAGE, prompt);
        } catch (FeignException e) {
            log.warn("OpenAI STT 실패: status={} {}", e.status(), e.getMessage());
            throw new GeneralException(SttErrorCode.UNAVAILABLE);
        }
        if (response == null || isBlank(response.getText())) {
            throw new GeneralException(SttErrorCode.EMPTY_TRANSCRIPT);
        }
        return new SttResult(response.getText().strip(), SttEngine.OPENAI, System.currentTimeMillis() - startedAt);
    }

    private void validate(MultipartFile audio, Integer durationMs) {
        if (audio == null || audio.isEmpty()) {
            throw new GeneralException(SttErrorCode.INVALID_AUDIO);
        }
        if (audio.getSize() > sttProperties.getMaxAudioBytes()) {
            throw new GeneralException(SttErrorCode.AUDIO_TOO_LARGE);
        }
        if (durationMs != null && (durationMs <= 0 || durationMs > sttProperties.getMaxDurationMs())) {
            throw new GeneralException(SttErrorCode.INVALID_AUDIO);
        }
    }

    private Request.Options options(Integer durationMs) {
        return new Request.Options(sttProperties.getOracle().getConnectTimeoutMs(), TimeUnit.MILLISECONDS,
                sttProperties.readTimeoutMs(durationMs), TimeUnit.MILLISECONDS, true);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
