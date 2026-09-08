package com.soundmap.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // 401
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해 주세요."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 가수가 포함되어 있습니다."),
    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장르가 포함되어 있습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    CHAT_REQUEST_REJECTED(HttpStatus.CONFLICT, "상대가 대화 요청을 거절했습니다."),
    CHAT_NOT_ACCEPTED(HttpStatus.CONFLICT, "아직 수락되지 않은 대화입니다."),
    CANNOT_CHAT_WITH_SELF(HttpStatus.CONFLICT, "자기 자신과는 대화할 수 없습니다."),
    CANNOT_ACCEPT_OWN_REQUEST(HttpStatus.CONFLICT, "내가 보낸 요청은 내가 수락할 수 없습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.CONFLICT, "자기 자신은 차단할 수 없습니다."),
    CANNOT_REPORT_SELF(HttpStatus.CONFLICT, "자기 자신은 신고할 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "이미 신고한 사용자입니다."),
    BLOCKED_USER(HttpStatus.CONFLICT, "차단된 사용자입니다."),
    LOCATION_SHARING_DISABLED(HttpStatus.CONFLICT, "위치 공유가 꺼져 있습니다. 설정에서 켜 주세요."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
