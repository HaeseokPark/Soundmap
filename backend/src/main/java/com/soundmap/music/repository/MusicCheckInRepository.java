package com.soundmap.music.repository;
import com.soundmap.music.entity.MusicCheckIn;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.*;
public interface MusicCheckInRepository extends JpaRepository<MusicCheckIn,Long> {
    @EntityGraph(attributePaths="track")
    List<MusicCheckIn> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(Long userId, LocalDateTime start, LocalDateTime end, Pageable page);
    @Query("select c.track.artistName, count(c) from MusicCheckIn c where c.userId=:userId and c.createdAt>=:start and c.createdAt<:end group by c.track.artistName order by count(c) desc, c.track.artistName asc")
    List<Object[]> artists(Long userId, LocalDateTime start, LocalDateTime end);
    @Query("select c.track.id, count(distinct c.userId) from MusicCheckIn c where c.publiclyShared=true and c.userId in :users and c.createdAt>=:start and c.createdAt<:end group by c.track.id order by count(distinct c.userId) desc, c.track.id asc")
    List<Object[]> chart(Collection<Long> users, LocalDateTime start, LocalDateTime end, Pageable page);
    void deleteByIdAndUserId(Long id, Long userId);

    /** 시드 데이터가 오늘치 기록을 이미 갖고 있는지 확인하는 데 쓴다. */
    @Query("select count(c) from MusicCheckIn c where c.userId in :users and c.createdAt >= :start")
    long countSince(Collection<Long> users, LocalDateTime start);
}
