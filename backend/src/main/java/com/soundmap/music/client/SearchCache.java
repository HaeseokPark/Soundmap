package com.soundmap.music.client;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 검색 결과 캐시.
 *
 * <p>iTunes Search API 는 분당 20회 정도로 알려진 쿼터가 있다. 사용자들이 같은
 * 가수를 검색하는 일이 잦으므로 결과를 잠깐 들고 있으면 호출 수가 크게 줄어든다.
 *
 * <p>크기와 유효시간을 모두 제한한다. 크기 제한이 없으면 검색어마다 항목이 쌓여
 * 메모리가 계속 늘고, 유효시간이 없으면 새로 나온 곡이 영영 검색되지 않는다.
 *
 * @param <V> 캐시할 값
 */
public class SearchCache<V> {

    private final Duration ttl;
    private final Map<String, Entry<V>> store;

    public SearchCache(int maxEntries, Duration ttl) {
        this.ttl = ttl;
        this.store = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Entry<V>> eldest) {
                        return size() > maxEntries;
                    }
                });
    }

    public Optional<V> get(String key) {
        Entry<V> entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public void put(String key, V value) {
        store.put(key, new Entry<>(value, Instant.now().plus(ttl)));
    }

    public int size() {
        return store.size();
    }

    private record Entry<V>(V value, Instant expiresAt) {
    }
}
