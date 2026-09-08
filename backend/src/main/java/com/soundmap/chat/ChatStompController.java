package com.soundmap.chat;

import com.soundmap.chat.dto.ChatDtos.ChatMessageResponse;
import com.soundmap.chat.dto.ChatDtos.SendMessageRequest;
import com.soundmap.global.error.ApiException;
import com.soundmap.global.jwt.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket 으로 들어오는 메시지 처리.
 *
 * <p>클라이언트는 /app/chats/{roomId}/send 로 보내고 /topic/chats/{roomId} 를 구독한다.
 * 저장과 검증은 REST 와 똑같이 {@link ChatService} 를 거치므로, 어느 경로로 들어와도
 * 수락되지 않은 방에는 메시지가 남지 않는다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final ChatBroadcaster broadcaster;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload SendMessageRequest request,
                     Authentication authentication) {

        AuthPrincipal me = (AuthPrincipal) authentication.getPrincipal();

        ChatMessageResponse saved = chatService.sendMessage(me.userId(), roomId, request.content());
        broadcaster.broadcastMessage(roomId, saved);
        broadcaster.notifyRoomChanged(chatService.partnerIdOf(me.userId(), roomId), roomId);
    }

    /**
     * 메시지 처리 중 난 오류는 보낸 사람에게만 돌려준다.
     * 여기서 잡지 않으면 조용히 사라져서 클라이언트가 영영 응답을 기다린다.
     */
    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleApiException(ApiException e) {
        return Map.of("code", e.getErrorCode().name(), "message", e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleUnexpected(Exception e) {
        log.error("WebSocket 메시지 처리 실패", e);
        return Map.of("code", "INTERNAL_ERROR", "message", "메시지를 보내지 못했습니다.");
    }
}
