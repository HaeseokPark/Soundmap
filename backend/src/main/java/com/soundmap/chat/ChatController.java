package com.soundmap.chat;

import com.soundmap.chat.dto.ChatDtos.*;
import com.soundmap.global.jwt.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatBroadcaster broadcaster;

    /** 대화 요청. 이미 방이 있으면 그 방을 돌려준다. */
    @PostMapping("/requests")
    public ResponseEntity<ChatRoomDetail> request(@AuthenticationPrincipal AuthPrincipal me,
                                                  @Valid @RequestBody ChatRequest request) {
        ChatRoomDetail detail = chatService.request(me.userId(), request.targetUserId());
        broadcaster.notifyRoomChanged(detail.partnerId(), detail.roomId());
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @GetMapping
    public List<ChatRoomSummary> myRooms(@AuthenticationPrincipal AuthPrincipal me) {
        return chatService.findMyRooms(me.userId());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthPrincipal me) {
        return new UnreadCountResponse(chatService.countUnread(me.userId()));
    }

    @GetMapping("/{roomId}")
    public ChatRoomDetail room(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long roomId) {
        return chatService.findRoom(me.userId(), roomId);
    }

    @PostMapping("/{roomId}/accept")
    public ChatRoomDetail accept(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long roomId) {
        ChatRoomDetail detail = chatService.accept(me.userId(), roomId);
        broadcaster.notifyRoomChanged(detail.partnerId(), roomId);
        return detail;
    }

    @PostMapping("/{roomId}/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long roomId) {
        Long partnerId = chatService.partnerIdOf(me.userId(), roomId);
        chatService.reject(me.userId(), roomId);
        broadcaster.notifyRoomChanged(partnerId, roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponse> messages(@AuthenticationPrincipal AuthPrincipal me,
                                              @PathVariable Long roomId,
                                              @RequestParam(required = false) Long before,
                                              @RequestParam(required = false) Integer size) {
        return chatService.findMessages(me.userId(), roomId, before, size);
    }

    /**
     * 메시지 전송 (REST).
     *
     * <p>WebSocket 이 끊긴 상황에서도 대화가 끊기지 않도록 남겨 둔다.
     * 저장 후 브로드캐스트까지 하므로 상대 화면은 똑같이 갱신된다.
     */
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ChatMessageResponse> send(@AuthenticationPrincipal AuthPrincipal me,
                                                    @PathVariable Long roomId,
                                                    @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse saved = chatService.sendMessage(me.userId(), roomId, request.content());
        broadcaster.broadcastMessage(roomId, saved);
        broadcaster.notifyRoomChanged(chatService.partnerIdOf(me.userId(), roomId), roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{roomId}/tracks")
    public ChatMessageResponse sendTrack(@AuthenticationPrincipal AuthPrincipal me,@PathVariable Long roomId,
            @Valid @RequestBody com.soundmap.music.dto.NowPlayingRequest track) {
        var saved=chatService.sendTrack(me.userId(),roomId,track);
        broadcaster.broadcastMessage(roomId,saved);
        broadcaster.notifyRoomChanged(chatService.partnerIdOf(me.userId(),roomId),roomId);
        return saved;
    }

    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthPrincipal me,
                                         @PathVariable Long roomId) {
        chatService.markRead(me.userId(), roomId);
        return ResponseEntity.noContent().build();
    }
}
