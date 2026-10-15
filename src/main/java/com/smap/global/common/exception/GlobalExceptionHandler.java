package com.smap.global.common.exception;

import com.smap.global.common.response.ApiResponse;
import feign.FeignException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Spring MVC가 판단한 상태 코드와 Allow 등의 표준 헤더를 보존
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorDetails error = describe(ex, status);
        logFailure(ex, status, error.code());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(ex,
                ApiResponse.fail(error.code(), error.message()), responseHeaders, status, request);
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(GeneralException ex) {
        BaseErrorCode error = ex.getErrorCode();
        return failure(ex, error.getStatus(), error.getCode(), error.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining(", "));
        return failure(ex, HttpStatus.BAD_REQUEST, "COMMON_400_CONSTRAINT",
                message.isBlank() ? "요청 값이 올바르지 않습니다." : message);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleDataConflict(Exception ex) {
        return failure(ex, HttpStatus.CONFLICT, "COMMON_409_DATA_CONFLICT",
                "기존 데이터 또는 현재 데이터 상태와 충돌하는 요청입니다.");
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessException.class})
    public ResponseEntity<ApiResponse<Void>> handleDatabaseUnavailable(Exception ex) {
        return failure(ex, HttpStatus.SERVICE_UNAVAILABLE, "COMMON_503_DATABASE_UNAVAILABLE",
                "데이터베이스에 일시적으로 접근할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(FeignException ex) {
        // 외부 서비스 인증 오류 등을 우리 API 사용자의 인증 오류로 전달하지 않습니다.
        if (ex.status() == 408 || ex.status() == 504) {
            return failure(ex, HttpStatus.GATEWAY_TIMEOUT, "COMMON_504_EXTERNAL_TIMEOUT",
                    "외부 서비스 응답 시간이 초과되었습니다.");
        }
        if (ex.status() < 0 || ex.status() == 429 || ex.status() == 503) {
            return failure(ex, HttpStatus.SERVICE_UNAVAILABLE, "COMMON_503_EXTERNAL_UNAVAILABLE",
                    "외부 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
        return failure(ex, HttpStatus.BAD_GATEWAY, "COMMON_502_EXTERNAL_SERVICE",
                "외부 서비스 요청을 처리하지 못했습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // 명시적으로 지정된 HTTP 상태도 포괄 예외 처리로 500이 되지 않도록 보존
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            ErrorDetails error = describe(ex, status);
            logFailure(ex, status, error.code());
            return ResponseEntity.status(status).headers(errorResponse.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail(error.code(), error.message()));
        }
        ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (annotation != null) {
            ErrorDetails error = describe(ex, annotation.code());
            return failure(ex, annotation.code(), error.code(), error.message());
        }
        return failure(ex, HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다.");
    }

    private ErrorDetails describe(Exception ex, HttpStatusCode status) {
        if (status.value() == 400) {
            if (ex instanceof BindException binding) {
                String message = binding.getBindingResult().getAllErrors().stream()
                        .map(error -> error instanceof org.springframework.validation.FieldError field
                                ? "[" + field.getField() + "] " + field.getDefaultMessage()
                                : Objects.requireNonNullElse(error.getDefaultMessage(), "입력값이 올바르지 않습니다."))
                        .sorted().collect(Collectors.joining(", "));
                return new ErrorDetails("COMMON_400_VALIDATION",
                        message.isBlank() ? "요청 값이 올바르지 않습니다." : message);
            }
            if (ex instanceof HandlerMethodValidationException) {
                return new ErrorDetails("COMMON_400_VALIDATION", "요청 값이 검증 조건을 만족하지 않습니다.");
            }
            if (ex instanceof TypeMismatchException) {
                return new ErrorDetails("COMMON_400_TYPE_MISMATCH", "요청 값의 형식이 올바르지 않습니다.");
            }
            if (ex instanceof HttpMessageNotReadableException) {
                return new ErrorDetails("COMMON_400_BODY_NOT_READABLE", "요청 본문(JSON)을 올바르게 작성해 주세요.");
            }
            if (ex instanceof MissingRequestValueException) {
                return new ErrorDetails("COMMON_400_MISSING_PARAMETER", "필수 요청 값이 누락되었습니다.");
            }
        }
        return switch (status.value()) {
            case 400 -> new ErrorDetails("COMMON_400_BAD_REQUEST", "요청 값이 올바르지 않습니다.");
            case 401 -> new ErrorDetails("COMMON_401_UNAUTHORIZED", "인증이 필요합니다.");
            case 403 -> new ErrorDetails("COMMON_403_FORBIDDEN", "접근 권한이 없습니다.");
            case 404 -> new ErrorDetails("COMMON_404_NOT_FOUND", "요청한 경로나 리소스를 찾을 수 없습니다.");
            case 405 -> new ErrorDetails("COMMON_405_METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다.");
            case 406 -> new ErrorDetails("COMMON_406_NOT_ACCEPTABLE", "요청한 응답 형식을 제공할 수 없습니다.");
            case 409 -> new ErrorDetails("COMMON_409_CONFLICT", "현재 상태와 충돌하는 요청입니다.");
            case 413 -> new ErrorDetails("COMMON_413_FILE_TOO_LARGE", "파일 크기가 허용 용량을 초과했습니다.");
            case 415 -> new ErrorDetails("COMMON_415_UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 요청 본문 형식입니다.");
            case 429 -> new ErrorDetails("COMMON_429_TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            case 503 -> new ErrorDetails("COMMON_503_UNAVAILABLE", "일시적으로 요청을 처리할 수 없습니다.");
            default -> new ErrorDetails("COMMON_" + status.value(), status.is5xxServerError()
                    ? "서버 오류가 발생했습니다." : "요청을 처리할 수 없습니다.");
        };
    }

    private ResponseEntity<ApiResponse<Void>> failure(Exception ex, HttpStatus status, String code, String message) {
        logFailure(ex, status, code);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(code, message));
    }

    private void logFailure(Exception ex, HttpStatusCode status, String code) {
        if (status.is5xxServerError()) {
            log.error("[{}] {}", code, ex.getClass().getSimpleName(), ex);
        } else {
            log.warn("[{}] {}", code, ex.getClass().getSimpleName());
        }
    }

    private record ErrorDetails(String code, String message) {
    }
}
