import { useCallback, useState } from "react";

/**
 * 브라우저에서 현재 좌표를 읽어온다.
 *
 * <p>Geolocation API 는 보안 컨텍스트(HTTPS 또는 localhost)에서만 동작한다.
 * 폰에서 192.168.x.x 로 접속하면 여기서 실패하므로, 실기기 테스트는
 * HTTPS 로 배포한 주소에서 해야 한다.
 */

/**
 * 더 나은 좌표를 기다리는 시간(ms).
 *
 * <p>GPS 는 켜자마자 정확해지지 않는다. 처음에는 캐시나 WiFi 기반의 거친 좌표를
 * 주고 몇 초에 걸쳐 정밀해진다. {@code getCurrentPosition} 은 가장 먼저 도착한
 * 값을 돌려주기 때문에, 대개 그 거친 첫 값을 쓰게 된다. 그래서 잠시 지켜보며
 * 가장 정확한 값을 고른다.
 */
const SAMPLE_MS = 5000;

/** 이 정도면 충분하다. 더 기다리지 않고 바로 쓴다. */
const GOOD_ENOUGH_METERS = 20;

/** 이보다 나쁘면 "대략 이 동네" 수준이라 사용자에게 알린다. */
const POOR_ACCURACY_METERS = 100;

/**
 * 이보다 나쁘면 저장하지 않는다.
 *
 * <p>IP 기반 추정은 몇 km 씩 틀린다. 그런 좌표를 지도에 찍으면 엉뚱한 동네에
 * 내가 있는 것으로 남는다. 다만 노트북의 WiFi 측위는 보통 100~500m 라서,
 * 기준을 너무 조이면 PC 에서는 위치 공유 자체가 막힌다. 그 사이에 선을 둔다.
 */
const MAX_ACCEPTABLE_METERS = 1000;

export function useGeolocation() {
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const locate = useCallback(() => {
    setError(null);

    if (!navigator.geolocation) {
      const message = "이 브라우저는 위치 기능을 지원하지 않습니다.";
      setError(message);
      return Promise.reject(new Error(message));
    }

    setLoading(true);
    return new Promise((resolve, reject) => {
      let best = null;
      let settled = false;
      let watchId = null;
      let timer = null;

      const stop = () => {
        clearTimeout(timer);
        if (watchId != null) navigator.geolocation.clearWatch(watchId);
        setLoading(false);
      };

      const fail = (message) => {
        if (settled) return;
        settled = true;
        stop();
        setError(message);
        reject(new Error(message));
      };

      const finish = () => {
        if (settled) return;

        if (!best) {
          fail("현재 위치를 가져오지 못했습니다.");
          return;
        }

        const accuracy = Math.round(best.accuracy);
        if (accuracy > MAX_ACCEPTABLE_METERS) {
          fail(
            `위치가 너무 부정확합니다 (오차 약 ${formatDistance(accuracy)}). ` +
              "실외에서 다시 시도하거나 기기의 위치 서비스를 켜 주세요."
          );
          return;
        }

        settled = true;
        stop();
        resolve({ lat: best.lat, lon: best.lon, accuracy });
      };

      watchId = navigator.geolocation.watchPosition(
        (position) => {
          const { latitude, longitude, accuracy } = position.coords;
          if (!best || accuracy < best.accuracy) {
            best = { lat: latitude, lon: longitude, accuracy };
          }
          if (best.accuracy <= GOOD_ENOUGH_METERS) {
            finish();
          }
        },
        (positionError) => {
          // 지켜보는 동안 실패해도, 앞서 받아 둔 좌표가 있으면 그걸 쓴다.
          if (best) {
            finish();
            return;
          }
          fail(
            positionError.code === positionError.PERMISSION_DENIED
              ? "위치 권한이 거부되었습니다. 브라우저 설정에서 허용해 주세요."
              : "현재 위치를 가져오지 못했습니다."
          );
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
      );

      timer = setTimeout(finish, SAMPLE_MS);
    });
  }, []);

  return { locate, error, loading };
}

/** 오차가 어느 정도인지 사람이 읽을 수 있게. */
export function formatDistance(meters) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${meters}m`;
}

export function isPoorAccuracy(meters) {
  return meters > POOR_ACCURACY_METERS;
}
