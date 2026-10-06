import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { locationApi, mapApi, musicApi } from "../api/music";
import { useAuth } from "../auth/AuthContext";
import { useChatSocket } from "../chat/ChatSocketContext";
import FeatureNav from "../components/FeatureNav";
import SaveTrackButton from "../music/SaveTrackButton";
import Logo from "../components/Logo";
import KakaoMap from "../map/KakaoMap";
import { formatDistance, isPoorAccuracy, useGeolocation } from "../map/useGeolocation";
import PinList from "../map/PinList";
import PinSheet from "../map/PinSheet";
import TasteFilterBar from "../map/TasteFilterBar";
import TrackSearchPanel from "../map/TrackSearchPanel";

// 아직 위치를 공유하지 않은 사용자에게 보여줄 기본 화면 (한동대 부근)
const FALLBACK_CENTER = [36.1034, 129.3897];
/**
 * 카카오맵 축척. 라이플릿의 zoom 과 반대로 숫자가 작을수록 확대된다.
 * 5 는 눈금 250m 정도로, 기본 반경(2km) 안의 사람들이 화면에 들어온다.
 */
const DEFAULT_LEVEL = 5;
/**
 * "전국" 선택지가 쓰는 반경(m).
 *
 * 한반도 대각선(약 500km)을 넉넉히 덮는 값이라 사실상 거리 제한이 없다.
 * 백엔드의 max-radius-meters 와 같은 값이어야 한다. 더 크게 보내도 서버가 잘라낸다.
 */
const NATIONWIDE_METERS = 1000000;
const RADIUS_OPTIONS = [500, 1000, 2000, 5000, NATIONWIDE_METERS];
/**
 * 지도 자동 갱신 주기(ms).
 *
 * 옆 사람이 곡을 바꾸거나 새로 들어와도 내 화면은 그대로였다. 실시간이라고
 * 부를 만큼 잦을 필요는 없지만, 발표 중에 멈춰 있는 것처럼 보이면 곤란하다.
 */
const REFRESH_MS = 30000;

