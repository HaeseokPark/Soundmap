package com.soundmap.music.repository;

import com.soundmap.music.entity.NowPlaying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NowPlayingRepository extends JpaRepository<NowPlaying, Long> {

    /** 지도에 그릴 때 곡 정보까지 한 번에 가져온다 (N+1 방지). */
    @Query("select np from NowPlaying np join fetch np.track where np.userId = :userId")
    Optional<NowPlaying> findWithTrackByUserId(Long userId);

    /**
     * 여러 사용자의 재생곡을 한 번에 가져온다.
     * 마커마다 따로 조회하면 사용자 수만큼 쿼리가 나간다(N+1).
     */
    @Query("select np from NowPlaying np join fetch np.track where np.userId in :userIds")
    List<NowPlaying> findAllWithTrackByUserIdIn(Collection<Long> userIds);

    /** 시드 데이터의 재생 시각을 현재로 당긴다. 개발용이다. */
    @Modifying(clearAutomatically = true)
    @Query("update NowPlaying np set np.updatedAt = :now where np.userId in :userIds")
    int touchAll(@Param("userIds") Collection<Long> userIds, @Param("now") LocalDateTime now);
}
