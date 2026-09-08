package com.soundmap.music.dto;

import com.soundmap.music.entity.NowPlaying;

import java.time.LocalDateTime;

public record NowPlayingResponse(
        TrackResponse track,
        LocalDateTime updatedAt
) {
    public static NowPlayingResponse from(NowPlaying nowPlaying) {
        return new NowPlayingResponse(
                TrackResponse.from(nowPlaying.getTrack()),
                nowPlaying.getUpdatedAt()
        );
    }
}
