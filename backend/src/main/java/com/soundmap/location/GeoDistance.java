package com.soundmap.location;

import java.math.BigDecimal;

/**
 * 거리 계산과 바운딩 박스.
 *
 * <p>MySQL 에는 PostGIS 의 {@code ST_DWithin} 처럼 반경 검색이 인덱스를 타는 함수가 없다.
 * {@code ST_Distance_Sphere(...) < 2000} 로 쓰면 전체 스캔이 된다. 그래서 여기서 만든
 * 사각형으로 먼저 DB 에서 걸러내고(인덱스 사용), 정확한 원형 거리는 애플리케이션에서 계산한다.
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** 위도 1도의 길이(m). 경도는 위도에 따라 짧아지므로 cos 을 곱해 쓴다. */
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private GeoDistance() {
    }

    /** 두 좌표 사이의 대권 거리(m). */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double haversineMeters(BigDecimal lat1, BigDecimal lon1,
                                         BigDecimal lat2, BigDecimal lon2) {
        return haversineMeters(lat1.doubleValue(), lon1.doubleValue(),
                lat2.doubleValue(), lon2.doubleValue());
    }

    /**
     * 반경을 감싸는 최소 사각형. 원보다 넓게 잡히므로 여기에 걸린 결과는
     * 반드시 haversine 으로 한 번 더 걸러야 한다.
     */
    public static BoundingBox boundingBox(double lat, double lon, double radiusMeters) {
        double latDelta = radiusMeters / METERS_PER_DEGREE_LAT;

        // 극지방에서 cos 이 0 에 수렴하면 경도 폭이 발산한다. 최소값으로 막아준다.
        double cosLat = Math.max(Math.cos(Math.toRadians(lat)), 0.01);
        double lonDelta = radiusMeters / (METERS_PER_DEGREE_LAT * cosLat);

        return new BoundingBox(
                BigDecimal.valueOf(lat - latDelta),
                BigDecimal.valueOf(lat + latDelta),
                BigDecimal.valueOf(lon - lonDelta),
                BigDecimal.valueOf(lon + lonDelta)
        );
    }

    public record BoundingBox(BigDecimal minLat, BigDecimal maxLat,
                              BigDecimal minLon, BigDecimal maxLon) {
    }
}
