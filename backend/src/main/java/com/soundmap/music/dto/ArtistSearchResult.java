package com.soundmap.music.dto;

/**
 * iTunes 에서 찾은 가수 한 명. 아직 DB 에 없을 수 있다.
 *
 * <p>iTunes 의 {@code entity=musicArtist} 는 이미지를 주지 않고, 한글 이름으로
 * 검색해도 영문 표기("검정치마" → "The Black Skirts")로 돌려준다. 그래서 곡을
 * 검색해서({@code entity=song&attribute=artistTerm}) 그 곡들의 가수 정보를
 * 쓴다. 이쪽은 사용자가 입력한 대로의 이름과 앨범아트가 함께 온다.
 */
public record ArtistSearchResult(
        Long externalId,
        String name,
        String imageUrl
) {
    /**
     * iTunes 는 100x100 썸네일을 준다. 원형 아바타로 쓰기엔 흐려서 더 큰 이미지를 받는다.
     * (곡 앨범아트와 같은 규칙이다)
     */
    public static ArtistSearchResult of(Long artistId, String artistName, String artworkUrl100) {
        String image = artworkUrl100 == null
                ? null
                : artworkUrl100.replace("100x100bb", "400x400bb");
        return new ArtistSearchResult(artistId, artistName, image);
    }
}
