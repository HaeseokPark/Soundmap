package com.soundmap.location.entity;

import com.soundmap.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자의 공개 위치.
 *
 * <p>여기에 들어오는 값은 이미 격자에 스냅된 좌표다. 원본 GPS 좌표는 어디에도
 * 저장하지 않는다. 서버가 보관하지 않는 정보는 유출될 수도 없다.
 */
@Entity
@Table(
        name = "user_locations",
        indexes = @Index(name = "idx_user_locations_geo", columnList = "grid_lat, grid_lon")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLocation {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "grid_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal gridLat;

    @Column(name = "grid_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal gridLon;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserLocation(User user, BigDecimal gridLat, BigDecimal gridLon) {
        this.user = user;
        this.gridLat = gridLat;
        this.gridLon = gridLon;
        this.updatedAt = LocalDateTime.now();
    }

    public void moveTo(BigDecimal gridLat, BigDecimal gridLon) {
        this.gridLat = gridLat;
        this.gridLon = gridLon;
        this.updatedAt = LocalDateTime.now();
    }
}
