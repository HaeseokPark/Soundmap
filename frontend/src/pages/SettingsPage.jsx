import PushSettings from "../notifications/PushSettings";
import FeatureNav from "../components/FeatureNav";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { safetyApi, settingsApi } from "../api/safety";
import { useAuth } from "../auth/AuthContext";

const PROFILE_SCOPES = [
  { code: "PUBLIC", label: "누구나", hint: "주변 사람 누구나 내 취향과 재생곡을 볼 수 있습니다." },
  {
    code: "CHAT_ONLY",
    label: "대화 중인 사람만",
    hint: "그 외에는 닉네임과 취향 일치도까지만 보입니다.",
  },
];

export default function SettingsPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [settings, setSettings] = useState(null);
  const [blocks, setBlocks] = useState([]);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([settingsApi.get(), safetyApi.listBlocks()])
      .then(([loaded, blocked]) => {
        setSettings(loaded);
        setBlocks(blocked);
      })
      .catch((err) => setError(err.message));
  }, []);

  const save = async (next) => {
    setError(null);
    setStatus(null);
    const previous = settings;
    setSettings(next); // 토글은 즉시 반응해야 눌린 느낌이 난다
    try {
      const saved = await settingsApi.update(next);
      setSettings(saved);
      setStatus(
        !saved.shareLocation
          ? "위치 공유를 껐습니다. 저장돼 있던 좌표도 지웠습니다."
          : "저장했습니다."
      );
    } catch (err) {
      setSettings(previous); // 실패하면 되돌린다
      setError(err.message);
    }
  };

  const unblock = async (userId) => {
    await safetyApi.unblock(userId);
    setBlocks((prev) => prev.filter((b) => b.userId !== userId));
  };

  if (error && !settings) {
    return (
      <div className="screen-center">
        <div className="card">
          <p className="error">{error}</p>
          <button className="secondary" type="button" onClick={() => navigate("/")}>
            지도로
          </button>
        </div>
      </div>
    );
  }

  if (!settings) return <div className="screen-center">불러오는 중...</div>;

  return (
    <div className="chat-page">
      <header className="map-header">
        <button className="link-button" type="button" onClick={() => navigate("/")}>
          ← 지도
        </button>
        <span className="map-header__logo">설정</span>
      </header>

      <FeatureNav/>
      <div className="chat-list-body">
        <PushSettings/>
        {status && <p className="map-dock__status">{status}</p>}
        {error && <p className="error">{error}</p>}

        <section className="chat-section">
          <h2>공개 범위</h2>

          <label className="toggle-row">
            <span className="toggle-row__text">
              <strong>위치 공유</strong>
              <em>
                끄면 지도에서 사라지고, 서버에 저장된 좌표도 삭제됩니다.
              </em>
            </span>
            <input
              type="checkbox"
              checked={settings.shareLocation}
              onChange={(e) => save({ ...settings, shareLocation: e.target.checked })}
            />
          </label>

          <label className="toggle-row">
            <span className="toggle-row__text">
              <strong>현재 듣는 곡 공개</strong>
              <em>끄면 다른 사람 화면에서만 감춰집니다. 내 화면에는 그대로 보입니다.</em>
            </span>
            <input
              type="checkbox"
              checked={settings.shareNowPlaying}
              onChange={(e) => save({ ...settings, shareNowPlaying: e.target.checked })}
            />
          </label>
        </section>

        <section className="chat-section">
          <h2>프로필 공개</h2>
          <div className="chip-grid">
            {PROFILE_SCOPES.map((scope) => (
              <button
                key={scope.code}
                type="button"
                className={settings.profileScope === scope.code ? "chip chip--on" : "chip"}
                onClick={() => save({ ...settings, profileScope: scope.code })}
              >
                {scope.label}
              </button>
            ))}
          </div>
          <p className="hint">
            {PROFILE_SCOPES.find((s) => s.code === settings.profileScope)?.hint}
          </p>
        </section>

        <section className="chat-section">
          <h2>차단한 사용자 {blocks.length > 0 && `(${blocks.length})`}</h2>
          {blocks.length === 0 ? (
            <p className="hint">차단한 사용자가 없습니다.</p>
          ) : (
            <ul className="room-list">
              {blocks.map((blocked) => (
                <li key={blocked.userId}>
                  <div className="block-row">
                    <strong>{blocked.nickname}</strong>
                    <button
                      className="secondary"
                      type="button"
                      onClick={() => unblock(blocked.userId)}
                    >
                      차단 해제
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <p className="hint">
            차단하면 서로의 지도·검색·프로필·대화에서 완전히 보이지 않습니다.
          </p>
        </section>

        <section className="chat-section">
          <button className="secondary" type="button" onClick={() => logout().catch(e => setError(e.message))}>
            로그아웃
          </button>
        </section>
      </div>
    </div>
  );
}
