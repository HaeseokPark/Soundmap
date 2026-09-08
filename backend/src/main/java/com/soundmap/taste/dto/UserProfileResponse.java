package com.soundmap.taste.dto;

import com.soundmap.music.dto.TrackResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 프로필. 상대에게 말을 걸기 전에 어떤 음악을 좋아하는 사람인지 확인하는 화면.
 *
 * @param commonArtists 나와 겹치는 가수. 내 프로필을 볼 때는 빈 목록이다.
 * @param restricted    상대가 공개 범위를 좁혀 둔 경우 true. 취향과 재생곡이 비어 온다.
 */
public record UserProfileResponse(
        Long userId,
        String nickname,
        LocalDateTime joinedAt,
        TrackResponse nowPlaying,
        List<ArtistResponse> favoriteArtists,
        List<GenreResponse> favoriteGenres,
        List<ArtistResponse> commonArtists,
        List<GenreResponse> commonGenres,
        boolean mine,
        boolean restricted
) {
}
