package com.soundmap.map.dto;

/**
 * 지도/목록 조회 조건.
 *
 * @param radiusMeters 생략 시 기본 반경, 최대치를 넘으면 잘린다
 * @param artistId     이 가수를 좋아하는 사람만
 * @param genreId      이 장르를 좋아하는 사람만
 */
public record PinFilter(Integer radiusMeters, Long artistId, Long genreId) {

    public static PinFilter of(Integer radiusMeters, Long artistId, Long genreId) {
        return new PinFilter(radiusMeters, artistId, genreId);
    }
}