export default function MapPage() {
  const { user } = useAuth();
  const { locate, loading: locating } = useGeolocation();
  const { unreadCount } = useChatSocket();

  const [pins, setPins] = useState([]);
  const [nowPlaying, setNowPlaying] = useState(null);
  const [myPosition, setMyPosition] = useState(null);
  const [filter, setFilter] = useState({ radiusMeters: 2000 });
  const [sort, setSort] = useState("distance");
  const sortedPins = useMemo(() => [...pins].sort((a,b) => Number(b.mine)-Number(a.mine) || (sort==="taste" ? (b.matchScore??-1)-(a.matchScore??-1) : 0) || (a.distanceMeters??0)-(b.distanceMeters??0) || a.userId-b.userId), [pins,sort]);
  const [view, setView] = useState("map");
  const [selectedPin, setSelectedPin] = useState(null);
  const [searchOpen, setSearchOpen] = useState(false);
  const [status, setStatus] = useState(null);
  const requestSeq = useRef(0);

  const refresh = useCallback(async (activeFilter) => {
    const seq = ++requestSeq.current;
    const [pinList, current] = await Promise.all([
      mapApi.getPins({
        radiusMeters: activeFilter.radiusMeters,
        artistId: activeFilter.artistId ?? undefined,
        genreId: activeFilter.genreId ?? undefined,
      }),
      musicApi.getMyNowPlaying(),
    ]);

    // 조건을 빠르게 바꾸면 옛 응답이 나중에 도착할 수 있다.
    if (seq !== requestSeq.current) return null;

    setPins(pinList);
    setNowPlaying(current);

    const mine = pinList.find((pin) => pin.mine);
    if (mine) setMyPosition([mine.lat, mine.lon]);
    return pinList;
  }, []);

  useEffect(() => {
    refresh(filter).catch((err) => setStatus(err.message));
  }, [refresh, filter]);

  /**
   * 주기적으로 다시 불러온다.
   *
   * <p>보이지 않는 탭에서는 건너뛴다. 휴대폰을 주머니에 넣어 둔 동안에도 30초마다
   * 서버를 두드릴 이유가 없다. 대신 화면으로 돌아오는 순간 한 번 당겨 온다.
   *
   * <p>실패는 조용히 넘긴다. 사용자가 시킨 적 없는 요청이라 잠깐 끊겼다고
   * 화면에 오류를 띄우면 놀라기만 한다.
   */
  useEffect(() => {
    const pull = () => {
      if (document.visibilityState !== "visible") return;
      refresh(filter).catch(() => {});
    };

    const timer = setInterval(pull, REFRESH_MS);
    document.addEventListener("visibilitychange", pull);
    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", pull);
    };
  }, [refresh, filter]);

  const shareLocation = async () => {
    setStatus(null);
    try {
      const { lat, lon, accuracy } = await locate();
      const saved = await locationApi.update(lat, lon);
      setMyPosition([saved.lat, saved.lon]);
      const pinList = await refresh(filter);
      if (pinList) {
        const others = Math.max(pinList.length - 1, 0);
        const where = filter.radiusMeters === NATIONWIDE_METERS ? "전국에" : "주변에";
        // 오차가 크면 왜 마커가 엉뚱한 곳에 있는지 알려 준다. 모르면 고장으로 보인다.
        const note = isPoorAccuracy(accuracy)
          ? ` 다만 위치 오차가 약 ${formatDistance(accuracy)}로 큽니다. 실외에서 다시 눌러 보세요.`
          : "";
        setStatus(`${where} ${others}명이 있습니다.${note}`);
      }
    } catch (err) {
      setStatus(err.message);
    }
  };

  const handleSelectTrack = async (track) => {
    setStatus(null);
    try {
      const saved = await musicApi.checkIn({
        source: track.source,
        externalId: track.externalId,
        title: track.title,
        artistName: track.artistName,
        albumName: track.albumName,
        albumArtUrl: track.albumArtUrl,
        previewUrl: track.previewUrl,
      });
      setNowPlaying(saved);
      setSearchOpen(false);
      await refresh(filter);
    } catch (err) {
      setStatus(err.message);
    }
  };

  const stopSharingTrack = async () => {
    await musicApi.clearNowPlaying();
    setNowPlaying(null);
    await refresh(filter);
  };

  const nearbyCount = pins.filter((pin) => !pin.mine).length;
  const hasFilter = filter.artistId != null || filter.genreId != null;

  /*
   * 내 위치가 있어야 기준점이 생긴다. "전국"은 화면을 한참 벗어나 의미가 없다.
   *
   * 값이 그대로여도 렌더마다 새 객체를 만들면 지도가 원을 지웠다 다시 그린다.
   */
  const radiusCircle = useMemo(
    () =>
      myPosition && filter.radiusMeters !== NATIONWIDE_METERS
        ? { center: myPosition, meters: filter.radiusMeters }
        : null,
    [myPosition, filter.radiusMeters]
  );

  return (
    <div className="map-page">
      <header className="map-header">
        <span className="map-header__brand">
          <Logo size={24} />
          <span className="map-header__logo">잔향</span>
        </span>
        <span className="map-header__count">
          {hasFilter
            ? "조건 일치 "
            : filter.radiusMeters === NATIONWIDE_METERS
              ? "전국 "
              : "주변 "}
          {nearbyCount}명
        </span>
        <Link className="map-header__user" to={`/users/${user.id}`}>
          {user.nickname}
        </Link>
        <Link className="link-button chat-link" to="/chats">
          대화
          {unreadCount > 0 && <span className="unread-badge">{unreadCount}</span>}
        </Link>
        <Link className="link-button" to="/settings">
          설정
        </Link>
      </header>

      <FeatureNav/>
      <TasteFilterBar filter={filter} onChange={setFilter} />

      <div className="map-body">
        {view === "map" ? (
          <KakaoMap
            center={myPosition ?? FALLBACK_CENTER}
            level={DEFAULT_LEVEL}
            pins={pins}
            onSelect={setSelectedPin}
            radius={radiusCircle}
          />
        ) : (
          <PinList pins={sortedPins} onSelect={setSelectedPin} />
        )}

        <div className="map-overlay">
          <div className="view-toggle">
            <button
              type="button"
              className={view === "map" ? "view-toggle__on" : ""}
              onClick={() => setView("map")}
            >
              지도
            </button>
            <button
              type="button"
              className={view === "list" ? "view-toggle__on" : ""}
              onClick={() => setView("list")}
            >
              목록
            </button>
          </div>

          <label className="sort-picker">정렬 <select value={sort} onChange={e=>{setSort(e.target.value);setView("list");}}><option value="distance">가까운 순</option><option value="taste">취향이 비슷한 순</option></select></label>
          <div className="radius-picker">
            {RADIUS_OPTIONS.map((option) => (
              <button
                key={option}
                type="button"
                className={
                  option === filter.radiusMeters ? "radius-chip radius-chip--active" : "radius-chip"
                }
                onClick={() => {
                  setFilter((prev) => ({ ...prev, radiusMeters: option }));
                  // 전국은 상대가 수백 km 밖에 있어서 지도에서는 화면 밖으로 벗어난다.
                  // 그대로 두면 아무 일도 안 일어난 것처럼 보이므로 목록으로 넘긴다.
                  // 지도 버튼을 눌러 언제든 돌아올 수 있다.
                  if (option === NATIONWIDE_METERS) setView("list");
                }}
              >
                {formatRadiusLabel(option)}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="map-dock">
        {status && <p className="map-dock__status">{status}</p>}

        {nowPlaying ? (
          <div className="now-playing">
            <img className="now-playing__art" src={nowPlaying.track.albumArtUrl} alt="" />
            <div className="now-playing__text">
              <strong>{nowPlaying.track.title}</strong>
              <em>{nowPlaying.track.artistName}</em>
            </div>
            <SaveTrackButton track={nowPlaying.track}/>
            <button className="link-button" type="button" onClick={stopSharingTrack}>
              내리기
            </button>
          </div>
        ) : (
          <p className="hint">아직 올린 곡이 없습니다.</p>
        )}

        <div className="map-dock__actions">
          <button className="primary" type="button" onClick={() => setSearchOpen(true)}>
            {nowPlaying ? "곡 바꾸기" : "듣는 곡 올리기"}
          </button>
          <button className="secondary" type="button" onClick={shareLocation} disabled={locating}>
            {locating ? "위치 확인 중..." : myPosition ? "위치 갱신" : "내 위치 공유"}
          </button>
        </div>
      </div>

      {selectedPin && <PinSheet pin={selectedPin} onClose={() => setSelectedPin(null)} />}

      {searchOpen && (
        <TrackSearchPanel onSelect={handleSelectTrack} onClose={() => setSearchOpen(false)} />
      )}
    </div>
  );
}

function formatRadiusLabel(meters) {
  if (meters === NATIONWIDE_METERS) return "전국";
  return meters >= 1000 ? `${meters / 1000}km` : `${meters}m`;
}
