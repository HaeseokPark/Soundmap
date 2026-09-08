package com.soundmap.taste.dto;

import com.soundmap.taste.entity.Artist;

public record ArtistResponse(Long id, String name, String imageUrl) {

    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(artist.getId(), artist.getName(), artist.getImageUrl());
    }
}
