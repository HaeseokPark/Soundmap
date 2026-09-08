package com.soundmap.map;

import com.soundmap.location.GeoDistance;
import com.soundmap.location.LocationProperties;
import com.soundmap.location.entity.UserLocation;
import com.soundmap.location.repository.UserLocationRepository;
import com.soundmap.map.dto.MapPin;
import com.soundmap.map.dto.PinFilter;
import com.soundmap.music.dto.TrackResponse;
import com.soundmap.music.entity.NowPlaying;
import com.soundmap.music.repository.NowPlayingRepository;
import com.soundmap.safety.SafetyService;
import com.soundmap.settings.SettingsService;
import com.soundmap.taste.TasteMatch;
import com.soundmap.taste.dto.ArtistResponse;
import com.soundmap.taste.entity.Artist;
import com.soundmap.taste.entity.Genre;
import com.soundmap.taste.repository.TasteQueryRepository;
import com.soundmap.taste.repository.UserFavoriteArtistRepository;
import com.soundmap.taste.repository.UserFavoriteGenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    private final UserLocationRepository locationRepository;
    private final NowPlayingRepository nowPlayingRepository;
    private final UserFavoriteArtistRepository favoriteArtistRepository;
    private final UserFavoriteGenreRepository favoriteGenreRepository;
    private final TasteQueryRepository tasteQueryRepository;
    private final SafetyService safetyService;
    private final SettingsService settingsService;
    private final LocationProperties properties;

    /**
     * 지도와 목록에 그릴 마커를 모은다. 내 마커가 먼저, 나머지는 가까운 순이다.
     *
     * <p>사용자 수와 무관하게 쿼리 수가 고정되도록, 후보를 먼저 좁힌 뒤
     * 재생곡과 취향을 각각 한 번씩 묶어서 가져온다.
     *
     * <p>위치를 아직 공유하지 않았으면 기준점이 없으므로 빈 목록을 준다.
     *
     * <p>차단한 사용자와 나를 차단한 사용자는 DB 단계에서 걸러낸다. 화면에서만
     * 감추면 응답에는 남아 있어서 개발자 도구로 그대로 보인다.
     */
    public List<MapPin> findVisiblePins(Long userId, PinFilter filter) {
        Optional<UserLocation> myLocation = locationRepository.findById(userId);
        if (myLocation.isEmpty()) {
            return List.of();
        }

        UserLocation me = myLocation.get();
        int radius = clampRadius(filter.radiusMeters());

        GeoDistance.BoundingBox box = GeoDistance.boundingBox(
                me.getGridLat().doubleValue(), me.getGridLon().doubleValue(), radius);

        // 1) 사각형으로 후보를 좁힌다 (인덱스 사용)
        // 차단 관계에 있는 사용자는 조회 단계에서 제외한다.
        // IN 절이 비면 문법 오류가 나므로 실제 id 가 될 수 없는 0 을 넣어 둔다.
        Set<Long> hidden = new HashSet<>(safetyService.hiddenUserIdsFor(userId));
        hidden.add(0L);

        List<UserLocation> candidates = locationRepository.findInBoundingBox(
                box.minLat(), box.maxLat(), box.minLon(), box.maxLon(), userId,
                LocalDateTime.now().minus(properties.pinTtl()), hidden);

        // 2) 사각형은 원보다 넓다. 정확한 거리로 한 번 더 거르고 가까운 순으로 정렬한다.
        List<Map.Entry<UserLocation, Double>> withinRadius = candidates.stream()
                .map(location -> Map.entry(location, GeoDistance.haversineMeters(
                        me.getGridLat(), me.getGridLon(),
                        location.getGridLat(), location.getGridLon())))
                .filter(entry -> entry.getValue() <= radius)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .toList();

        List<Long> candidateIds = withinRadius.stream()
                .map(entry -> entry.getKey().getUserId())
                .toList();

        // 3) 내 취향. 겹치는 가수 이름을 보여줘야 하므로 엔티티로 받는다.
        Map<Long, Artist> myArtistById = favoriteArtistRepository.findArtistsByUserId(userId).stream()
                .collect(Collectors.toMap(Artist::getId, Function.identity()));
        Set<Long> myGenreIds = favoriteGenreRepository.findGenresByUserId(userId).stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        // 4) 후보들의 재생곡과 취향을 각각 한 번에 (N+1 방지)
        Map<Long, NowPlaying> nowPlayingByUser = batchNowPlaying(candidateIds);
        // 현재 곡을 비공개로 둔 사용자는 마커에 앨범아트 대신 기본 표시가 나간다.
        Set<Long> hidingNowPlaying = settingsService.userIdsHidingNowPlaying(candidateIds);
        Map<Long, Set<Long>> artistsByUser = candidateIds.isEmpty()
                ? Map.of()
                : groupPairs(tasteQueryRepository.findArtistPairsByUserIds(candidateIds));
        Map<Long, Set<Long>> genresByUser = candidateIds.isEmpty()
                ? Map.of()
                : groupPairs(tasteQueryRepository.findGenrePairsByUserIds(candidateIds));

        List<MapPin> pins = new ArrayList<>();
        pins.add(new MapPin(
                me.getUserId(),
                me.getUser().getNickname(),
                me.getGridLat(),
                me.getGridLon(),
                trackOf(nowPlayingRepository.findWithTrackByUserId(userId).orElse(null)),
                me.getUpdatedAt(),
                null,
                null,
                List.of(),
                true));

        for (Map.Entry<UserLocation, Double> entry : withinRadius) {
            UserLocation location = entry.getKey();
            Long candidateId = location.getUserId();

            Set<Long> theirArtists = artistsByUser.getOrDefault(candidateId, Set.of());
            Set<Long> theirGenres = genresByUser.getOrDefault(candidateId, Set.of());

            if (!matchesFilter(filter, theirArtists, theirGenres)) {
                continue;
            }

            List<ArtistResponse> commonArtists = theirArtists.stream()
                    .filter(myArtistById::containsKey)
                    .map(id -> ArtistResponse.from(myArtistById.get(id)))
                    .sorted(Comparator.comparing(ArtistResponse::name))
                    .toList();

            pins.add(new MapPin(
                    candidateId,
                    location.getUser().getNickname(),
                    location.getGridLat(),
                    location.getGridLon(),
                    hidingNowPlaying.contains(candidateId) ? null : trackOf(nowPlayingByUser.get(candidateId)),
                    location.getUpdatedAt(),
                    (int) Math.round(entry.getValue()),
                    TasteMatch.combinedScore(myArtistById.keySet(), theirArtists, myGenreIds, theirGenres),
                    commonArtists,
                    false));
        }

        return pins;
    }

    /** 가수/장르 필터. 지정하지 않으면 전부 통과시킨다. */
    private boolean matchesFilter(PinFilter filter, Set<Long> theirArtists, Set<Long> theirGenres) {
        if (filter.artistId() != null && !theirArtists.contains(filter.artistId())) {
            return false;
        }
        return filter.genreId() == null || theirGenres.contains(filter.genreId());
    }

    private Map<Long, NowPlaying> batchNowPlaying(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return nowPlayingRepository.findAllWithTrackByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(NowPlaying::getUserId, Function.identity()));
    }

    /** [userId, valueId] 쌍을 userId 별 집합으로 묶는다. */
    private Map<Long, Set<Long>> groupPairs(List<Object[]> pairs) {
        Map<Long, Set<Long>> grouped = new HashMap<>();
        for (Object[] pair : pairs) {
            grouped.computeIfAbsent((Long) pair[0], key -> new HashSet<>()).add((Long) pair[1]);
        }
        return grouped;
    }

    private int clampRadius(Integer requested) {
        if (requested == null || requested <= 0) {
            return properties.defaultRadiusMeters();
        }
        return Math.min(requested, properties.maxRadiusMeters());
    }

    private TrackResponse trackOf(NowPlaying nowPlaying) {
        return nowPlaying == null ? null : TrackResponse.from(nowPlaying.getTrack());
    }
}
