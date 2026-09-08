package com.soundmap.chat.repository;

import com.soundmap.chat.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    /** 두 사람 사이에 수락된 대화가 있는지. 프로필 공개 범위를 판단할 때 쓴다. */
    @Query("""
            select count(r) > 0 from ChatRoom r
            where r.userLowId = :low and r.userHighId = :high
              and r.status = com.soundmap.chat.ChatRoomStatus.ACCEPTED
            """)
    boolean existsAcceptedBetween(@Param("low") Long low, @Param("high") Long high);

    /**
     * 안 읽은 개수를 늘릴 때 쓰는 조회. 행에 쓰기 잠금을 건다.
     *
     * <p>잠금이 없으면 메시지 두 건이 거의 동시에 들어왔을 때 두 트랜잭션이 같은
     * 값을 읽고 같은 값을 써서 증가가 하나 사라진다(lost update). 실제로 3건을
     * 연달아 보냈더니 카운터가 1로 남는 것을 확인했다.
     *
     * <p>한 방에 두 사람뿐이라 잠금 경합은 사실상 없다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ChatRoom r where r.id = :id")
    Optional<ChatRoom> findByIdForUpdate(@Param("id") Long id);

    /**
     * 내가 속한 방 목록. 최근 대화가 위로 오게 한다.
     *
     * <p>거절된 방은 빼고, 아직 수락하지 않은 요청은 남긴다.
     * 받은 요청을 확인할 곳이 여기뿐이기 때문이다.
     */
    @Query("""
            select r from ChatRoom r
            where (r.userLowId = :userId or r.userHighId = :userId)
              and r.status <> com.soundmap.chat.ChatRoomStatus.REJECTED
            order by coalesce(r.lastMessageAt, r.createdAt) desc
            """)
    List<ChatRoom> findMyRooms(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(case when r.userLowId = :userId then r.unreadForLow
                                     else r.unreadForHigh end), 0)
            from ChatRoom r
            where (r.userLowId = :userId or r.userHighId = :userId)
              and r.status = com.soundmap.chat.ChatRoomStatus.ACCEPTED
            """)
    long countTotalUnread(@Param("userId") Long userId);
}
