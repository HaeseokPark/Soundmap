package com.soundmap.chat;

import com.soundmap.chat.dto.ChatDtos.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * WebSocket 으로 화면을 갱신시키는 알림 담당.
 *
 * <p>메시지는 방 단위 토픽으로, "방 상태가 바뀌었다"는 신호는 사용자 개인 큐로 보낸다.
 * 대화 요청과 수락은 상대가 아직 그 방을 열지 않은 상태에서도 알려야 하기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class ChatBroadcaster {

    public static final String ROOM_TOPIC_PREFIX = "/topic/chats/";
    public static final String USER_QUEUE = "/queue/chats";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 방을 구독 중인 두 사람에게 새 메시지를 보낸다.
     *
     * <p>{@code mine} 은 보낸 사람 기준 값이라 그대로 전달하면 받는 쪽에서 뒤집힌다.
     * 그래서 senderId 만 실어 보내고, 내 메시지인지는 받는 쪽이 판단한다.
     */
    public void broadcastMessage(Long roomId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend(
                ROOM_TOPIC_PREFIX + roomId,
                new SocketMessage(message.id(), message.roomId(), message.senderId(),
                        message.content(), message.createdAt(), message.track()));
    }

    /** 상대에게 "방 목록을 다시 불러오라"고 알린다. */
    public void notifyRoomChanged(Long userId, Long roomId) {
        if (userId == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId), USER_QUEUE, new RoomChanged(roomId, "ROOM_CHANGED"));
    }

    public record SocketMessage(
            Long id,
            Long roomId,
            Long senderId,
            String content,
            LocalDateTime createdAt,
            com.soundmap.music.dto.TrackResponse track
    ) {
    }

    public record RoomChanged(Long roomId, String type) {
    }
}
