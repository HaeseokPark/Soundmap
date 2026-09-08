package com.soundmap.music.client;

import com.soundmap.music.MusicProperties;
import com.soundmap.music.client.dto.ITunesSearchResponse;
import com.soundmap.music.dto.ArtistSearchResult;
import com.soundmap.music.dto.TrackSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * iTunes Search API 호출.
 *
 * <p>응답을 String 으로 받아 직접 파싱한다. iTunes 는 JSON 을 주면서도
 * Content-Type 을 {@code text/javascript} 로 내려주기 때문에, 타입 기반으로
 * 컨버터를 고르게 두면 JSON 으로 인식하지 못한다.
 *
 * <p>외부 API 가 느리거나 죽어도 우리 서비스는 계속 떠 있어야 하므로,
 * 타임아웃을 짧게 두고 실패하면 예외 대신 빈 목록을 돌려준다.
 */
@Slf4j
@Component
public class ITunesClient {

    private static final int CACHE_MAX_ENTRIES = 300;
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 가수 검색은 곡을 받아서 가수로 묶는다. 한 가수가 곡을 여러 개 갖고 있어서
     * 곡 검색과 같은 개수를 받으면 서로 다른 가수가 몇 명 안 나온다. 넉넉히 받는다.
     */
    private static final int ARTIST_FETCH_LIMIT = 50;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MusicProperties properties;
    private final SearchCache<List<TrackSearchResult>> cache =
            new SearchCache<>(CACHE_MAX_ENTRIES, CACHE_TTL);
    private final SearchCache<List<ArtistSearchResult>> artistCache =
            new SearchCache<>(CACHE_MAX_ENTRIES, CACHE_TTL);

