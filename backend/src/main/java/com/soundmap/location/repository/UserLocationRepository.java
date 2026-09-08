package com.soundmap.location.repository;

import com.soundmap.location.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    /**
     * 더미 사용자들의 갱신 시각을 현재로 당긴다. 개발용 시드 데이터 전용이다.
     *
     * <p>지도는 {@code pin-ttl} 보다 오래된 위치를 그리지 않는다. 시드는 처음 한 번만
     * 만들어지므로, 하루가 지나면 더미가 통째로 지도에서 사라져 개발·시연 때
     * 빈 화면이 된다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update UserLocation ul set ul.updatedAt = :now where ul.userId in :userIds")
    int touchAll(@Param("userIds") Collection<Long> userIds, @Param("now") LocalDateTime now);

    /**
     * 사각형 범위 안의 사용자를 찾는다.
     *
     * <p>{@code grid_lat, grid_lon} 복합 인덱스를 타도록 BETWEEN 만 쓴다. 거리 함수를
     * WHERE 에 넣으면 인덱스를 못 타므로, 정확한 원형 거리는 호출한 쪽에서 계산한다.
     *
     * <p>닉네임을 함께 쓰기 때문에 user 를 fetch join 한다. 없으면 마커 수만큼
     * 추가 쿼리가 나간다(N+1).
     *
     * <p>hiddenUserIds 에는 차단 관계에 있는 사용자를 넣는다. 목록이 비어 있으면
     * IN 절이 문법 오류가 되므로 호출하는 쪽에서 최소 한 개(0)를 채워 보낸다.
     */
    @Query("""
            select ul from UserLocation ul
            join fetch ul.user
            where ul.gridLat between :minLat and :maxLat
              and ul.gridLon between :minLon and :maxLon
              and ul.userId <> :excludedUserId
              and ul.updatedAt >= :updatedAfter
              and ul.userId not in :hiddenUserIds
            """)
    List<UserLocation> findInBoundingBox(@Param("minLat") BigDecimal minLat,
                                         @Param("maxLat") BigDecimal maxLat,
                                         @Param("minLon") BigDecimal minLon,
                                         @Param("maxLon") BigDecimal maxLon,
                                         @Param("excludedUserId") Long excludedUserId,
                                         @Param("updatedAfter") LocalDateTime updatedAfter,
                                         @Param("hiddenUserIds") Collection<Long> hiddenUserIds);
}
