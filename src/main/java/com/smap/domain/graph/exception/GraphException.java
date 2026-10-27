package com.smap.domain.graph.exception;

import com.smap.global.common.exception.GeneralException;

/** 그래프 오류를 공통 예외 처리기로 전달하는 도메인 예외이다. */
public class GraphException extends GeneralException {
    /** 오류 코드의 상태·코드·메시지를 공통 예외에 전달한다. */
    public GraphException(GraphErrorCode errorCode) {
        super(errorCode);
    }
}
