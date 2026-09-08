package com.soundmap.map.dto;

import com.soundmap.music.dto.TrackResponse;
import com.soundmap.taste.dto.ArtistResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지도 위의 마커 하나. 목록 화면도 같은 데이터를 쓴다.
 *
 * <p>좌표는 격자로 스냅된 값이라 실제 위치와 최대 100m 정도 차이가 난다.
 *
 * @param track          아직 곡을 올리지 않은 사용자는 null
 * @param distanceMeters 내 마커면 null. 스냅된 좌표로 계산하므로 어차피 근사값이다.
 * @param matchScore     0~100 취향 일치도. 내 마커면 null.
 * @param commonArtists  나와 겹치는 가수. 대화를 시작할 실마리로 쓴다.
 */
public record MapPin(
        Long userId,
        String nickname,
        BigDecimal lat,
        BigDecimal lon,
        TrackResponse track,
        LocalDateTime updatedAt,
        Integer distanceMeters,
        Integer matchScore,
        List<ArtistResponse> commonArtists,
        boolean mine
) {
}
