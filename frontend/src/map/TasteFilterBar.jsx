import { useEffect, useRef, useState } from "react";
import { tasteApi } from "../api/taste";

/**
 * 가수 · 장르로 지도를 걸러내는 검색 바.
 *
 * 선택하면 지도와 목록이 동시에 좁혀진다. 같은 데이터를 두 방식으로 보여주는 것이
 * 이 서비스의 탐색 방식이라서, 필터는 한 곳에서만 관리한다.
 */
export default function TasteFilterBar({ filter, onChange }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [artists, setArtists] = useState([]);
  const [genres, setGenres] = useState([]);
  const requestSeq = useRef(0);

  useEffect(() => {
    tasteApi.listGenres().then(setGenres).catch(() => setGenres([]));
  }, []);

  useEffect(() => {
    if (!open) return undefined;
    const timer = setTimeout(
      () => {
        const seq = ++requestSeq.current;
        tasteApi
          .searchArtists(query.trim())
          .then((list) => {
            if (seq === requestSeq.current) setArtists(list);
          })
          .catch(() => {});
      },
      query ? 300 : 0
    );
    return () => clearTimeout(timer);
  }, [query, open]);

  const selectArtist = (artist) => {
    onChange({ ...filter, artistId: artist.id, artistName: artist.name });
    setOpen(false);
    setQuery("");
  };

  const selectGenre = (genre) => {
    onChange({ ...filter, genreId: genre.id, genreName: genre.name });
    setOpen(false);
  };

  const clearAll = () => onChange({ radiusMeters: filter.radiusMeters });

  const hasFilter = filter.artistId != null || filter.genreId != null;

  return (
    <>
      <div className="filter-bar">
        <button className="filter-bar__open" type="button" onClick={() => setOpen(true)}>
          {hasFilter ? "조건 바꾸기" : "가수 · 장르로 찾기"}
        </button>

        {hasFilter && (
          <div className="filter-bar__chips">
            {filter.artistName && (
              <button
                type="button"
                className="chip chip--on"
                onClick={() => onChange({ ...filter, artistId: null, artistName: null })}
              >
                {filter.artistName} ✕
              </button>
            )}
            {filter.genreName && (
              <button
                type="button"
                className="chip chip--on"
                onClick={() => onChange({ ...filter, genreId: null, genreName: null })}
              >
                {filter.genreName} ✕
              </button>
            )}
            <button type="button" className="link-button" onClick={clearAll}>
              전체 해제
            </button>
          </div>
        )}
      </div>

      {open && (
        <div className="sheet-backdrop" onClick={() => setOpen(false)}>
          <div className="sheet" onClick={(e) => e.stopPropagation()}>
            <div className="sheet__handle" />
            <h2 className="sheet__title">어떤 음악을 좋아하는 사람을 찾으세요?</h2>

            <div className="filter-section">
              <h3>장르</h3>
              <div className="chip-grid">
                {genres.map((genre) => (
                  <button
                    key={genre.id}
                    type="button"
                    className={filter.genreId === genre.id ? "chip chip--on" : "chip"}
                    onClick={() => selectGenre(genre)}
                  >
                    {genre.name}
                  </button>
                ))}
              </div>
            </div>

            <div className="filter-section">
              <h3>가수</h3>
              <input
                className="search-input"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="가수 이름으로 검색"
              />
              <div className="artist-grid artist-grid--compact">
                {artists.map((artist) => (
                  <button
                    key={artist.id}
                    type="button"
                    className={
                      filter.artistId === artist.id ? "artist-card artist-card--on" : "artist-card"
                    }
                    onClick={() => selectArtist(artist)}
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
            </div>
          </div>
        </div>
      )}
    </>
  );
}
