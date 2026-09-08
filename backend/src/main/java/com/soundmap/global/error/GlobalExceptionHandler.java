package com.soundmap.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleApiException(ApiException e) {
        ErrorCode code = e.getErrorCode();
        return respond(code);
    }

    /** @Valid 검증 실패 - 어떤 필드가 왜 틀렸는지 내려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleValidation(
            MethodArgumentNotValidException e) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(com.soundmap.global.error.ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
    }

    /**
     * 본문이 JSON 이 아니거나 인코딩이 깨진 경우.
     * 클라이언트 잘못이므로 500 이 아니라 400 으로 돌려준다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException e) {
        log.warn("요청 본문을 읽지 못했습니다: {}", e.getMessage());
        return respond(ErrorCode.INVALID_INPUT);
    }

    /** /api/users/abc/profile 처럼 경로 변수나 쿼리 파라미터의 타입이 맞지 않는 경우. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(com.soundmap.global.error.ErrorResponse.of(
                        ErrorCode.INVALID_INPUT,
                        Map.of(e.getName(), "값의 형식이 올바르지 않습니다.")));
    }

    /** 필수 쿼리 파라미터가 빠진 경우. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException e) {

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(com.soundmap.global.error.ErrorResponse.of(
                        ErrorCode.INVALID_INPUT,
                        Map.of(e.getParameterName(), "필수 값입니다.")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleNoResource(
            NoResourceFoundException e) {
        return respond(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 마지막 그물.
     *
     * <p>여기서 무조건 500 을 주면 안 된다. Spring MVC 가 던지는 요청 오류들은 대부분
     * {@link ErrorResponse} 를 구현하고 있고 이미 올바른 상태 코드를 들고 있다.
     * 그것까지 500 으로 덮으면 클라이언트 잘못이 서버 장애처럼 보여서 원인 추적이 어려워진다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.soundmap.global.error.ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            if (status.is4xxClientError()) {
                log.warn("잘못된 요청 ({}): {}", status, e.getMessage());
                return ResponseEntity.status(status)
                        .body(com.soundmap.global.error.ErrorResponse.of(
                                status.value() == 404 ? ErrorCode.RESOURCE_NOT_FOUND : ErrorCode.INVALID_INPUT));
            }
        }

        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<com.soundmap.global.error.ErrorResponse> respond(ErrorCode code) {
        return ResponseEntity.status(code.getStatus())
                .body(com.soundmap.global.error.ErrorResponse.of(code));
    }
}
