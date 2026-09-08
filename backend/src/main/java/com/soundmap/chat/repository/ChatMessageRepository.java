package com.soundmap.chat.repository;

import com.soundmap.chat.entity.ChatMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 최신 메시지부터 size 개.
     *
     * <p>페이지 번호 대신 id 커서를 쓴다. 대화 중에 새 메시지가 들어오면 번호가
     * 밀려서 같은 메시지를 두 번 받거나 건너뛰게 되기 때문이다.
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="track")
    List<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Limit limit);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="track")
    List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long beforeId, Limit limit);
}
