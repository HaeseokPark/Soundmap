import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { chatApi } from "../api/chat";
import SaveTrackButton from "../music/SaveTrackButton";
import PreviewButton from "../music/PreviewButton";
import { stop as stopPreview } from "../music/previewPlayer";
import { objectParticleForList } from "../utils/korean";

/** 지도 마커나 목록 항목을 눌렀을 때 올라오는 상세 시트. */
export default function PinSheet({ pin, onClose }) {
  const navigate = useNavigate();
  const [requesting, setRequesting] = useState(false);
  const [error, setError] = useState(null);

  const track = pin.track;
  const commonArtists = pin.commonArtists ?? [];

  // 시트를 닫거나 다른 사람을 누르면 앞의 곡을 멈춘다.
  useEffect(() => stopPreview, [pin.userId]);

  const startChat = async () => {
    setError(null);
    setRequesting(true);
    try {
      const room = await chatApi.request(pin.userId);
      navigate(`/chats/${room.roomId}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setRequesting(false);
    }
  };

  return (
    <div className="sheet-backdrop" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />

        <div className="pin-head">
          <div>
            <h2 className="sheet__title">{pin.nickname}</h2>
            <p className="pin-head__meta">
              {pin.mine ? "나" : formatDistance(pin.distanceMeters)}
              {" · "}
              {formatUpdatedAt(pin.updatedAt)}
            </p>
          </div>
          {!pin.mine && pin.matchScore != null && (
            <span className="match-badge match-badge--large">{pin.matchScore}%</span>
          )}
        </div>

        {track ? (
          <div className="pin-track">
            {track.albumArtUrl && (
              <img className="pin-track__art" src={track.albumArtUrl} alt="" />
            )}
            <div className="pin-track__text">
              <strong>{track.title}</strong>
              <em>{track.artistName}</em>
              {track.albumName && <span className="pin-track__album">{track.albumName}</span>}
            </div>
            <PreviewButton url={track.previewUrl} className="preview-button--large" />
            <SaveTrackButton track={track}/>
          </div>
        ) : (
          <p className="hint">아직 곡을 올리지 않았습니다.</p>
        )}

        {!pin.mine && commonArtists.length > 0 && (
          <p className="common-line">
            두 분 모두 <strong>{commonArtists.map((a) => a.name).join(", ")}</strong>
            {objectParticleForList(commonArtists.map((a) => a.name))} 좋아합니다.
            {commonArtists.length > 1 && ` (${commonArtists.length}명)`}
          </p>
        )}

        {error && <p className="error">{error}</p>}

        {pin.mine ? (
          <button className="primary" type="button" onClick={() => navigate(`/users/${pin.userId}`)}>
            내 프로필 보기
          </button>
        ) : (
          <div className="sheet__actions">
            <button
              className="secondary"
              type="button"
              onClick={() => navigate(`/users/${pin.userId}`)}
            >
              음악 취향 보기
            </button>
            <button className="primary" type="button" onClick={startChat} disabled={requesting}>
              {requesting ? "요청 중..." : "대화 요청"}
            </button>
          </div>
        )}

        <p className="pin-note">
          위치는 약 100m 격자로 표시됩니다. 정확한 좌표는 저장되지 않습니다.
        </p>
      </div>
    </div>
  );
}

function formatDistance(meters) {
  if (meters == null) return "";
  if (meters < 1000) return `약 ${Math.round(meters / 10) * 10}m`;
  return `약 ${(meters / 1000).toFixed(1)}km`;
}

function formatUpdatedAt(isoString) {
  const diffMinutes = Math.floor((Date.now() - new Date(isoString).getTime()) / 60000);
  if (diffMinutes < 1) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  const hours = Math.floor(diffMinutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  return `${Math.floor(hours / 24)}일 전`;
}
