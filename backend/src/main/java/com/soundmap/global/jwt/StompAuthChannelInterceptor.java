package com.soundmap.global.jwt;

import com.soundmap.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket 연결의 인증.
 *
 * <p>브라우저 WebSocket 은 핸드셰이크에 임의 헤더를 붙일 수 없다. 그래서 토큰은
 * STOMP CONNECT 프레임의 헤더로 받는다. 여기서 한 번 검증해 Principal 을 심어 두면
 * 이후 프레임에서는 다시 파싱하지 않아도 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        AuthPrincipal principal = resolvePrincipal(accessor);
        if (principal == null) {
            // 인증 없이 붙으면 이후 구독/발행이 모두 막히도록 연결 자체를 거절한다.
            throw new IllegalArgumentException("WebSocket 연결에 유효한 토큰이 필요합니다.");
        }

        accessor.setUser(authenticationFor(principal));
        return message;
    }

    private AuthPrincipal resolvePrincipal(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader(TOKEN_HEADER);
        if (values == null || values.isEmpty()) {
            return null;
        }

        String raw = values.get(0);
        String token = raw.startsWith(BEARER_PREFIX) ? raw.substring(BEARER_PREFIX.length()).trim() : raw.trim();
        if (token.isEmpty()) {
            return null;
        }

        try {
            return tokenProvider.parse(token);
        } catch (ApiException e) {
            log.debug("WebSocket 토큰 검증 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Principal 의 이름을 userId 로 둔다. convertAndSendToUser 가 이 이름으로
     * 개인 큐 주소를 만들기 때문에, 서버에서 사용자에게 직접 보낼 때 그대로 쓸 수 있다.
     */
    private Principal authenticationFor(AuthPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, List.of()) {
            @Override
            public String getName() {
                return String.valueOf(principal.userId());
            }
        };
    }
}
