package com.soundmap.location.dto;

import com.soundmap.location.entity.UserLocation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 격자로 스냅된 뒤의 좌표. 클라이언트가 보낸 원본과 다를 수 있고, 그게 정상이다. */
public record LocationResponse(
        BigDecimal lat,
        BigDecimal lon,
        LocalDateTime updatedAt
) {
    public static LocationResponse from(UserLocation location) {
        return new LocationResponse(location.getGridLat(), location.getGridLon(), location.getUpdatedAt());
    }
}
