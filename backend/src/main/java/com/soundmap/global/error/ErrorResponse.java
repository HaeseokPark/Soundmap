package com.soundmap.global.error;

import java.util.Map;

/**
 * 모든 실패 응답의 공통 형태.
 *
 * <pre>
 * { "code": "DUPLICATE_EMAIL", "message": "이미 사용 중인 이메일입니다.", "fieldErrors": null }
 * </pre>
 */
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }
}
