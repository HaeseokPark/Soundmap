package com.soundmap.taste;

import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.music.client.ITunesClient;
import com.soundmap.music.dto.ArtistSearchResult;
import com.soundmap.music.dto.TrackResponse;
import com.soundmap.chat.repository.ChatRoomRepository;
import com.soundmap.music.repository.NowPlayingRepository;
import com.soundmap.safety.SafetyService;
import com.soundmap.settings.ProfileScope;
import com.soundmap.settings.SettingsService;
import com.soundmap.taste.dto.*;
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
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteService {

    private static final int ARTIST_SEARCH_LIMIT = 40;

    /**
     * 우리 DB 에서 이만큼도 안 나오면 iTunes 까지 찾아본다.
     *
     * <p>기준을 두는 이유는 두 가지다. 흔한 검색어는 로컬에서 바로 끝나서 빠르고,
     * iTunes 의 분당 호출 한도도 아낄 수 있다.
     */
    private static final int LOCAL_RESULT_THRESHOLD = 5;

    private final ArtistRepository artistRepository;
    private final ITunesClient iTunesClient;
    private final GenreRepository genreRepository;
    private final UserFavoriteArtistRepository favoriteArtistRepository;
    private final UserFavoriteGenreRepository favoriteGenreRepository;
    private final NowPlayingRepository nowPlayingRepository;
    private final UserRepository userRepository;
    private final SafetyService safetyService;
    private final SettingsService settingsService;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 가수 검색.
     *
     * <p>기준 데이터로 심어 둔 가수만으로는 찾는 사람이 없는 경우가 훨씬 많다.
     * 그래서 우리 DB 에서 충분히 안 나오면 iTunes 까지 찾아보고, 거기서 나온
     * 가수는 {@code artists} 에 저장해 둔다. 저장하는 이유는 취향 저장이 가수
     * id 를 받기 때문이다 — 행이 있어야 고를 수 있다. 곡(tracks)도 같은 방식이다.
     *
     * <p>검색만 해도 행이 생기므로 고르지 않은 가수도 쌓인다. 다음 사람이 같은
     * 이름을 찾을 때 외부 호출 없이 바로 나오므로 그대로 둔다.
     */
    @Transactional
    public List<ArtistResponse> searchArtists(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return artistRepository.findAllByOrderByName(Limit.of(ARTIST_SEARCH_LIMIT)).stream()
                    .map(ArtistResponse::from).toList();
        }

        List<Artist> local = artistRepository.findByNameContainingIgnoreCaseOrderByName(
                trimmed, Limit.of(ARTIST_SEARCH_LIMIT));
        if (local.size() >= LOCAL_RESULT_THRESHOLD) {
            return local.stream().map(ArtistResponse::from).toList();
        }

        // 로컬 결과를 앞에 두고, iTunes 에서 새로 찾은 가수를 뒤에 붙인다.
        Set<String> seen = local.stream()
                .map(artist -> artist.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ArtistResponse> merged = new ArrayList<>(local.stream().map(ArtistResponse::from).toList());
        for (ArtistSearchResult found : iTunesClient.searchArtists(trimmed)) {
            if (merged.size() >= ARTIST_SEARCH_LIMIT) {
                break;
            }
            if (found.name() == null || !seen.add(found.name().toLowerCase(Locale.ROOT))) {
                continue;
            }
            merged.add(ArtistResponse.from(findOrCreateArtist(found)));
        }
        return merged;
    }

    /** 이름이 같은 가수가 이미 있으면 그 행을 쓴다. 기준 데이터와 중복 행이 생기지 않게 한다. */
    private Artist findOrCreateArtist(ArtistSearchResult found) {
        return artistRepository.findByName(found.name())
                .orElseGet(() -> artistRepository.save(
                        Artist.builder().name(found.name()).imageUrl(found.imageUrl()).build()));
    }

    public List<GenreResponse> findAllGenres() {
        return genreRepository.findAll().stream().map(GenreResponse::from).toList();
    }

    public TasteResponse findTaste(Long userId) {
        return new TasteResponse(
                favoriteArtistRepository.findArtistsByUserId(userId).stream()
                        .map(ArtistResponse::from).toList(),
                favoriteGenreRepository.findGenresByUserId(userId).stream()
                        .map(GenreResponse::from).toList()
        );
    }

    /** 취향을 저장한다. 부분 수정이 아니라 통째로 교체한다. */
    @Transactional
    public TasteResponse replaceTaste(Long userId, TasteRequest request) {
        Set<Long> artistIds = new LinkedHashSet<>(request.artistIds());
        Set<Long> genreIds = new LinkedHashSet<>(request.genreIds());

        // 존재하지 않는 id 가 섞여 들어오면 조용히 무시하지 말고 알려준다.
        if (artistRepository.countByIdIn(artistIds) != artistIds.size()) {
            throw new ApiException(ErrorCode.ARTIST_NOT_FOUND);
        }
        if (genreRepository.countByIdIn(genreIds) != genreIds.size()) {
            throw new ApiException(ErrorCode.GENRE_NOT_FOUND);
        }

        favoriteArtistRepository.deleteByIdUserId(userId);
        favoriteGenreRepository.deleteByIdUserId(userId);
        // delete 와 insert 사이에 flush 하지 않으면 같은 키로 insert 가 먼저 나가 충돌한다.
        favoriteArtistRepository.flush();
        favoriteGenreRepository.flush();

        favoriteArtistRepository.saveAll(
                artistIds.stream().map(id -> new UserFavoriteArtist(userId, id)).toList());
        favoriteGenreRepository.saveAll(
                genreIds.stream().map(id -> new UserFavoriteGenre(userId, id)).toList());

        return findTaste(userId);
    }

    public boolean hasTaste(Long userId) {
        return favoriteArtistRepository.existsByIdUserId(userId);
    }

    /**
     * 프로필 조회.
     *
     * <p>남의 프로필이면 나와 겹치는 취향을 함께 계산한다. 상대에게 말을 걸기 전에
     * "무슨 얘기를 할 수 있는지" 보여주는 것이 이 서비스의 핵심이라서다.
     */
    public UserProfileResponse findProfile(Long viewerId, Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        boolean mine = viewerId.equals(targetUserId);

        // 차단 관계면 존재 자체를 알리지 않는다. 403 을 주면 "차단당했구나" 를 알려주는 셈이다.
        if (!mine && safetyService.isBlockedBetween(viewerId, targetUserId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        // 공개 범위를 좁혀 둔 상대는 대화가 수락된 경우에만 전부 보여준다.
        if (!mine && settingsService.profileScopeOf(targetUserId) == ProfileScope.CHAT_ONLY
                && !hasAcceptedChat(viewerId, targetUserId)) {
            return restrictedProfile(target, viewerId);
        }

        List<Artist> targetArtists = favoriteArtistRepository.findArtistsByUserId(targetUserId);
        List<Genre> targetGenres = favoriteGenreRepository.findGenresByUserId(targetUserId);

        List<ArtistResponse> commonArtists = List.of();
        List<GenreResponse> commonGenres = List.of();

        if (!mine) {
            Set<Long> myArtistIds = Set.copyOf(favoriteArtistRepository.findArtistIdsByUserId(viewerId));
            Set<Long> myGenreIds = favoriteGenreRepository.findGenresByUserId(viewerId).stream()
                    .map(Genre::getId).collect(java.util.stream.Collectors.toSet());

            commonArtists = targetArtists.stream()
                    .filter(artist -> myArtistIds.contains(artist.getId()))
                    .map(ArtistResponse::from).toList();
            commonGenres = targetGenres.stream()
                    .filter(genre -> myGenreIds.contains(genre.getId()))
                    .map(GenreResponse::from).toList();
        }

        // 현재 곡을 비공개로 둔 사용자는 프로필에서도 감춘다.
        TrackResponse nowPlaying = (!mine && settingsService.hidesNowPlaying(targetUserId))
                ? null
                : nowPlayingRepository.findWithTrackByUserId(targetUserId)
                        .map(np -> TrackResponse.from(np.getTrack()))
                        .orElse(null);

        return new UserProfileResponse(
                target.getId(),
                target.getNickname(),
                target.getCreatedAt(),
                nowPlaying,
                targetArtists.stream().map(ArtistResponse::from).toList(),
                targetGenres.stream().map(GenreResponse::from).toList(),
                commonArtists,
                commonGenres,
                mine,
                false
        );
    }

    private boolean hasAcceptedChat(Long viewerId, Long targetUserId) {
        return chatRoomRepository.existsAcceptedBetween(
                Math.min(viewerId, targetUserId), Math.max(viewerId, targetUserId));
    }

    /**
     * 공개 범위가 좁혀진 프로필.
     *
     * <p>닉네임과 취향 일치도까지만 보여준다. 대화를 걸지 말지 판단하는 데는
     * 그 정도면 충분하고, 무엇을 듣는지까지 보여줄 이유는 없다.
     */
    private UserProfileResponse restrictedProfile(User target, Long viewerId) {
        return new UserProfileResponse(
                target.getId(),
                target.getNickname(),
                target.getCreatedAt(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                true);
    }
}
