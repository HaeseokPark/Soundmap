package com.soundmap.music.dto;

import com.soundmap.music.entity.Track;

public record TrackResponse(
        Long id,
        String title,
        String artistName,
        String albumName,
        String albumArtUrl,
        String previewUrl
) {
    public static TrackResponse from(Track track) {
        return new TrackResponse(
                track.getId(),
                track.getTitle(),
                track.getArtistName(),
                track.getAlbumName(),
                track.getAlbumArtUrl(),
                track.getPreviewUrl()
        );
    }
}
