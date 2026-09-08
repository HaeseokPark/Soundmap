package com.soundmap.location;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 좌표를 고정 격자에 스냅한다.
 *
 * <p>매 요청마다 무작위 오프셋을 더하는 방식은 쓰면 안 된다. 같은 사용자를 여러 번
 * 조회해서 평균을 내면 실제 좌표가 복원되기 때문이다(averaging attack). 여기서는
 * 같은 입력이 항상 같은 격자로 떨어지는 결정론적 방식을 쓴다.
 *
 * <p>기본 격자 0.001도는 위도로 약 111m, 경도로 (한국 기준) 약 90m 이다.
 */
public final class GeoGrid {

    private static final int STORED_SCALE = 6;

    private GeoGrid() {
    }

    public static BigDecimal snap(double value, BigDecimal step) {
        BigDecimal cells = BigDecimal.valueOf(value)
                .divide(step, 0, RoundingMode.HALF_UP);
        return cells.multiply(step).setScale(STORED_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isValidLatitude(double lat) {
        return lat >= -90.0 && lat <= 90.0;
    }

    public static boolean isValidLongitude(double lon) {
        return lon >= -180.0 && lon <= 180.0;
    }
}
