package com.soundmap.taste.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(
        name = "user_favorite_genres",
        indexes = @Index(name = "idx_ufg_genre", columnList = "genre_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteGenre {

    @EmbeddedId
    private Key id;

    public UserFavoriteGenre(Long userId, Long genreId) {
        this.id = new Key(userId, genreId);
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public Long getGenreId() {
        return id.getGenreId();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "genre_id", nullable = false)
        private Long genreId;

        public Key(Long userId, Long genreId) {
            this.userId = userId;
            this.genreId = genreId;
        }
    }
}
