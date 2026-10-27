package com.smap.domain.graph.exception;

import com.smap.global.common.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 저장된 그래프의 잘못된 노드 참조를 나타내는 서버 오류 코드이다. */
@Getter
@RequiredArgsConstructor
public enum GraphErrorCode implements BaseErrorCode {
    INVALID_NODE_REFERENCE(HttpStatus.INTERNAL_SERVER_ERROR, "GRAPH_500_NODE_REFERENCE", "그래프의 노드 연결 정보가 올바르지 않습니다.");

    /** 공통 예외 처리기가 사용할 HTTP 상태. */
    private final HttpStatus status;
    /** 클라이언트가 오류를 구분하는 코드. */
    private final String code;
    /** 클라이언트에게 반환할 오류 설명. */
    private final String message;
}
