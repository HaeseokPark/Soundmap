import { useEffect, useRef, useState } from "react";
import { musicApi } from "../api/music";
import PreviewButton from "../music/PreviewButton";
import { stop as stopPreview } from "../music/previewPlayer";

const DEBOUNCE_MS = 400;
const MIN_QUERY_LENGTH = 2;

export default function TrackSearchPanel({ onSelect, onClose, title="지금 듣는 곡", busy=false }) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState(null);
  const inputRef = useRef(null);
  // 늦게 도착한 옛 응답이 최신 결과를 덮어쓰지 않도록 요청 번호를 센다.
  const requestSeq = useRef(0);

  useEffect(() => {
    inputRef.current?.focus();
    // 시트를 닫으면 재생도 멈춘다. 안 그러면 화면이 사라진 뒤에도 소리가 남는다.
    return stopPreview;
  }, []);

  // 타이핑할 때마다 외부 API 를 때리면 금방 쿼터를 넘긴다. 입력이 멎으면 검색한다.
  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setResults([]);
      setSearching(false);
      return undefined;
    }

    const timer = setTimeout(() => {
      const seq = ++requestSeq.current;
      setSearching(true);
      setError(null);
      musicApi
        .searchTracks(trimmed)
        .then((tracks) => {
          if (seq !== requestSeq.current) return;
          setResults(tracks);
        })
        .catch((err) => {
          if (seq !== requestSeq.current) return;
          setError(err.message);
        })
        .finally(() => {
          if (seq !== requestSeq.current) return;
          setSearching(false);
        });
    }, DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [query]);

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />
        <h2 className="sheet__title">{title}</h2>

        <input
          ref={inputRef}
          className="search-input"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="곡 제목이나 가수를 입력하세요"
        />

        {searching && <p className="hint">검색 중...</p>}
        {error && <p className="error">{error}</p>}
        {!searching && query.trim().length >= MIN_QUERY_LENGTH && results.length === 0 && !error && (
          <p className="hint">검색 결과가 없습니다.</p>
        )}

        <ul className="track-list">
          {results.map((track) => (
            // 재생 버튼을 행 버튼 안에 넣으면 버튼이 중첩돼 잘못된 마크업이 된다.
            // 형제로 두고 가로로 붙인다.
            <li className="track-row-wrap" key={`${track.source}-${track.externalId}`}>
              <button className="track-row" disabled={busy} type="button" onClick={() => onSelect(track)}>
                {track.albumArtUrl ? (
                  <img className="track-row__art" src={track.albumArtUrl} alt="" />
                ) : (
                  <div className="track-row__art track-row__art--empty">♪</div>
                )}
                <span className="track-row__text">
                  <strong>{track.title}</strong>
                  <em>{track.artistName}</em>
                </span>
              </button>
              <PreviewButton url={track.previewUrl} />
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
