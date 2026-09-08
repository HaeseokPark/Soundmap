package com.soundmap.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 더미 데이터 설정. 개발용이며 배포 환경에서는 enabled=false 로 둔다.
 *
 * @param password 더미 계정 공통 비밀번호. 채팅 등을 테스트할 때 로그인해서 쓴다.
 */
@ConfigurationProperties(prefix = "soundmap.seed")
public record SeedProperties(
        boolean enabled,
        int userCount,
        double centerLat,
        double centerLon,
        int radiusMeters,
        String password
) {
}
