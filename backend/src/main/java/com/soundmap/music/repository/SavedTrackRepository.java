package com.soundmap.music.repository;
import com.soundmap.music.entity.SavedTrack;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface SavedTrackRepository extends JpaRepository<SavedTrack,Long> {
    boolean existsByUserIdAndTrackId(Long userId, Long trackId);
    void deleteByUserIdAndTrackId(Long userId, Long trackId);
    @EntityGraph(attributePaths="track")
    List<SavedTrack> findByUserIdOrderByIdDesc(Long userId, Pageable page);
    @Query("select s.track.id from SavedTrack s where s.userId=:userId")
    List<Long> trackIds(Long userId);
}
