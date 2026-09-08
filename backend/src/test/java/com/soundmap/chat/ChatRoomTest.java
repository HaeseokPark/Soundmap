package com.soundmap.chat;

import com.soundmap.chat.entity.ChatRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChatRoomTest {

    private static final Long ALICE = 10L;
    private static final Long BOB = 3L;

    @Test
    @DisplayName("누가 먼저 걸든 같은 쌍은 같은 방이 된다")
    void pairIsOrderIndependent() {
        ChatRoom fromAlice = ChatRoom.between(ALICE, BOB);
        ChatRoom fromBob = ChatRoom.between(BOB, ALICE);

        assertEquals(fromAlice.getUserLowId(), fromBob.getUserLowId());
        assertEquals(fromAlice.getUserHighId(), fromBob.getUserHighId());
        assertEquals(BOB, fromAlice.getUserLowId(), "작은 id 가 low 여야 한다");
    }

    @Test
    @DisplayName("요청자는 정렬과 무관하게 기억된다")
    void requesterIsRemembered() {
        assertEquals(ALICE, ChatRoom.between(ALICE, BOB).getRequesterId());
        assertEquals(BOB, ChatRoom.between(BOB, ALICE).getRequesterId());
    }

    @Test
    @DisplayName("처음에는 수락 전 상태다")
    void startsPending() {
        ChatRoom room = ChatRoom.between(ALICE, BOB);

        assertEquals(ChatRoomStatus.PENDING, room.getStatus());
        assertFalse(room.isAccepted(), "수락 전에는 메시지를 보낼 수 없어야 한다");
    }

    @Test
    @DisplayName("상대 찾기와 멤버 확인")
    void partnerAndMembership() {
        ChatRoom room = ChatRoom.between(ALICE, BOB);

        assertEquals(BOB, room.partnerOf(ALICE));
        assertEquals(ALICE, room.partnerOf(BOB));
        assertTrue(room.hasMember(ALICE));
        assertTrue(room.hasMember(BOB));
        assertFalse(room.hasMember(999L), "제3자는 멤버가 아니다");
    }

    @Test
    @DisplayName("메시지를 보내면 받는 쪽 안 읽은 개수만 늘어난다")
    void unreadIncreasesForReceiverOnly() {
        ChatRoom room = ChatRoom.between(ALICE, BOB);
        room.accept();

        room.onMessageSent(ALICE, "안녕하세요", LocalDateTime.now());
        room.onMessageSent(ALICE, "DAY6 좋아하시네요", LocalDateTime.now());

        assertEquals(2, room.unreadCountFor(BOB), "받는 쪽은 2건");
        assertEquals(0, room.unreadCountFor(ALICE), "보낸 쪽은 0건이어야 한다");
    }

    @Test
    @DisplayName("읽으면 내 것만 초기화된다")
    void markReadClearsOnlyMine() {
        ChatRoom room = ChatRoom.between(ALICE, BOB);
        room.accept();
        room.onMessageSent(ALICE, "안녕", LocalDateTime.now());
        room.onMessageSent(BOB, "안녕하세요", LocalDateTime.now());

        room.markRead(BOB);

        assertEquals(0, room.unreadCountFor(BOB));
        assertEquals(1, room.unreadCountFor(ALICE), "상대의 안 읽은 개수는 그대로여야 한다");
    }

    @Test
    @DisplayName("마지막 메시지 미리보기는 200자로 잘린다")
    void lastMessagePreviewIsTruncated() {
        ChatRoom room = ChatRoom.between(ALICE, BOB);
        room.accept();

        room.onMessageSent(ALICE, "가".repeat(500), LocalDateTime.now());

        assertEquals(200, room.getLastMessageContent().length(),
                "목록 미리보기가 컬럼 길이를 넘으면 저장에서 터진다");
    }

    @Test
    @DisplayName("수락과 거절이 상태에 반영된다")
    void acceptAndReject() {
        ChatRoom accepted = ChatRoom.between(ALICE, BOB);
        accepted.accept();
        assertTrue(accepted.isAccepted());

        ChatRoom rejected = ChatRoom.between(ALICE, BOB);
        rejected.reject();
        assertEquals(ChatRoomStatus.REJECTED, rejected.getStatus());
        assertFalse(rejected.isAccepted());
    }
}
