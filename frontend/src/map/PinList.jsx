import { useNavigate } from "react-router-dom";

/**
 * 검색 결과를 목록으로 보여준다. 지도와 같은 데이터를 쓰며,
 * 지도로는 알기 어려운 취향 일치도와 공통 가수를 앞세운다.
 */
export default function PinList({ pins, onSelect }) {
  const navigate = useNavigate();
  const others = pins.filter((pin) => !pin.mine);

  if (others.length === 0) {
    return (
      <div className="pin-list pin-list--empty">
        <p className="hint">조건에 맞는 사람이 주변에 없습니다.</p>
        <p className="hint">반경을 넓히거나 조건을 지워보세요.</p>
      </div>
    );
  }

  return (
    <ul className="pin-list">
      {others.map((pin) => (
        <li key={pin.userId}>
          <div className="pin-row">
            <button
              className="pin-row__main"
              type="button"
              onClick={() => onSelect(pin)}
            >
              {pin.track?.albumArtUrl ? (
                <img className="pin-row__art" src={pin.track.albumArtUrl} alt="" />
              ) : (
                <div className="pin-row__art pin-row__art--empty">♪</div>
              )}

              <span className="pin-row__text">
                <span className="pin-row__title">
                  <strong>{pin.nickname}</strong>
                  <span className="match-badge">{pin.matchScore}%</span>
                </span>
                <em>
                  {pin.track ? `${pin.track.title} · ${pin.track.artistName}` : "곡을 올리지 않음"}
                </em>
                {pin.commonArtists.length > 0 && (
                  <span className="pin-row__common">
                    공통 가수 {pin.commonArtists.length}명 · {pin.commonArtists.map((a) => a.name).join(", ")}
                  </span>
                )}
              </span>

              <span className="pin-row__distance">{formatDistance(pin.distanceMeters)}</span>
            </button>

            <button
              className="pin-row__profile"
              type="button"
              onClick={() => navigate(`/users/${pin.userId}`)}
            >
              취향 보기
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}

function formatDistance(meters) {
  if (meters == null) return "";
  if (meters < 1000) return `${Math.round(meters / 10) * 10}m`;
  return `${(meters / 1000).toFixed(1)}km`;
}
