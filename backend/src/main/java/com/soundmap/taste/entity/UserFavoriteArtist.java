package com.soundmap.taste.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 사용자 - 좋아하는 가수.
 *
 * <p>복합 기본키를 쓴다. 같은 조합이 두 번 저장되는 것을 구조적으로 막고,
 * PK 인덱스가 "이 사용자의 가수 목록" 조회에 그대로 쓰인다.
 * 반대 방향("이 가수를 좋아하는 사람")은 6주차 검색에서 쓰므로 별도 인덱스를 둔다.
 */
@Entity
@Table(
        name = "user_favorite_artists",
        indexes = @Index(name = "idx_ufa_artist", columnList = "artist_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteArtist {

    @EmbeddedId
    private Key id;

    public UserFavoriteArtist(Long userId, Long artistId) {
        this.id = new Key(userId, artistId);
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public Long getArtistId() {
        return id.getArtistId();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "artist_id", nullable = false)
        private Long artistId;

        public Key(Long userId, Long artistId) {
            this.userId = userId;
            this.artistId = artistId;
        }
    }
}
