package com.soundmap.taste.dto;

import com.soundmap.taste.entity.Genre;

public record GenreResponse(Long id, String name) {

    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName());
    }
}
