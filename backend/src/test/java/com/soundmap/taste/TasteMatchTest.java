package com.soundmap.taste;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasteMatchTest {

    @Test
    @DisplayName("완전히 같으면 100")
    void identicalIsFull() {
        assertEquals(100, TasteMatch.score(Set.of(1L, 2L, 3L), Set.of(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("겹치는 것이 없으면 0")
    void disjointIsZero() {
        assertEquals(0, TasteMatch.score(Set.of(1L, 2L), Set.of(3L, 4L)));
    }

    @Test
    @DisplayName("한쪽이 비어 있으면 0")
    void emptyIsZero() {
        assertEquals(0, TasteMatch.score(Set.of(), Set.of(1L)));
        assertEquals(0, TasteMatch.score(Set.of(1L), Set.of()));
    }

    @Test
    @DisplayName("교집합 2 / 합집합 5 = 40")
    void jaccardExample() {
        assertEquals(40, TasteMatch.score(Set.of(1L, 2L, 3L), Set.of(1L, 3L, 4L, 5L)));
    }

    @Test
    @DisplayName("취향을 넓게 고른다고 점수가 높아지지 않는다")
    void breadthDoesNotInflateScore() {
        Set<Long> mine = Set.of(1L, 2L, 3L);

        // 좁게 고른 사람: 3개 중 3개가 겹친다
        int focused = TasteMatch.score(mine, Set.of(1L, 2L, 3L));
        // 넓게 고른 사람: 겹치는 수는 같지만 고른 것이 훨씬 많다
        int broad = TasteMatch.score(mine, Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        assertTrue(focused > broad,
                "교집합만 세면 넓게 고른 쪽이 유리해진다. 자카드는 그러면 안 된다 (%d vs %d)"
                        .formatted(focused, broad));
    }

    @Test
    @DisplayName("가수가 장르보다 점수에 크게 기여한다")
    void artistsWeighMoreThanGenres() {
        Set<Long> mine = Set.of(1L, 2L, 3L);

        int artistOnly = TasteMatch.combinedScore(mine, mine, Set.of(1L), Set.of(9L));
        int genreOnly = TasteMatch.combinedScore(mine, Set.of(9L), Set.of(1L), Set.of(1L));

        assertEquals(70, artistOnly);
        assertEquals(30, genreOnly);
        assertTrue(artistOnly > genreOnly);
    }

    @Test
    @DisplayName("점수는 항상 0~100 사이")
    void alwaysWithinRange() {
        int score = TasteMatch.combinedScore(
                Set.of(1L, 2L, 3L), Set.of(1L, 2L, 3L),
                Set.of(1L, 2L), Set.of(1L, 2L));

        assertTrue(score >= 0 && score <= 100, "범위를 벗어남: " + score);
        assertEquals(100, score);
    }
}
