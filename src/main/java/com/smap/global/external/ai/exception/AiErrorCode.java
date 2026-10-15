package com.smap.global.external.ai.exception;

import com.smap.global.common.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements BaseErrorCode {
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI_502_INVALID_RESPONSE", "AI 서비스에서 올바른 응답을 받지 못했습니다."),
    EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "AI_502_EMPTY_RESPONSE", "AI 서비스에서 텍스트 응답을 받지 못했습니다."),
    CONTENT_FILTERED(HttpStatus.UNPROCESSABLE_ENTITY, "AI_422_CONTENT_FILTERED", "AI 서비스의 콘텐츠 정책으로 응답이 제한되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
