package com.soundmap.music.dto;

import com.soundmap.music.client.dto.ITunesSearchResponse;
import com.soundmap.music.entity.TrackSource;

/**
 * 검색 결과 한 건. 아직 DB 에 저장되지 않은 상태다.
 * 사용자가 실제로 고른 곡만 {@code tracks} 테이블에 들어간다.
 */
public record TrackSearchResult(
        TrackSource source,
        String externalId,
        String title,
        String artistName,
        String albumName,
        String albumArtUrl,
        String previewUrl
) {
    public static TrackSearchResult fromITunes(ITunesSearchResponse.ITunesTrack track) {
        return new TrackSearchResult(
                TrackSource.ITUNES,
                String.valueOf(track.trackId()),
                track.trackName(),
                track.artistName(),
                track.collectionName(),
                upscaleArtwork(track.artworkUrl100()),
                track.previewUrl()
        );
    }

    /**
     * iTunes 는 100x100 썸네일 URL 을 준다. 지도 마커로 쓰기엔 흐려서
     * 파일명 부분만 바꿔 더 큰 이미지를 받는다. (문서화된 규칙은 아니지만 오래 동작해 온 방식이다)
     */
    private static String upscaleArtwork(String artworkUrl100) {
        if (artworkUrl100 == null) {
            return null;
        }
        return artworkUrl100.replace("100x100bb", "400x400bb");
    }
}
