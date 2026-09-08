package com.soundmap.location;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 격자 스냅은 이 서비스의 위치 프라이버시가 기대는 유일한 장치다.
 * 여기가 깨지면 사용자의 실제 좌표가 노출되므로 테스트로 고정해 둔다.
 */
class GeoGridTest {

    private static final BigDecimal STEP = new BigDecimal("0.001");

    @Test
    @DisplayName("같은 입력은 항상 같은 격자로 떨어진다")
    void deterministic() {
        double lat = 36.103419;

        BigDecimal first = GeoGrid.snap(lat, STEP);
        for (int i = 0; i < 100; i++) {
            assertEquals(first, GeoGrid.snap(lat, STEP));
        }
    }

    @Test
    @DisplayName("한 칸 안의 서로 다른 좌표는 같은 값으로 뭉개진다")
    void collapsesWithinCell() {
        BigDecimal a = GeoGrid.snap(36.103419, STEP);
        BigDecimal b = GeoGrid.snap(36.103201, STEP);

        assertEquals(a, b);
        assertEquals(new BigDecimal("36.103000"), a);
    }

    @Test
    @DisplayName("반복 조회해도 평균이 실제 좌표로 수렴하지 않는다")
    void resistsAveragingAttack() {
        // 무작위 오프셋 방식이라면 여러 번 관측해 평균 내면 실제 좌표가 복원된다.
        // 결정론적 스냅은 매번 같은 값이라 관측을 아무리 모아도 정보가 늘지 않는다.
        double actualLat = 36.1034567;

        BigDecimal observed = GeoGrid.snap(actualLat, STEP);
        BigDecimal sum = BigDecimal.ZERO;
        int samples = 1000;
        for (int i = 0; i < samples; i++) {
            sum = sum.add(GeoGrid.snap(actualLat, STEP));
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(samples), 6, java.math.RoundingMode.HALF_UP);

        assertEquals(observed, average);
        assertNotEquals(0, BigDecimal.valueOf(actualLat).compareTo(average));
    }

    @Test
    @DisplayName("다른 칸은 다른 값으로 남는다")
    void differentCellsStayDifferent() {
        assertNotEquals(GeoGrid.snap(36.1030, STEP), GeoGrid.snap(36.1050, STEP));
    }

    @Test
    @DisplayName("남반구/서반구 좌표도 동작한다")
    void handlesNegativeCoordinates() {
        // 절대값 기준으로 반올림한다: -33.8688 은 -33.869 쪽이 더 가깝다.
        assertEquals(new BigDecimal("-33.869000"), GeoGrid.snap(-33.8688, STEP));
        assertEquals(new BigDecimal("-70.123000"), GeoGrid.snap(-70.1232, STEP));
    }

    @Test
    @DisplayName("격자를 키우면 더 넓게 뭉개진다")
    void largerGridHidesMore() {
        BigDecimal coarse = new BigDecimal("0.01");

        assertEquals(GeoGrid.snap(36.1034, coarse), GeoGrid.snap(36.1012, coarse));
        assertNotEquals(GeoGrid.snap(36.1034, STEP), GeoGrid.snap(36.1012, STEP));
    }

    @Test
    @DisplayName("좌표 범위 검증")
    void validatesRanges() {
        org.junit.jupiter.api.Assertions.assertTrue(GeoGrid.isValidLatitude(36.1));
        org.junit.jupiter.api.Assertions.assertFalse(GeoGrid.isValidLatitude(91.0));
        org.junit.jupiter.api.Assertions.assertTrue(GeoGrid.isValidLongitude(129.3));
        org.junit.jupiter.api.Assertions.assertFalse(GeoGrid.isValidLongitude(181.0));
    }
}
