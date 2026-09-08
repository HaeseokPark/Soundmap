package com.soundmap.taste.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "genres",
        uniqueConstraints = @UniqueConstraint(name = "uk_genres_name", columnNames = "name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    public Genre(String name) {
        this.name = name;
    }
}
