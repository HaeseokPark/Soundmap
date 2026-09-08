package com.soundmap.music.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ITunesSearchResponse(int resultCount, List<ITunesTrack> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ITunesTrack(
            Long trackId,
            String trackName,
            Long artistId,
            String artistName,
            String collectionName,
            String artworkUrl100,
            String previewUrl
    ) {
    }
}
