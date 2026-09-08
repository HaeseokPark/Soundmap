package com.soundmap.music;

import com.soundmap.music.client.SearchCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchCacheTest {

    @Test
    @DisplayName("넣은 값을 다시 꺼낼 수 있다")
    void storesAndReturns() {
        SearchCache<String> cache = new SearchCache<>(10, Duration.ofMinutes(5));
        cache.put("iu", "밤편지");

        assertEquals("밤편지", cache.get("iu").orElseThrow());
    }

    @Test
    @DisplayName("없는 키는 비어 있다")
    void missingKeyIsEmpty() {
        SearchCache<String> cache = new SearchCache<>(10, Duration.ofMinutes(5));

        assertTrue(cache.get("없음").isEmpty());
    }

    @Test
    @DisplayName("유효시간이 지나면 버린다")
    void expiresAfterTtl() throws InterruptedException {
        SearchCache<String> cache = new SearchCache<>(10, Duration.ofMillis(50));
        cache.put("iu", "밤편지");

        Thread.sleep(80);

        assertTrue(cache.get("iu").isEmpty(), "유효시간이 지난 값은 나오면 안 된다");
    }

    @Test
    @DisplayName("최대 개수를 넘으면 오래된 것부터 버린다")
    void evictsOldestBeyondMaxSize() {
        SearchCache<String> cache = new SearchCache<>(3, Duration.ofMinutes(5));

        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.put("d", "4");

        assertEquals(3, cache.size(), "크기 제한을 넘으면 안 된다");
        assertTrue(cache.get("a").isEmpty(), "가장 오래된 항목이 밀려나야 한다");
        assertEquals("4", cache.get("d").orElseThrow());
    }

    @Test
    @DisplayName("최근에 읽은 항목은 밀려나지 않는다")
    void accessOrderKeepsRecentlyUsed() {
        SearchCache<String> cache = new SearchCache<>(3, Duration.ofMinutes(5));

        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.get("a");   // a 를 다시 사용
        cache.put("d", "4");

        assertTrue(cache.get("a").isPresent(), "최근에 쓴 a 는 남아야 한다");
        assertTrue(cache.get("b").isEmpty(), "가장 오래 안 쓴 b 가 밀려나야 한다");
    }
}
