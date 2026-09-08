package com.soundmap.music.dto;

import com.soundmap.music.entity.TrackSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 검색 결과에서 고른 곡을 그대로 돌려보낸다. */
public record NowPlayingRequest(

        @NotNull(message = "곡 출처가 필요합니다.")
        TrackSource source,

        @NotBlank(message = "곡 식별자가 필요합니다.")
        @Size(max = 64)
        String externalId,

        @NotBlank(message = "곡 제목이 필요합니다.")
        @Size(max = 300)
        String title,

        @NotBlank(message = "가수 이름이 필요합니다.")
        @Size(max = 200)
        String artistName,

        @Size(max = 300)
        String albumName,

        @Size(max = 500)
        String albumArtUrl,

        /** 30초 미리듣기 주소. 곡에 따라 없을 수 있어서 필수가 아니다. */
        @Size(max = 500)
        String previewUrl
) {
}
