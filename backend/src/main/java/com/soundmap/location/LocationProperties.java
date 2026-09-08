package com.soundmap.location;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * @param gridSizeDegrees 격자 한 칸의 크기(도). 작을수록 정확하고 클수록 익명성이 높다.
 * @param defaultRadiusMeters 주변 조회 기본 반경
 * @param maxRadiusMeters     클라이언트가 요청할 수 있는 최대 반경
 * @param pinTtl              이 시간보다 오래된 위치는 지도에 그리지 않는다
 */
@ConfigurationProperties(prefix = "soundmap.location")
public record LocationProperties(
        BigDecimal gridSizeDegrees,
        int defaultRadiusMeters,
        int maxRadiusMeters,
        Duration pinTtl
) {
}
