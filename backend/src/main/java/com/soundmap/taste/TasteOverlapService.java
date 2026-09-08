package com.soundmap.taste;

import com.soundmap.taste.dto.ArtistResponse;
import com.soundmap.taste.entity.Artist;
import com.soundmap.taste.entity.Genre;
import com.soundmap.taste.repository.TasteQueryRepository;
import com.soundmap.taste.repository.UserFavoriteArtistRepository;
import com.soundmap.taste.repository.UserFavoriteGenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 두 사용자의 취향이 얼마나 겹치는지 계산해 주는 조회 전용 서비스.
 *
 * <p>지도, 프로필, 채팅이 모두 같은 계산을 필요로 해서 한 곳으로 모았다.
 * 채팅방 목록처럼 상대가 여러 명인 경우를 위해 묶음 계산도 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteOverlapService {

    private final UserFavoriteArtistRepository favoriteArtistRepository;
    private final UserFavoriteGenreRepository favoriteGenreRepository;
    private final TasteQueryRepository tasteQueryRepository;

    /**
     * @param matchScore    0~100
     * @param commonArtists 겹치는 가수. 이름을 보여줘야 하므로 엔티티에서 옮겨 담는다.
     */
    public record Overlap(int matchScore, List<ArtistResponse> commonArtists) {

        public static Overlap empty() {
            return new Overlap(0, List.of());
        }
    }

    public Overlap between(Long viewerId, Long otherId) {
        Map<Long, Artist> myArtistById = myArtistsById(viewerId);
        Set<Long> myGenreIds = myGenreIds(viewerId);

        Set<Long> theirArtists = idsOf(tasteQueryRepository.findArtistPairsByUserIds(List.of(otherId)));
        Set<Long> theirGenres = idsOf(tasteQueryRepository.findGenrePairsByUserIds(List.of(otherId)));

        List<ArtistResponse> common = theirArtists.stream()
                .filter(myArtistById::containsKey)
                .map(id -> ArtistResponse.from(myArtistById.get(id)))
                .sorted(Comparator.comparing(ArtistResponse::name))
                .toList();

        int score = TasteMatch.combinedScore(myArtistById.keySet(), theirArtists, myGenreIds, theirGenres);
        return new Overlap(score, common);
    }

    /**
     * 여러 상대에 대한 일치도를 한 번에 계산한다.
     * 채팅방 목록처럼 상대가 여러 명일 때 쿼리가 사람 수만큼 늘어나는 것을 막는다.
     */
    public Map<Long, Integer> scoresFor(Long viewerId, Collection<Long> otherIds) {
        if (otherIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> myArtistIds = myArtistsById(viewerId).keySet();
        Set<Long> myGenreIds = myGenreIds(viewerId);

        Map<Long, Set<Long>> artistsByUser = groupPairs(
                tasteQueryRepository.findArtistPairsByUserIds(otherIds));
        Map<Long, Set<Long>> genresByUser = groupPairs(
                tasteQueryRepository.findGenrePairsByUserIds(otherIds));

        Map<Long, Integer> scores = new HashMap<>();
        for (Long otherId : otherIds) {
            scores.put(otherId, TasteMatch.combinedScore(
                    myArtistIds,
                    artistsByUser.getOrDefault(otherId, Set.of()),
                    myGenreIds,
                    genresByUser.getOrDefault(otherId, Set.of())));
        }
        return scores;
    }

    private Map<Long, Artist> myArtistsById(Long userId) {
        return favoriteArtistRepository.findArtistsByUserId(userId).stream()
                .collect(Collectors.toMap(Artist::getId, Function.identity()));
    }

    private Set<Long> myGenreIds(Long userId) {
        return favoriteGenreRepository.findGenresByUserId(userId).stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
    }

    private Set<Long> idsOf(List<Object[]> pairs) {
        Set<Long> ids = new HashSet<>();
        for (Object[] pair : pairs) {
            ids.add((Long) pair[1]);
        }
        return ids;
    }

    private Map<Long, Set<Long>> groupPairs(List<Object[]> pairs) {
        Map<Long, Set<Long>> grouped = new HashMap<>();
        for (Object[] pair : pairs) {
            grouped.computeIfAbsent((Long) pair[0], key -> new HashSet<>()).add((Long) pair[1]);
        }
        return grouped;
    }
}
