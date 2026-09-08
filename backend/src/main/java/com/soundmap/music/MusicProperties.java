package com.soundmap.music;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * iTunes Search API 설정.
 *
 * <p>키가 필요 없고 무료라서 초기 개발에 쓰기 좋다. 다만 공식 쿼터가 분당 20회
 * 수준으로 알려져 있으니, 사용자가 늘면 검색 결과 캐싱을 붙여야 한다.
 */
@ConfigurationProperties(prefix = "soundmap.itunes")
public record MusicProperties(
        String baseUrl,
        String country,
        String localizeCountry,
        int limit,
        Duration timeout
) {
}
