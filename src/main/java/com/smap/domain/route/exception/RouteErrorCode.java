package com.smap.domain.route.exception;

import com.smap.global.common.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 존재하지 않는 노드와 단절 경로를 구분하는 길찾기 오류 코드이다. */
@Getter
@RequiredArgsConstructor
public enum RouteErrorCode implements BaseErrorCode {
    NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_404_NODE", "출발 또는 도착 노드를 찾을 수 없습니다."),
    PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_404_PATH", "연결된 경로가 없습니다.");

    /** 공통 예외 처리기가 사용할 HTTP 상태. */
    private final HttpStatus status;
    /** 클라이언트가 오류를 구분하는 코드. */
    private final String code;
    /** 클라이언트에게 반환할 오류 설명. */
    private final String message;
}
