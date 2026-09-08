package com.soundmap.location;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoDistanceTest {

    private static final double CAMPUS_LAT = 36.1034;
    private static final double CAMPUS_LON = 129.3897;

    @Test
    @DisplayName("같은 지점의 거리는 0이다")
    void zeroForSamePoint() {
        assertEquals(0.0, GeoDistance.haversineMeters(CAMPUS_LAT, CAMPUS_LON, CAMPUS_LAT, CAMPUS_LON), 0.001);
    }

    @Test
    @DisplayName("위도 0.001도는 약 111m")
    void oneGridCellIsAboutHundredMeters() {
        double meters = GeoDistance.haversineMeters(
                CAMPUS_LAT, CAMPUS_LON, CAMPUS_LAT + 0.001, CAMPUS_LON);

        assertEquals(111.0, meters, 1.5);
    }

    @Test
    @DisplayName("같은 위도에서 경도 1도는 위도 1도보다 짧다")
    void longitudeShrinksWithLatitude() {
        double alongLat = GeoDistance.haversineMeters(CAMPUS_LAT, CAMPUS_LON, CAMPUS_LAT + 1, CAMPUS_LON);
        double alongLon = GeoDistance.haversineMeters(CAMPUS_LAT, CAMPUS_LON, CAMPUS_LAT, CAMPUS_LON + 1);

        assertTrue(alongLon < alongLat,
                "경도 1도(%.0fm)는 위도 1도(%.0fm)보다 짧아야 한다".formatted(alongLon, alongLat));
    }

    @Test
    @DisplayName("서울 - 부산 거리는 약 325km")
    void knownLongDistance() {
        double meters = GeoDistance.haversineMeters(37.5665, 126.9780, 35.1796, 129.0756);

        assertEquals(325_000, meters, 5_000);
    }

    @Test
    @DisplayName("바운딩 박스는 반경 위의 점을 모두 포함한다")
    void boundingBoxCoversTheCircle() {
        int radius = 2000;
        GeoDistance.BoundingBox box = GeoDistance.boundingBox(CAMPUS_LAT, CAMPUS_LON, radius);

        // 원 위의 점 36개를 찍어 사각형 안에 들어오는지 본다.
        for (int degrees = 0; degrees < 360; degrees += 10) {
            double angle = Math.toRadians(degrees);
            double lat = CAMPUS_LAT + (radius * Math.cos(angle)) / 111_320.0;
            double lon = CAMPUS_LON + (radius * Math.sin(angle))
                    / (111_320.0 * Math.cos(Math.toRadians(CAMPUS_LAT)));

            assertTrue(lat >= box.minLat().doubleValue() && lat <= box.maxLat().doubleValue(),
                    "위도 %f 가 박스를 벗어났다 (%d도)".formatted(lat, degrees));
            assertTrue(lon >= box.minLon().doubleValue() && lon <= box.maxLon().doubleValue(),
                    "경도 %f 가 박스를 벗어났다 (%d도)".formatted(lon, degrees));
        }
    }

    @Test
    @DisplayName("바운딩 박스는 원보다 넓다 - 그래서 거리 재검증이 필요하다")
    void boundingBoxIsWiderThanCircle() {
        int radius = 2000;
        GeoDistance.BoundingBox box = GeoDistance.boundingBox(CAMPUS_LAT, CAMPUS_LON, radius);

        // 사각형의 모서리는 반경 밖이다. DB 에서 사각형으로만 거르면 이런 점이 딸려온다.
        double cornerDistance = GeoDistance.haversineMeters(
                CAMPUS_LAT, CAMPUS_LON,
                box.maxLat().doubleValue(), box.maxLon().doubleValue());

        assertTrue(cornerDistance > radius,
                "모서리까지 거리(%.0fm)는 반경(%dm)보다 커야 한다".formatted(cornerDistance, radius));
    }
}
