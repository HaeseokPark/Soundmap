package com.soundmap.taste.dto;

import java.util.List;

public record TasteResponse(
        List<ArtistResponse> artists,
        List<GenreResponse> genres
) {
}