    public ITunesClient(ObjectMapper objectMapper, MusicProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public List<TrackSearchResult> searchSongs(String term) {
        String cacheKey = term.toLowerCase(Locale.ROOT);
        Optional<List<TrackSearchResult>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("term", term)
                            .queryParam("media", "music")
                            .queryParam("entity", "song")
                            .queryParam("country", properties.country())
                            .queryParam("limit", properties.limit())
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("iTunes 호출 실패 (term={}): {}", term, e.getMessage());
            return List.of();
        }

        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            ITunesSearchResponse response = objectMapper.readValue(body, ITunesSearchResponse.class);
            if (response.results() == null) {
                return List.of();
            }
            List<ITunesSearchResponse.ITunesTrack> tracks = response.results().stream()
                    .filter(track -> track.trackId() != null && track.trackName() != null)
                    .toList();
            List<TrackSearchResult> results = localize(tracks).stream()
                    .map(TrackSearchResult::fromITunes)
                    .toList();
            // 빈 결과는 캐시하지 않는다. 레이트 리밋에 걸려 빈 응답이 온 경우
            // 30분 동안 그 검색어가 막혀 버리기 때문이다.
            if (!results.isEmpty()) {
                cache.put(cacheKey, results);
            }
            return results;
        } catch (RuntimeException e) {
            log.warn("iTunes 응답 파싱 실패 (term={}): {}", term, e.getMessage());
            return List.of();
        }
    }

    /**
     * 가수 검색.
     *
     * <p>{@code entity=musicArtist} 대신 곡을 가수 이름으로 검색해서 가수를 뽑아낸다.
     * 그쪽은 이미지가 없고 한글로 찾아도 영문 표기로 돌려주기 때문이다.
     * 같은 가수의 곡이 여러 건 오므로 {@code artistId} 로 묶고 첫 앨범아트를 쓴다.
     */
    public List<ArtistSearchResult> searchArtists(String term) {
        String cacheKey = term.toLowerCase(Locale.ROOT);
        Optional<List<ArtistSearchResult>> cached = artistCache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        String body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("term", term)
                            .queryParam("media", "music")
                            .queryParam("entity", "song")
                            .queryParam("attribute", "artistTerm")
                            .queryParam("country", properties.country())
                            .queryParam("limit", ARTIST_FETCH_LIMIT)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("iTunes 가수 검색 실패 (term={}): {}", term, e.getMessage());
            return List.of();
        }

        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            ITunesSearchResponse response = objectMapper.readValue(body, ITunesSearchResponse.class);
            if (response.results() == null) {
                return List.of();
            }

            // artistId 로 묶는다. 먼저 나온 곡의 앨범아트를 그 가수의 사진으로 쓴다.
            Map<Long, ArtistSearchResult> byArtistId = new LinkedHashMap<>();
            for (ITunesSearchResponse.ITunesTrack track : response.results()) {
                if (track.artistId() == null || track.artistName() == null) {
                    continue;
                }
                byArtistId.computeIfAbsent(track.artistId(), id ->
                        ArtistSearchResult.of(id, track.artistName(), track.artworkUrl100()));
            }

            List<ArtistSearchResult> results = List.copyOf(byArtistId.values());
            // 곡 검색과 같은 이유로 빈 결과는 캐시하지 않는다.
            if (!results.isEmpty()) {
                artistCache.put(cacheKey, results);
            }
            return results;
        } catch (RuntimeException e) {
            log.warn("iTunes 가수 응답 파싱 실패 (term={}): {}", term, e.getMessage());
            return List.of();
        }
    }

    /**
     * 곡 제목과 가수 이름을 한국어 표기로 바꿔 끼운다.
     *
     * <p>US 스토어는 "밤편지" 를 "Through the Night" 로, "아이유" 를 "IU" 로 준다.
     * KR 스토어프론트에는 한국어 표기가 있지만 {@code /search} 로는 곡이 나오지 않는다.
     * (한국은 iTunes 음악 판매 지역이 아니다.) 반면 {@code /lookup} 은 KR 로도 응답한다.
     *
     * <p>그래서 곡은 US 에서 찾고 — 미리듣기 주소가 US 에만 있다 — 거기서 얻은
     * trackId 를 KR 로 한 번 더 조회해 이름만 덮어쓴다. 앨범아트와 미리듣기는
     * US 값을 그대로 둔다.
     *
     * <p>KR 스토어에 없는 곡(주로 해외 음원)은 US 표기를 유지한다. 이 조회가
     * 실패해도 검색 자체는 US 결과로 계속되어야 하므로 예외는 삼킨다.
     */
    private List<ITunesSearchResponse.ITunesTrack> localize(List<ITunesSearchResponse.ITunesTrack> tracks) {
        String country = properties.localizeCountry();
        if (country == null || country.isBlank() || tracks.isEmpty()) {
            return tracks;
        }

        String ids = tracks.stream()
                .map(track -> String.valueOf(track.trackId()))
                .collect(Collectors.joining(","));

        Map<Long, ITunesSearchResponse.ITunesTrack> byTrackId;
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/lookup")
                            .queryParam("id", ids)
                            .queryParam("country", country)
                            .build())
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return tracks;
            }
            ITunesSearchResponse response = objectMapper.readValue(body, ITunesSearchResponse.class);
            if (response.results() == null) {
                return tracks;
            }
            byTrackId = response.results().stream()
                    .filter(track -> track.trackId() != null)
                    .collect(Collectors.toMap(
                            ITunesSearchResponse.ITunesTrack::trackId,
                            track -> track,
                            (first, second) -> first));
        } catch (RuntimeException e) {
            log.warn("iTunes {} 현지화 조회 실패: {}", country, e.getMessage());
            return tracks;
        }

        return tracks.stream()
                .map(track -> {
                    ITunesSearchResponse.ITunesTrack local = byTrackId.get(track.trackId());
                    if (local == null) {
                        return track;
                    }
                    return new ITunesSearchResponse.ITunesTrack(
                            track.trackId(),
                            firstNonBlank(local.trackName(), track.trackName()),
                            track.artistId(),
                            firstNonBlank(local.artistName(), track.artistName()),
                            firstNonBlank(local.collectionName(), track.collectionName()),
                            track.artworkUrl100(),
                            track.previewUrl());
                })
                .toList();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
