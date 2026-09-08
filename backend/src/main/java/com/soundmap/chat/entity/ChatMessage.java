package com.soundmap.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 한 건.
 *
 * <p>방 엔티티를 참조하지 않고 roomId 만 들고 있다. 메시지를 저장할 때마다 방을
 * 함께 로딩할 필요가 없고, 목록 조회도 roomId 인덱스로 끝나기 때문이다.
 */
@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_messages_room", columnList = "room_id, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="track_id")
    private com.soundmap.music.entity.Track track;

    public void attachTrack(com.soundmap.music.entity.Track track) { this.track=track; }

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChatMessage(Long roomId, Long senderId, String content) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
