package com.soundmap.dev;

import com.soundmap.location.GeoGrid;
import com.soundmap.location.LocationProperties;
import com.soundmap.location.entity.UserLocation;
import com.soundmap.location.repository.UserLocationRepository;
import com.soundmap.music.entity.MusicCheckIn;
import com.soundmap.music.entity.NowPlaying;
import com.soundmap.music.entity.Track;
import com.soundmap.music.entity.TrackSource;
import com.soundmap.music.repository.NowPlayingRepository;
import com.soundmap.music.repository.TrackRepository;
import com.soundmap.taste.entity.Artist;
import com.soundmap.taste.entity.Genre;
import com.soundmap.taste.entity.UserFavoriteArtist;
import com.soundmap.taste.entity.UserFavoriteGenre;
import com.soundmap.taste.repository.ArtistRepository;
import com.soundmap.taste.repository.GenreRepository;
import com.soundmap.taste.repository.UserFavoriteArtistRepository;
import com.soundmap.taste.repository.UserFavoriteGenreRepository;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 지도를 사람이 있는 것처럼 채워주는 더미 데이터.
 *
 * <p>혼자 개발하면 테스트 상대가 없다. 빈 지도를 놓고 만들면 마커 겹침이나
 * 이미지 로딩 지연 같은 문제를 발표 직전에야 발견하게 되므로, 개발 초기부터
 * 사람이 있는 상태로 작업한다.
 *
 * <p>이미 더미가 있으면 아무것도 하지 않는다. 다시 만들려면 DB 를 비우면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // 기준 데이터(ReferenceDataSeeder)가 먼저 들어와야 취향을 붙일 수 있다
@ConditionalOnProperty(prefix = "soundmap.seed", name = "enabled", havingValue = "true")
public class DummyDataSeeder implements ApplicationRunner {

    private static final String EMAIL_DOMAIN = "@dummy.soundmap.dev";

    private static final String[] NICKNAMES = {
            "민수", "지수", "서연", "도윤", "하은", "지호", "예린", "시우", "수아", "준서",
            "채원", "은우", "다인", "현우", "소율", "태호", "유진", "재민", "나윤", "건우",
            "서아", "지훈", "하린", "승우", "지민", "윤아", "성현", "예은", "동현", "가은",
            "현서", "주원", "세아", "민재", "아린", "정우", "보람", "태윤", "슬기", "진호"
    };

    private final UserRepository userRepository;
    private final UserLocationRepository locationRepository;
    private final TrackRepository trackRepository;
    private final NowPlayingRepository nowPlayingRepository;
    private final com.soundmap.music.repository.MusicCheckInRepository checkInRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final UserFavoriteArtistRepository favoriteArtistRepository;
    private final UserFavoriteGenreRepository favoriteGenreRepository;
    private final SeedProperties seedProperties;
    private final LocationProperties locationProperties;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (userRepository.existsByEmail(email(1))) {
            refreshExistingDummies();
            return;
        }

        List<Track> tracks = loadTracks();
        if (tracks.isEmpty()) {
            log.warn("시드 곡 데이터가 비어 있어 더미 사용자를 만들지 않습니다.");
            return;
        }

        // 실행할 때마다 같은 배치가 나오도록 시드를 고정한다.
        Random random = new Random(42);
        List<Artist> allArtists = artistRepository.findAll();
        List<Genre> allGenres = genreRepository.findAll();
        int count = Math.min(seedProperties.userCount(), NICKNAMES.length);

        for (int i = 1; i <= count; i++) {
            User user = userRepository.save(User.builder()
                    .email(email(i))
                    .passwordHash(passwordEncoder.encode(seedProperties.password()))
                    .nickname(NICKNAMES[i - 1])
                    .build());

            double[] point = randomPointNearCenter(random);
            BigDecimal step = locationProperties.gridSizeDegrees();
            locationRepository.save(new UserLocation(
                    user,
                    GeoGrid.snap(point[0], step),
                    GeoGrid.snap(point[1], step)));

            Track playing = tracks.get(random.nextInt(tracks.size()));
            nowPlayingRepository.save(new NowPlaying(user, playing));
            // 동네 차트와 음악 기록은 now_playing 이 아니라 체크인 기록을 센다.
            // 여기서 남기지 않으면 지도에 40명이 있어도 차트가 텅 빈다.
            checkInRepository.save(new MusicCheckIn(user.getId(), playing, true));

            assignRandomTaste(user.getId(), allArtists, allGenres, random);
        }

