package com.soundmap.global.jwt;

/**
 * 인증된 요청의 주체. 컨트롤러에서 {@code @AuthenticationPrincipal AuthPrincipal me} 로 받는다.
 *
 * <p>앞으로 위치 등록, 채팅 등 대부분의 API 가 userId 를 필요로 하므로
 * 매번 DB 를 조회하지 않도록 토큰에서 꺼낸 값을 그대로 담아둔다.
 */
public record AuthPrincipal(Long userId, String email) {
}
