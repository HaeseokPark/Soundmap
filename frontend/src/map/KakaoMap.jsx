import { useEffect, useRef, useState } from "react";

import { createAlbumMarkerElement } from "./albumMarker";
import { loadKakaoMaps } from "./kakaoLoader";

/**
 * 카카오맵이 허용하는 축척 범위. 숫자가 작을수록 확대다.
 * 끝에 닿으면 버튼을 눌러도 아무 일이 없으므로 미리 비활성으로 표시한다.
 */
const MIN_LEVEL = 1;
const MAX_LEVEL = 14;

/**
 * 카카오맵 화면.
 *
 * <p>SDK 가 명령형이라 React 가 그리는 부분은 빈 상자 하나뿐이다. 지도와 마커는
 * ref 로 들고 effect 에서 직접 만들고 지운다.
 */
export default function KakaoMap({ center, level, pins, onSelect, radius }) {
  const boxRef = useRef(null);
  const mapRef = useRef(null);
  const overlaysRef = useRef([]);
  const circleRef = useRef(null);
  // 지도를 만들 때 필요한 값들. 다시 만들 일이 없도록 effect 밖에 둔다.
  const startRef = useRef({ center, level });
  // 프로퍼티가 바뀔 때마다 마커를 다시 그리지 않도록 최신 콜백만 갈아 끼운다.
  const selectRef = useRef(onSelect);
  const lastCenterRef = useRef(null);

  const [ready, setReady] = useState(false);
  const [error, setError] = useState(null);
  // 버튼을 언제 잠글지 판단하려면 지금 축척을 알아야 한다.
  const [zoomLevel, setZoomLevel] = useState(level);

  useEffect(() => {
    selectRef.current = onSelect;
  }, [onSelect]);

  useEffect(() => {
    let cancelled = false;
    let observer = null;

    loadKakaoMaps()
      .then((maps) => {
        if (cancelled || !boxRef.current) return;

        const [lat, lon] = startRef.current.center;
        mapRef.current = new maps.Map(boxRef.current, {
          center: new maps.LatLng(lat, lon),
          level: startRef.current.level,
        });

        // 버튼 말고 손가락·휠로 확대해도 버튼 잠김 상태가 따라와야 한다.
        maps.event.addListener(mapRef.current, "zoom_changed", () =>
          setZoomLevel(mapRef.current.getLevel())
        );

        // 지도는 남는 높이를 채우는 칸에 들어간다. 만들어지는 시점에는 그 높이가
        // 아직 0 일 수 있고, 그러면 타일이 화면 일부에만 깔린다.
        observer = new ResizeObserver(() => mapRef.current?.relayout());
        observer.observe(boxRef.current);

        setReady(true);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });

    return () => {
      cancelled = true;
      observer?.disconnect();
      overlaysRef.current.forEach((overlay) => overlay.setMap(null));
      overlaysRef.current = [];
      circleRef.current?.setMap(null);
      circleRef.current = null;
      mapRef.current = null;
    };
  }, []);

  // 내 위치가 정해지면 그쪽으로 옮긴다. 좌표가 실제로 바뀐 경우에만 움직인다.
  // 매번 부르면 사용자가 지도를 옮겨 놓아도 원래 자리로 튕겨 돌아온다.
  useEffect(() => {
    if (!ready || !center) return;

    const key = center.join(",");
    if (lastCenterRef.current === key) return;
    lastCenterRef.current = key;

    mapRef.current.setCenter(new window.kakao.maps.LatLng(center[0], center[1]));
  }, [ready, center]);

  /**
   * 지금 보고 있는 반경을 원으로 그린다.
   *
   * <p>반경을 바꿔도 지도에는 아무 변화가 없어서, 목록 개수가 왜 달라졌는지
   * 알기 어려웠다. 원을 깔아 두면 "이 안에 있는 사람들"이 눈으로 보인다.
   *
   * <p>내 위치가 없으면 기준점이 없어 그리지 않는다. "전국"처럼 화면을 한참
   * 벗어나는 반경도 호출한 쪽에서 걸러 보낸다.
   */
  useEffect(() => {
    if (!ready) return undefined;

    circleRef.current?.setMap(null);
    circleRef.current = null;

    if (!radius) return undefined;

    const maps = window.kakao.maps;
    circleRef.current = new maps.Circle({
      center: new maps.LatLng(radius.center[0], radius.center[1]),
      radius: radius.meters,
      strokeWeight: 2,
      strokeColor: "#3182f6",
      strokeOpacity: 0.7,
      strokeStyle: "solid",
      fillColor: "#3182f6",
      fillOpacity: 0.06,
    });
    circleRef.current.setMap(mapRef.current);

    return () => {
      circleRef.current?.setMap(null);
      circleRef.current = null;
    };
  }, [ready, radius]);

  useEffect(() => {
    if (!ready) return;

    const maps = window.kakao.maps;
    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = pins.map((pin) => {
      const element = createAlbumMarkerElement({
        albumArtUrl: pin.track?.albumArtUrl,
        mine: pin.mine,
      });
      element.addEventListener("click", () => selectRef.current(pin));

      const overlay = new maps.CustomOverlay({
        position: new maps.LatLng(pin.lat, pin.lon),
        content: element,
        // 앨범 커버의 한가운데가 좌표에 오게 한다. 기본값은 아래쪽 끝이라
        // 핀 모양이 아닌 원형 마커에는 맞지 않는다.
        xAnchor: 0.5,
        yAnchor: 0.5,
        // 내 마커가 남의 마커에 가리지 않게 한 단계 위로 올린다.
        zIndex: pin.mine ? 2 : 1,
      });
      overlay.setMap(mapRef.current);
      return overlay;
    });
  }, [ready, pins]);

  if (error) {
    return (
      <div className="map-canvas map-canvas--error">
        <p className="hint">{error}</p>
      </div>
    );
  }

  /**
   * 카카오는 숫자가 작을수록 확대라 요청한 방향과 부호가 반대다.
   *
   * <p>{@code animate} 옵션은 쓰지 않는다. 붙이면 한 단계는 움직이지만 그 뒤로
   * 지도가 축척을 바꾸지 않는다. 버튼이든 SDK 직접 호출이든 마찬가지라
   * 애니메이션이 끝났다는 신호를 못 받고 잠기는 것으로 보인다.
   */
  const changeZoom = (step) => {
    const map = mapRef.current;
    if (!map) return;
    map.setLevel(map.getLevel() - step);
    setZoomLevel(map.getLevel());
  };

  return (
    <>
      <div className="map-canvas" ref={boxRef} />

      {ready && (
        <div className="zoom-control">
          <button
            type="button"
            aria-label="확대"
            onClick={() => changeZoom(1)}
            disabled={zoomLevel <= MIN_LEVEL}
          >
            +
          </button>
          <button
            type="button"
            aria-label="축소"
            onClick={() => changeZoom(-1)}
            disabled={zoomLevel >= MAX_LEVEL}
          >
            −
          </button>
        </div>
      )}
    </>
  );
}