        log.info("더미 사용자 {}명을 만들었습니다. 각자 취향도 부여했습니다. (비밀번호: {})",
                count, seedProperties.password());
    }

    /**
     * 더미마다 가수 4~8명, 장르 1~3개를 무작위로 붙인다.
     * 6주차 검색과 취향 일치도를 시험하려면 취향이 골고루 흩어져 있어야 한다.
     */
    private void assignRandomTaste(Long userId, List<Artist> artists,
                                   List<Genre> genres, Random random) {
        if (artists.isEmpty() || genres.isEmpty()) {
            return;
        }

        Set<Long> artistIds = new LinkedHashSet<>();
        int artistCount = 4 + random.nextInt(5);
        while (artistIds.size() < Math.min(artistCount, artists.size())) {
            artistIds.add(artists.get(random.nextInt(artists.size())).getId());
        }
        artistIds.forEach(artistId ->
                favoriteArtistRepository.save(new UserFavoriteArtist(userId, artistId)));

        Set<Long> genreIds = new LinkedHashSet<>();
        int genreCount = 1 + random.nextInt(3);
        while (genreIds.size() < Math.min(genreCount, genres.size())) {
            genreIds.add(genres.get(random.nextInt(genres.size())).getId());
        }
        genreIds.forEach(genreId ->
                favoriteGenreRepository.save(new UserFavoriteGenre(userId, genreId)));
    }

    /**
     * 이미 만들어 둔 더미의 위치·재생 시각을 현재로 당긴다.
     *
     * <p>지도는 {@code pin-ttl}(기본 24시간)보다 오래된 위치를 그리지 않는다. 시드는
     * 한 번만 만들어지므로, 하루가 지나면 더미 40명이 통째로 사라져 지도와 목록이
     * 빈 화면이 된다. DB 를 지우기 전까지 되살아나지도 않는다.
     *
     * <p>그래서 건너뛸 때 아무것도 안 하는 대신 시각만 새로 찍는다. 배포에서는
     * {@code seed.enabled=false} 라 이 코드가 아예 돌지 않는다.
     */
    private void refreshExistingDummies() {
        List<Long> dummyIds = userRepository.findByEmailEndingWith(EMAIL_DOMAIN).stream()
                .map(User::getId)
                .toList();

        if (dummyIds.isEmpty()) {
            log.info("더미 데이터가 이미 있습니다. 시딩을 건너뜁니다.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        locationRepository.touchAll(dummyIds, now);
        nowPlayingRepository.touchAll(dummyIds, now);
        int added = ensureTodayCheckIns(dummyIds);
        log.info("더미 {}명의 위치·재생 시각을 새로 찍었습니다. 오늘자 체크인 {}건 추가. 시딩은 건너뜁니다.",
                dummyIds.size(), added);
    }

    /**
     * 더미들의 오늘자 체크인 기록을 채운다.
     *
     * <p>동네 차트는 "오늘" 공유된 곡만 센다. 위치와 재생 시각만 새로 찍으면 지도는
     * 살아나지만 차트는 여전히 비어 있어서, 기능이 없는 것처럼 보인다.
     *
     * @return 새로 만든 기록 수. 이미 오늘치가 있으면 0.
     */
    private int ensureTodayCheckIns(List<Long> dummyIds) {
        LocalDateTime startOfToday = LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();
        if (checkInRepository.countSince(dummyIds, startOfToday) > 0) {
            return 0;
        }

        List<NowPlaying> playing = nowPlayingRepository.findAllWithTrackByUserIdIn(dummyIds);
        playing.forEach(np ->
                checkInRepository.save(new MusicCheckIn(np.getUserId(), np.getTrack(), true)));
        return playing.size();
    }

    private String email(int index) {
        return "dummy%02d%s".formatted(index, EMAIL_DOMAIN);
    }

    /**
     * 중심에서 반경 안의 임의의 점. 반지름에 sqrt 를 씌우지 않으면
     * 중심 쪽에만 몰려서 뭉친다.
     */
    private double[] randomPointNearCenter(Random random) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = seedProperties.radiusMeters() * Math.sqrt(random.nextDouble());

        double latOffset = (distance * Math.cos(angle)) / 111_320.0;
        double lonOffset = (distance * Math.sin(angle))
                / (111_320.0 * Math.cos(Math.toRadians(seedProperties.centerLat())));

        return new double[]{
                seedProperties.centerLat() + latOffset,
                seedProperties.centerLon() + lonOffset
        };
    }

    /** 시딩할 때마다 외부 API 를 호출하지 않도록, 곡 정보는 파일로 굳혀 두었다. */
    private List<Track> loadTracks() {
        SeedTrack[] seeds;
        try (InputStream in = new ClassPathResource("seed/dummy-tracks.json").getInputStream()) {
            seeds = objectMapper.readValue(in, SeedTrack[].class);
        } catch (Exception e) {
            log.warn("시드 곡 파일을 읽지 못했습니다: {}", e.getMessage());
            return List.of();
        }

        List<Track> tracks = new ArrayList<>();
        for (SeedTrack seed : seeds) {
            TrackSource source = TrackSource.valueOf(seed.source());
            Track track = trackRepository.findBySourceAndExternalId(source, seed.externalId())
                    .orElseGet(() -> trackRepository.save(Track.builder()
                            .source(source)
                            .externalId(seed.externalId())
                            .title(seed.title())
                            .artistName(seed.artistName())
                            .albumName(seed.albumName())
                            .albumArtUrl(seed.albumArtUrl())
                            .build()));
            tracks.add(track);
        }
        return tracks;
    }

    private record SeedTrack(String source, String externalId, String title,
                             String artistName, String albumName, String albumArtUrl) {
    }
}
