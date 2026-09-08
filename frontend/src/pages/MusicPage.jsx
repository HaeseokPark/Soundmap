import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { discoveryApi } from "../api/discovery";
import { locationApi } from "../api/music";
import TrackCard from "../music/TrackCard";
import { useLibrary } from "../music/libraryState";
import FeatureNav from "../components/FeatureNav";
import { stop } from "../music/previewPlayer";
const currentMonth = () =>
  new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
  }).format(new Date());
const titles = {
  library: "내 음악 보관함",
  history: "내가 공유한 음악",
  chart: "오늘의 동네 차트",
};
export default function MusicPage() {
  const { section } = useParams();
  const kind = titles[section] ? section : "library";
  const { ids, ready } = useLibrary();
  const [entries, setEntries] = useState([]),
    [artists, setArtists] = useState([]),
    [page, setPage] = useState(0),
    [hasMore, setHasMore] = useState(false);
  const [month, setMonth] = useState(currentMonth),
    [radius, setRadius] = useState(2000),
    [loading, setLoading] = useState(false),
    [error, setError] = useState(""),
    [located, setLocated] = useState(true);
  const seq = useRef(0);
  const load = useCallback(
    async (nextPage = 0) => {
      const request = ++seq.current;
      setLoading(true);
      setError("");
      try {
        let rows,
          more = false;
        if (kind === "library") {
          rows = await discoveryApi.saved(nextPage);
          more = rows.length === 50;
        } else if (kind === "history") {
          const data = await discoveryApi.history(month, nextPage);
          rows = data.entries;
          more = data.hasMore;
          if (request === seq.current) setArtists(data.artists);
        } else {
          const [data, location] = await Promise.all([
            discoveryApi.chart(radius),
            locationApi.getMine(),
          ]);
          rows = data;
          if (request === seq.current) setLocated(!!location);
        }
        if (request !== seq.current) return;
        setEntries((prev) => (nextPage ? [...prev, ...rows] : rows));
        setPage(nextPage);
        setHasMore(more);
      } catch (e) {
        if (request === seq.current) setError(e.message);
      } finally {
        if (request === seq.current) setLoading(false);
      }
    },
    [kind, month, radius],
  );
  useEffect(() => {
    setEntries([]);
    setArtists([]);
    load();
    return () => {
      seq.current++;
      stop();
    };
  }, [load]);
  const remove = async (id) => {
    try {
      await discoveryApi.deleteHistory(id);
      await load();
    } catch (e) {
      setError(e.message);
    }
  };
  const rows =
    kind === "library" && ready
      ? entries.filter((e) => ids.has(e.track.id))
      : entries;
  return (
    <div className="discovery-page">
      <header className="map-header">
        <Link className="link-button" to="/">
          ← 지도
        </Link>
        <h1>{titles[kind]}</h1>
      </header>
      <FeatureNav />
      <main className="discovery-body">
        <p className="hint">
          {kind === "library"
            ? "지도와 대화에서 발견한 곡을 모아두세요."
            : kind === "history"
              ? "직접 올린 곡의 개인 기록입니다. 실제 스트리밍 청취 횟수와는 다릅니다."
              : "현재 공개 위치 기준 반경 내 사용자가 오늘 공유한 곡입니다. 같은 사람의 같은 곡은 하루 한 번만 집계합니다. (한국 시간)"}
        </p>
        <div className="discovery-toolbar">
          {kind === "history" && (
            <label>
              기록 월{" "}
              <input
                type="month"
                value={month}
                max={currentMonth()}
                onChange={(e) => {
                  if (e.target.value) setMonth(e.target.value);
                }}
              />
            </label>
          )}
          {kind === "chart" && (
            <label>
              반경{" "}
              <select
                value={radius}
                onChange={(e) => setRadius(Number(e.target.value))}
              >
                <option value={500}>500m</option>
                <option value={1000}>1km</option>
                <option value={2000}>2km</option>
                <option value={5000}>5km</option>
              </select>
            </label>
          )}
          <button
            type="button"
            className="secondary"
            disabled={loading}
            onClick={() => load()}
          >
            새로고침
          </button>
        </div>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {kind === "history" && artists.length > 0 && (
          <section className="artist-summary">
            <h2>이달에 자주 공유한 가수</h2>
            <div className="chip-grid">
              {artists.map((a) => (
                <span className="chip chip--on" key={a.artistName}>
                  {a.artistName} · {a.count}회
                </span>
              ))}
            </div>
          </section>
        )}
        {kind === "chart" && !located && (
          <p className="hint">
            지역 차트를 보려면 <Link to="/">지도에서 내 위치를 공유</Link>해
            주세요.
          </p>
        )}
        {!loading && !error && rows.length === 0 && (
          <div className="discovery-empty">
            {kind === "library"
              ? "저장한 곡이 없습니다. 지도에서 마음에 드는 곡의 ♡ 곡 저장을 눌러보세요."
              : kind === "history"
                ? "이달에 공유한 곡이 없습니다. 새로 올리는 곡부터 기록됩니다."
                : "아직 이 반경에서 집계할 곡이 없습니다. 반경을 넓히거나 오늘의 곡을 공유해 보세요."}
          </div>
        )}
        <ol className="music-entries">
          {rows.map((entry, index) => (
            <li key={entry.id ?? entry.track.id}>
              {kind === "chart" && (
                <span className="chart-rank">{index + 1}</span>
              )}
              <TrackCard track={entry.track}>
                {kind === "chart" ? (
                  <small>{entry.people}명이 공유</small>
                ) : (
                  <small>
                    {new Date(entry.createdAt + "+09:00").toLocaleString(
                      "ko-KR",
                    )}
                  </small>
                )}
              </TrackCard>
              {kind === "history" && (
                <button
                  type="button"
                  className="link-button"
                  onClick={() => remove(entry.id)}
                >
                  기록 삭제
                </button>
              )}
            </li>
          ))}
        </ol>
        {loading && <p role="status">불러오는 중…</p>}
        {hasMore && (
          <button
            type="button"
            className="secondary"
            disabled={loading}
            onClick={() => load(page + 1)}
          >
            더 보기
          </button>
        )}
      </main>
    </div>
  );
}
