package com.soundmap.taste;

import java.util.HashSet;
import java.util.Set;

/**
 * 두 사용자의 음악 취향이 얼마나 겹치는지 계산한다.
 *
 * <p>자카드 유사도를 쓴다. 교집합 크기를 합집합 크기로 나눈 값이다.
 * 교집합만 세면 취향이 넓은 사람(가수를 30명 고른 사람)이 무조건 유리해지므로,
 * 합집합으로 나눠 "고른 것 대비 얼마나 겹치는가"로 바꾼다.
 *
 * <pre>
 *   A = {DAY6, 아이유, 혁오}
 *   B = {DAY6, 혁오, aespa, 뉴진스}
 *   교집합 2, 합집합 5  ->  40%
 * </pre>
 */
public final class TasteMatch {

    private TasteMatch() {
    }

    /**
     * @return 0~100 사이의 정수. 둘 중 한쪽이라도 비어 있으면 0.
     */
    public static int score(Set<Long> mine, Set<Long> theirs) {
        if (mine.isEmpty() || theirs.isEmpty()) {
            return 0;
        }

        Set<Long> intersection = new HashSet<>(mine);
        intersection.retainAll(theirs);
        if (intersection.isEmpty()) {
            return 0;
        }

        Set<Long> union = new HashSet<>(mine);
        union.addAll(theirs);

        return (int) Math.round(intersection.size() * 100.0 / union.size());
    }

    /**
     * 가수와 장르를 함께 본 점수.
     *
     * <p>가수 쪽에 더 큰 가중치를 둔다. 장르는 종류가 14개뿐이라 우연히 겹치기 쉬운 반면,
     * 같은 가수를 좋아한다는 건 훨씬 구체적인 신호이기 때문이다.
     */
    public static int combinedScore(Set<Long> myArtists, Set<Long> theirArtists,
                                    Set<Long> myGenres, Set<Long> theirGenres) {
        int artistScore = score(myArtists, theirArtists);
        int genreScore = score(myGenres, theirGenres);

        return (int) Math.round(artistScore * 0.7 + genreScore * 0.3);
    }
}
