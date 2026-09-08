package com.soundmap.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** 브라우저 Geolocation API 가 준 원본 좌표. 서버는 이 값을 저장하지 않는다. */
public record LocationRequest(

        @NotNull(message = "위도가 필요합니다.")
        @DecimalMin(value = "-90.0", message = "위도 범위를 벗어났습니다.")
        @DecimalMax(value = "90.0", message = "위도 범위를 벗어났습니다.")
        Double lat,

        @NotNull(message = "경도가 필요합니다.")
        @DecimalMin(value = "-180.0", message = "경도 범위를 벗어났습니다.")
        @DecimalMax(value = "180.0", message = "경도 범위를 벗어났습니다.")
        Double lon
) {
}
