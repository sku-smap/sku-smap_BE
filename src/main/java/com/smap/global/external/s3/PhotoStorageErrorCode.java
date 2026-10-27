package com.smap.global.external.s3;

import com.smap.global.common.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 자격 증명 등 내부 상세 정보를 응답으로 노출하지 않는 사진 오류 코드이다. */
@Getter
@RequiredArgsConstructor
public enum PhotoStorageErrorCode implements BaseErrorCode {
    SIGNING_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PHOTO_503_SIGNING_FAILED", "사진 조회 URL을 발급할 수 없습니다. 서버의 AWS 자격 증명 설정을 확인해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
