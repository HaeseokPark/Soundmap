package com.soundmap.chat.entity;

import com.soundmap.chat.ChatRoomStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1:1 채팅방.
 *
 * <p>두 사용자 id 를 작은 쪽/큰 쪽으로 정렬해 보관한다. 그래야 A가 B에게 건 요청과
 * B가 A에게 건 요청이 같은 행으로 모여, 유니크 제약 하나로 중복 방을 막을 수 있다.
 * 누가 먼저 걸었는지는 requesterId 로 따로 기억한다.
 *
 * <p>안 읽은 개수와 마지막 메시지는 여기에 함께 둔다. 채팅방 목록을 그릴 때마다
 * 방마다 메시지 테이블을 뒤지면 방 수만큼 쿼리가 나가기 때문이다.
 */
@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_rooms_pair",
                columnNames = {"user_low_id", "user_high_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_low_id", nullable = false)
    private Long userLowId;

    @Column(name = "user_high_id", nullable = false)
    private Long userHighId;

    /** 먼저 대화를 요청한 사람. 수락 권한은 상대에게만 있다. */
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    @Column(name = "last_message_content", length = 200)
    private String lastMessageContent;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "unread_for_low", nullable = false)
    private int unreadForLow;

    @Column(name = "unread_for_high", nullable = false)
    private int unreadForHigh;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChatRoom(Long userLowId, Long userHighId, Long requesterId) {
        this.userLowId = userLowId;
        this.userHighId = userHighId;
        this.requesterId = requesterId;
        this.status = ChatRoomStatus.PENDING;
        this.unreadForLow = 0;
        this.unreadForHigh = 0;
    }

    /** 두 사용자로 방을 만든다. id 정렬은 여기서만 한다. */
    public static ChatRoom between(Long requesterId, Long targetId) {
        Long low = Math.min(requesterId, targetId);
        Long high = Math.max(requesterId, targetId);
        return new ChatRoom(low, high, requesterId);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long partnerOf(Long userId) {
        return userLowId.equals(userId) ? userHighId : userLowId;
    }

    public boolean hasMember(Long userId) {
        return userLowId.equals(userId) || userHighId.equals(userId);
    }

    public boolean isAccepted() {
        return status == ChatRoomStatus.ACCEPTED;
    }

    public void accept() {
        this.status = ChatRoomStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ChatRoomStatus.REJECTED;
    }

    /** 메시지가 오갈 때마다 목록에 보여줄 미리보기와 안 읽은 개수를 갱신한다. */
    public void onMessageSent(Long senderId, String content, LocalDateTime sentAt) {
        this.lastMessageContent = content.length() > 200 ? content.substring(0, 200) : content;
        this.lastMessageAt = sentAt;

        if (userLowId.equals(senderId)) {
            this.unreadForHigh++;
        } else {
            this.unreadForLow++;
        }
    }

    public void markRead(Long userId) {
        if (userLowId.equals(userId)) {
            this.unreadForLow = 0;
        } else {
            this.unreadForHigh = 0;
        }
    }

    public int unreadCountFor(Long userId) {
        return userLowId.equals(userId) ? unreadForLow : unreadForHigh;
    }
}
