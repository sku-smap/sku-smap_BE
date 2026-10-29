package com.smap.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
    private final boolean success;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
    private final String code;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
    private final String message;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("200")
                .message("요청이 성공했습니다.")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("200")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
