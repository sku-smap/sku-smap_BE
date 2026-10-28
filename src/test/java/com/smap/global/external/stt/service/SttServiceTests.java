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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SttServiceTests {

    private static final String PROMPT = "청운관, 북악관";

    private final SttClient sttClient = mock(SttClient.class);
    private final TranscriptionClient transcriptionClient = mock(TranscriptionClient.class);
    private final SttProperties sttProperties = new SttProperties();
    private final AiProperties aiProperties = new AiProperties();
    private SttService sttService;

    @BeforeEach
    void setUp() {
        sttProperties.getOracle().setToken("token");
        aiProperties.setApiKey("key");
        sttService = new SttService(sttClient, transcriptionClient, sttProperties, aiProperties);
    }

    @Test
    void returnsWhisperTextWhenOracleSucceeds() {
        whisperReturns("  북악관 102호  ", false);

        SttResult result = sttService.transcribe(audio(), 3000, PROMPT);

        assertEquals("북악관 102호", result.text());
        assertEquals(SttEngine.WHISPER, result.engine());
        verifyNoInteractions(transcriptionClient);
    }

    @Test
    void oracleTooLargeIsAudioTooLarge() {
        whisperFails(413);

        assertError(SttErrorCode.AUDIO_TOO_LARGE, () -> sttService.transcribe(audio(), 3000, PROMPT));
        verifyNoInteractions(transcriptionClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 415})
    void oracleRejectedAudioIsInvalidAudio(int status) {
        whisperFails(status);

        assertError(SttErrorCode.INVALID_AUDIO, () -> sttService.transcribe(audio(), 3000, PROMPT));
        verifyNoInteractions(transcriptionClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 500, 503})
    void oracleServerErrorFallsBackToOpenAiAndKeepsOracleUp(int status) {
        whisperFails(status);
        openAiReturns("복사실");

        SttResult result = sttService.transcribe(audio(), 3000, PROMPT);

        assertEquals(SttEngine.OPENAI, result.engine());
        assertEquals("복사실", result.text());
        assertTrue(sttService.isOracleUp());
    }

    @Test
    void connectionFailureMarksOracleDownAndSkipsItNextTime() {
        whisperFails(-1);
        openAiReturns("복사실");

        sttService.transcribe(audio(), 3000, PROMPT);
        sttService.transcribe(audio(), 3000, PROMPT);

        assertFalse(sttService.isOracleUp());
        verify(sttClient, times(1)).transcribe(any(), anyString(), anyString(), any(Request.Options.class));
        verify(transcriptionClient, times(2)).transcribe(any(), anyString(), anyString(), anyString());
    }

    @Test
    void connectionFailureKeepsOracleUpWhenHealthCheckIsDisabled() {
        sttProperties.getOracle().setHealthCheckEnabled(false);
        whisperFails(-1);
        openAiReturns("복사실");

        SttResult result = sttService.transcribe(audio(), 3000, PROMPT);

        assertEquals(SttEngine.OPENAI, result.engine());
        assertTrue(sttService.isOracleUp());
    }

    @Test
    void noSpeechIsEmptyTranscript() {
        whisperReturns("", true);

        assertError(SttErrorCode.EMPTY_TRANSCRIPT, () -> sttService.transcribe(audio(), 3000, PROMPT));
        verifyNoInteractions(transcriptionClient);
    }

    @Test
    void blankWhisperTextIsEmptyTranscript() {
        whisperReturns("   ", false);

        assertError(SttErrorCode.EMPTY_TRANSCRIPT, () -> sttService.transcribe(audio(), 3000, PROMPT));
    }

    @Test
    void missingTokenUsesOpenAiDirectly() {
        sttProperties.getOracle().setToken("");
        openAiReturns("도서관");

        SttResult result = sttService.transcribe(audio(), 3000, PROMPT);

        assertEquals(SttEngine.OPENAI, result.engine());
        verifyNoInteractions(sttClient);
    }

    @Test
    void fallbackWithoutOpenAiKeyIsUnavailable() {
        aiProperties.setApiKey("");
        whisperFails(503);

        assertError(SttErrorCode.UNAVAILABLE, () -> sttService.transcribe(audio(), 3000, PROMPT));
    }

    @Test
    void openAiFailureIsUnavailable() {
        whisperFails(503);
        FeignException error = feignError(500);
        when(transcriptionClient.transcribe(any(), anyString(), anyString(), anyString())).thenThrow(error);

        assertError(SttErrorCode.UNAVAILABLE, () -> sttService.transcribe(audio(), 3000, PROMPT));
    }

    @Test
    void invalidInputIsRejectedBeforeCallingStt() {
        MultipartFile empty = new MockMultipartFile("audio", "a.webm", "audio/webm", new byte[0]);
        MultipartFile large = new MockMultipartFile("audio", "a.webm", "audio/webm",
                new byte[(int) sttProperties.getMaxAudioBytes() + 1]);

        assertError(SttErrorCode.INVALID_AUDIO, () -> sttService.transcribe(null, 3000, PROMPT));
        assertError(SttErrorCode.INVALID_AUDIO, () -> sttService.transcribe(empty, 3000, PROMPT));
        assertError(SttErrorCode.AUDIO_TOO_LARGE, () -> sttService.transcribe(large, 3000, PROMPT));
        assertError(SttErrorCode.INVALID_AUDIO, () -> sttService.transcribe(audio(), 0, PROMPT));
        assertError(SttErrorCode.INVALID_AUDIO, () -> sttService.transcribe(audio(), 15001, PROMPT));
        verifyNoInteractions(sttClient, transcriptionClient);
    }

    private void whisperReturns(String text, boolean noSpeech) {
        TranscribeResponseDTO response = mock(TranscribeResponseDTO.class);
        when(response.getText()).thenReturn(text);
        when(response.isNoSpeech()).thenReturn(noSpeech);
        when(sttClient.transcribe(any(), anyString(), anyString(), any(Request.Options.class))).thenReturn(response);
    }

    private void whisperFails(int status) {
        FeignException error = feignError(status);
        when(sttClient.transcribe(any(), anyString(), anyString(), any(Request.Options.class))).thenThrow(error);
    }

    private void openAiReturns(String text) {
        TranscriptionResponseDTO response = mock(TranscriptionResponseDTO.class);
        when(response.getText()).thenReturn(text);
        when(transcriptionClient.transcribe(any(), anyString(), anyString(), anyString())).thenReturn(response);
    }

    private static FeignException feignError(int status) {
        FeignException error = mock(FeignException.class);
        when(error.status()).thenReturn(status);
        return error;
    }

    private static MultipartFile audio() {
        return new MockMultipartFile("audio", "a.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private static void assertError(SttErrorCode expected, Executable call) {
        GeneralException error = assertThrows(GeneralException.class, call);
        assertEquals(expected, error.getErrorCode());
    }
}
