import { useSyncExternalStore } from "react";

import { getPlayingUrl, subscribe, toggle } from "./previewPlayer";

/**
 * 30초 미리듣기 재생/정지 버튼.
 *
 * 미리듣기 주소가 없는 곡이 있어서(iTunes 가 항상 주지는 않는다) `url` 이
 * 비면 아무것도 그리지 않는다. 버튼 자리를 비워두는 대신 통째로 감추는 편이
 * 목록에서 덜 어수선하다.
 */
export default function PreviewButton({ url, className = "" }) {
  const playingUrl = useSyncExternalStore(subscribe, getPlayingUrl, () => null);

  if (!url) return null;

  const playing = playingUrl === url;

  return (
    <button
      type="button"
      className={`preview-button ${playing ? "preview-button--playing" : ""} ${className}`.trim()}
      aria-label={playing ? "미리듣기 정지" : "30초 미리듣기"}
      title={playing ? "정지" : "30초 미리듣기"}
      onClick={(event) => {
        // 목록의 행 전체가 버튼이라, 막지 않으면 곡이 선택돼 시트가 닫힌다.
        event.stopPropagation();
        toggle(url);
      }}
    >
      {playing ? "■" : "▶"}
    </button>
  );
}
