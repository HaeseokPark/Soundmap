package com.soundmap.settings.repository;

import com.soundmap.settings.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    /**
     * 여러 사용자의 설정을 한 번에. 지도에 마커를 그릴 때 사람마다 설정을
     * 조회하면 사람 수만큼 쿼리가 나간다.
     *
     * <p>설정을 한 번도 건드리지 않은 사용자는 행이 없다. 그런 경우는 기본값으로 본다.
     */
    @Query("select s.userId from UserSettings s where s.userId in :userIds and s.shareNowPlaying = false")
    List<Long> findUserIdsHidingNowPlaying(@Param("userIds") Collection<Long> userIds);
}
