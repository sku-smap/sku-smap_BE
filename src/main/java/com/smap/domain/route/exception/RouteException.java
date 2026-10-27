package com.smap.domain.route.exception;

import com.smap.global.common.exception.GeneralException;

/** 길찾기 오류를 공통 예외 처리기로 전달하는 도메인 예외이다. */
public class RouteException extends GeneralException {
    /** 오류 코드의 상태·코드·메시지를 공통 예외에 전달한다. */
    public RouteException(RouteErrorCode errorCode) {
        super(errorCode);
    }
}
