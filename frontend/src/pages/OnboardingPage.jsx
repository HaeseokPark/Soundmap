import { useEffect, useMemo, useRef, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { tasteApi } from "../api/taste";
import { useAuth } from "../auth/AuthContext";

const MIN_ARTISTS = 3;
const MIN_GENRES = 1;

export default function OnboardingPage() {
  const { user, hasTaste, refreshTaste } = useAuth();
  const navigate = useNavigate();

  const [genres, setGenres] = useState([]);
  const [artists, setArtists] = useState([]);
  const [query, setQuery] = useState("");
  const [selectedGenreIds, setSelectedGenreIds] = useState(new Set());
  const [selectedArtists, setSelectedArtists] = useState(new Map());
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  // 늦게 도착한 옛 응답이 최신 결과를 덮어쓰지 않도록 요청 번호를 센다.
  const requestSeq = useRef(0);

  useEffect(() => {
    tasteApi.listGenres().then(setGenres).catch((err) => setError(err.message));
  }, []);

  // 입력이 멎으면 검색한다. 첫 렌더에서는 빈 검색어로 기본 목록을 받는다.
  useEffect(() => {
    const timer = setTimeout(
      () => {
        const seq = ++requestSeq.current;
        tasteApi
          .searchArtists(query.trim())
          .then((list) => {
            if (seq === requestSeq.current) setArtists(list);
          })
          .catch((err) => {
            if (seq === requestSeq.current) setError(err.message);
          });
      },
      query ? 300 : 0
    );
    return () => clearTimeout(timer);
  }, [query]);

  const canSubmit = useMemo(
    () => selectedArtists.size >= MIN_ARTISTS && selectedGenreIds.size >= MIN_GENRES,
    [selectedArtists, selectedGenreIds]
  );

  if (hasTaste) {
    return <Navigate to="/" replace />;
  }

  const toggleGenre = (id) =>
    setSelectedGenreIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const toggleArtist = (artist) =>
    setSelectedArtists((prev) => {
      const next = new Map(prev);
      if (next.has(artist.id)) next.delete(artist.id);
      else next.set(artist.id, artist);
      return next;
    });

  const handleSubmit = async () => {
    setError(null);
    setSaving(true);
    try {
      await tasteApi.replaceMyTaste([...selectedArtists.keys()], [...selectedGenreIds]);
      await refreshTaste();
      navigate("/", { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="onboarding">
      <header className="onboarding__head">
        <h1>{user.nickname}님, 어떤 음악을 좋아하세요?</h1>
        <p>취향이 겹치는 사람을 찾는 데 쓰입니다. 나중에 언제든 바꿀 수 있습니다.</p>
      </header>

      <section className="onboarding__section">
        <h2>
          장르 <span className="counter">{selectedGenreIds.size}/{MIN_GENRES}개 이상</span>
        </h2>
        <div className="chip-grid">
          {genres.map((genre) => (
            <button
              key={genre.id}
              type="button"
              className={selectedGenreIds.has(genre.id) ? "chip chip--on" : "chip"}
              onClick={() => toggleGenre(genre.id)}
            >
              {genre.name}
            </button>
          ))}
        </div>
      </section>

      <section className="onboarding__section">
        <h2>
          좋아하는 가수{" "}
          <span className="counter">{selectedArtists.size}/{MIN_ARTISTS}명 이상</span>
        </h2>

        {selectedArtists.size > 0 && (
          <div className="chip-grid chip-grid--selected">
            {[...selectedArtists.values()].map((artist) => (
              <button
                key={artist.id}
                type="button"
                className="chip chip--on"
                onClick={() => toggleArtist(artist)}
              >
                {artist.name} ✕
              </button>
            ))}
          </div>
        )}

        <input
          className="search-input"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="가수 이름으로 검색"
        />

        <div className="artist-grid">
          {artists.map((artist) => (
            <button
              key={artist.id}
              type="button"
              className={
                selectedArtists.has(artist.id) ? "artist-card artist-card--on" : "artist-card"
              }
              onClick={() => toggleArtist(artist)}
            >
              {artist.imageUrl ? (
                <img src={artist.imageUrl} alt="" />
              ) : (
                <div className="artist-card__fallback">{artist.name.slice(0, 1)}</div>
              )}
              <span>{artist.name}</span>
            </button>
          ))}
        </div>
      </section>

      {error && <p className="error">{error}</p>}

      <div className="onboarding__footer">
        <button
          className="primary"
          type="button"
          disabled={!canSubmit || saving}
          onClick={handleSubmit}
        >
          {saving
            ? "저장 중..."
            : canSubmit
              ? "시작하기"
              : `가수 ${Math.max(MIN_ARTISTS - selectedArtists.size, 0)}명 더 선택`}
        </button>
      </div>
    </div>
  );
}
