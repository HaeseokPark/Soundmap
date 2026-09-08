package com.soundmap.taste.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 "좋아하는 가수"로 고를 수 있는 아티스트 목록. */
@Entity
@Table(
        name = "artists",
        uniqueConstraints = @UniqueConstraint(name = "uk_artists_name", columnNames = "name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder
    private Artist(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
}
