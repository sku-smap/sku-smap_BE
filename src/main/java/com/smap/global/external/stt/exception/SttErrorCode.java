package com.smap.global.external.stt.exception;

import com.smap.global.common.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SttErrorCode implements BaseErrorCode {

    INVALID_AUDIO(HttpStatus.BAD_REQUEST, "STT_400_INVALID_AUDIO", "녹음 파일을 읽을 수 없어요. 다시 말해 주세요."),
    AUDIO_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "STT_413_AUDIO_TOO_LARGE", "녹음이 너무 길어요. 15초 안으로 말해 주세요."),
    EMPTY_TRANSCRIPT(HttpStatus.UNPROCESSABLE_ENTITY, "STT_422_EMPTY_TRANSCRIPT", "잘 듣지 못했어요. 조금 더 크게 말해 주세요."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "STT_503_UNAVAILABLE", "지금은 음성인식을 쓸 수 없어요. 글자로 입력해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
