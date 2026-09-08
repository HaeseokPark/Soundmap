package com.soundmap.chat;

import com.soundmap.chat.dto.ChatDtos.*;
import com.soundmap.chat.entity.ChatMessage;
import com.soundmap.chat.entity.ChatRoom;
import com.soundmap.chat.repository.ChatMessageRepository;
import com.soundmap.chat.repository.ChatRoomRepository;
import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.music.repository.NowPlayingRepository;
import com.soundmap.safety.SafetyService;
import com.soundmap.settings.SettingsService;
import com.soundmap.taste.TasteOverlapService;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 50;

    private final com.soundmap.music.MusicService musicService;
    private final com.soundmap.notification.NotificationService notifications;
    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NowPlayingRepository nowPlayingRepository;
    private final TasteOverlapService overlapService;
    private final SafetyService safetyService;
    private final SettingsService settingsService;

    /**
     * 대화를 요청한다. 이미 방이 있으면 그 방을 돌려준다.
     *
     * <p>거절당한 뒤 다시 요청하는 것은 막는다. 거절 의사를 무시하고 계속 말을
     * 걸 수 있으면 차단 기능이 있어도 소용이 없다.
     */
    @Transactional
    public ChatRoomDetail request(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new ApiException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 차단 관계면 존재하지 않는 사용자처럼 다룬다.
        if (safetyService.isBlockedBetween(requesterId, targetUserId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        Long low = Math.min(requesterId, targetUserId);
        Long high = Math.max(requesterId, targetUserId);

        ChatRoom room = roomRepository.findByUserLowIdAndUserHighId(low, high)
                .orElseGet(() -> {
                    var created=roomRepository.save(ChatRoom.between(requesterId,targetUserId));
                    notifications.create(targetUserId,requesterId,created.getId(),"REQUEST","새 대화 요청이 도착했습니다.");
                    return created;
                });

        if (room.getStatus() == ChatRoomStatus.REJECTED) {
            throw new ApiException(ErrorCode.CHAT_REQUEST_REJECTED);
        }

        return toDetail(room, requesterId, target);
    }

    @Transactional
    public ChatRoomDetail accept(Long userId, Long roomId) {
        ChatRoom room = findMyRoom(userId, roomId);

        if (room.getRequesterId().equals(userId)) {
            // 요청자가 스스로 수락하면 상대의 동의 없이 대화가 열린다.
            throw new ApiException(ErrorCode.CANNOT_ACCEPT_OWN_REQUEST);
        }
        if (room.getStatus() == ChatRoomStatus.REJECTED) {
            throw new ApiException(ErrorCode.CHAT_REQUEST_REJECTED);
        }

        if(!room.isAccepted()) notifications.create(room.partnerOf(userId),userId,roomId,"ACCEPT","대화 요청이 수락되었습니다.");
        room.accept();
        return toDetail(room, userId, partnerOf(room, userId));
    }

    @Transactional
    public void reject(Long userId, Long roomId) {
        ChatRoom room = findMyRoom(userId, roomId);
        if (room.getRequesterId().equals(userId)) {
            throw new ApiException(ErrorCode.CANNOT_ACCEPT_OWN_REQUEST);
        }
        room.reject();
    }

    /** 채팅방 목록. 상대 정보와 일치도를 묶어서 가져와 방 수만큼 쿼리가 나가지 않게 한다. */
    public List<ChatRoomSummary> findMyRooms(Long userId) {
        List<ChatRoom> rooms = roomRepository.findMyRooms(userId);
        if (rooms.isEmpty()) {
            return List.of();
        }

        java.util.Set<Long> hidden = safetyService.hiddenUserIdsFor(userId);
        rooms = rooms.stream()
                .filter(room -> !hidden.contains(room.partnerOf(userId)))
                .toList();
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> partnerIds = rooms.stream().map(room -> room.partnerOf(userId)).toList();
        Map<Long, User> partners = userRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Integer> scores = overlapService.scoresFor(userId, partnerIds);

        List<ChatRoomSummary> summaries = new ArrayList<>();
        for (ChatRoom room : rooms) {
            Long partnerId = room.partnerOf(userId);
            User partner = partners.get(partnerId);
            if (partner == null) {
                continue; // 탈퇴한 사용자
            }
            summaries.add(new ChatRoomSummary(
                    room.getId(),
                    partnerId,
                    partner.getNickname(),
                    room.getStatus(),
                    room.getRequesterId().equals(userId),
                    room.getLastMessageContent(),
                    room.getLastMessageAt(),
                    room.unreadCountFor(userId),
                    scores.get(partnerId)));
        }
        return summaries;
    }

    public ChatRoomDetail findRoom(Long userId, Long roomId) {
        ChatRoom room = findMyRoom(userId, roomId);
        return toDetail(room, userId, partnerOf(room, userId));
    }

    /**
     * 메시지 목록. 최신부터 가져와 화면에 맞게 오래된 순으로 뒤집어 준다.
     *
     * @param beforeId 이 id 보다 오래된 메시지만. 위로 스크롤할 때 쓴다.
     */
    @Transactional
    public List<ChatMessageResponse> findMessages(Long userId, Long roomId, Long beforeId, Integer size) {
        ChatRoom room = findMyRoom(userId, roomId);
        int limit = (size == null || size <= 0 || size > 100) ? DEFAULT_PAGE_SIZE : size;

        List<ChatMessage> found = beforeId == null
                ? messageRepository.findByRoomIdOrderByIdDesc(roomId, Limit.of(limit))
                : messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeId, Limit.of(limit));

        // 첫 페이지를 열었다는 건 최신 메시지까지 읽었다는 뜻이다.
        if (beforeId == null) {
            room.markRead(userId);
        }

        return found.stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(message -> ChatMessageResponse.of(message, userId))
                .toList();
    }

    /**
     * 메시지를 보낸다. REST 와 WebSocket 양쪽에서 이 메서드를 쓴다.
     *
     * <p>수락되지 않은 방에서는 보낼 수 없다.
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, Long roomId, String content) {
        return send(senderId,roomId,content,null);
    }

    @Transactional
    public ChatMessageResponse sendTrack(Long senderId, Long roomId, com.soundmap.music.dto.NowPlayingRequest request) {
        return send(senderId,roomId,"♪ " + request.title(),request);
    }

    private ChatMessageResponse send(Long senderId,Long roomId,String content,com.soundmap.music.dto.NowPlayingRequest trackRequest) {
        // 안 읽은 개수를 늘리므로 잠금을 걸고 읽는다. 잠금 없이 읽으면 동시에 들어온
        // 메시지들이 서로의 증가를 덮어써서 카운터가 실제보다 작아진다.
        ChatRoom room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.hasMember(senderId)) {
            throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (safetyService.isBlockedBetween(senderId, room.partnerOf(senderId))) {
            throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (!room.isAccepted()) {
            throw new ApiException(ErrorCode.CHAT_NOT_ACCEPTED);
        }

        String trimmed = content==null?"":content.trim();
        if (trimmed.isEmpty() || trimmed.length()>1000) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        ChatMessage message=new ChatMessage(roomId,senderId,trimmed);
        if(trackRequest!=null) message.attachTrack(musicService.findOrCreateTrack(trackRequest));
        ChatMessage saved = messageRepository.save(message);
        room.onMessageSent(senderId, trimmed, saved.getCreatedAt());
        notifications.create(room.partnerOf(senderId),senderId,roomId,"MESSAGE","새 메시지가 도착했습니다.");

        return ChatMessageResponse.of(saved, senderId);
    }

    @Transactional
    public void markRead(Long userId, Long roomId) {
        findMyRoom(userId, roomId).markRead(userId);
    }

    public long countUnread(Long userId) {
        return roomRepository.countTotalUnread(userId);
    }

    /** 방의 두 사람 중 나 말고 상대. 채팅 화면 갱신을 브로드캐스트할 때도 쓴다. */
    public Long partnerIdOf(Long userId, Long roomId) {
        return findMyRoom(userId, roomId).partnerOf(userId);
    }

    private ChatRoom findMyRoom(Long userId, Long roomId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.hasMember(userId)) {
            // 남의 방이라는 사실조차 알려주지 않는다.
            throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        // 차단한(또는 나를 차단한) 상대의 방은 없는 것으로 취급한다.
        if (safetyService.isBlockedBetween(userId, room.partnerOf(userId))) {
            throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        return room;
    }

    private User partnerOf(ChatRoom room, Long userId) {
        return userRepository.findById(room.partnerOf(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private ChatRoomDetail toDetail(ChatRoom room, Long userId, User partner) {
        TasteOverlapService.Overlap overlap = overlapService.between(userId, partner.getId());

        // 상대가 현재 곡을 비공개로 두었으면 배너에도 띄우지 않는다.
        var nowPlaying = settingsService.hidesNowPlaying(partner.getId())
                ? null
                : nowPlayingRepository.findWithTrackByUserId(partner.getId()).orElse(null);

        return new ChatRoomDetail(
                room.getId(),
                partner.getId(),
                partner.getNickname(),
                room.getStatus(),
                room.getRequesterId().equals(userId),
                room.isAccepted(),
                overlap.matchScore(),
                overlap.commonArtists(),
                nowPlaying == null ? null : nowPlaying.getTrack().getTitle(),
                nowPlaying == null ? null : nowPlaying.getTrack().getArtistName());
    }
}
