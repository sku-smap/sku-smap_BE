package com.smap.domain.search.exception;

import com.smap.global.common.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements BaseErrorCode {

    INVALID_QUERY(HttpStatus.BAD_REQUEST, "SEARCH_400_INVALID_QUERY", "찾을 곳을 1~200자로 입력해 주세요."),
    NOT_A_CANDIDATE(HttpStatus.BAD_REQUEST, "SEARCH_400_NOT_A_CANDIDATE", "검색 결과 후보에 없는 항목입니다."),
    SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH_404_NOT_FOUND", "검색 기록을 찾을 수 없습니다."),
    UNAUTHORIZED_ADMIN(HttpStatus.UNAUTHORIZED, "ADMIN_401_UNAUTHORIZED", "관리자 키가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
