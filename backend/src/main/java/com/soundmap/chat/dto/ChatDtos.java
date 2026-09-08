package com.soundmap.chat.dto;

import com.soundmap.chat.ChatRoomStatus;
import com.soundmap.chat.entity.ChatMessage;
import com.soundmap.taste.dto.ArtistResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 채팅 관련 요청/응답 모음. 서로 붙어 다니는 작은 타입들이라 한 파일에 둔다. */
public final class ChatDtos {

    private ChatDtos() {
    }

    public record ChatRequest(
            @NotNull(message = "대화할 상대를 지정해 주세요.")
            Long targetUserId
    ) {
    }

    public record SendMessageRequest(
            @NotBlank(message = "메시지를 입력해 주세요.")
            @Size(max = 1000, message = "메시지는 1000자를 넘을 수 없습니다.")
            String content
    ) {
    }

    /** 채팅방 목록의 한 줄. */
    public record ChatRoomSummary(
            Long roomId,
            Long partnerId,
            String partnerNickname,
            ChatRoomStatus status,
            boolean requestedByMe,
            String lastMessageContent,
            LocalDateTime lastMessageAt,
            int unreadCount,
            Integer matchScore
    ) {
    }

    /**
     * 채팅방 상세.
     *
     * @param commonArtists 대화를 시작할 실마리. "두 분 모두 DAY6를 좋아합니다" 배너에 쓴다.
     */
    public record ChatRoomDetail(
            Long roomId,
            Long partnerId,
            String partnerNickname,
            ChatRoomStatus status,
            boolean requestedByMe,
            boolean canSend,
            Integer matchScore,
            List<ArtistResponse> commonArtists,
            String partnerNowPlayingTitle,
            String partnerNowPlayingArtist
    ) {
    }

    public record ChatMessageResponse(
            Long id,
            Long roomId,
            Long senderId,
            String content,
            LocalDateTime createdAt,
            boolean mine,
            com.soundmap.music.dto.TrackResponse track
    ) {
        public static ChatMessageResponse of(ChatMessage message, Long viewerId) {
            return new ChatMessageResponse(
                    message.getId(),
                    message.getRoomId(),
                    message.getSenderId(),
                    message.getContent(),
                    message.getCreatedAt(),
                    message.getSenderId().equals(viewerId),
                    message.getTrack()==null?null:com.soundmap.music.dto.TrackResponse.from(message.getTrack())
            );
        }
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
