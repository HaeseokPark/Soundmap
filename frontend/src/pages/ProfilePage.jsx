import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { chatApi } from "../api/chat";
import { safetyApi } from "../api/safety";
import { tasteApi } from "../api/taste";
import SaveTrackButton from "../music/SaveTrackButton";
import PreviewButton from "../music/PreviewButton";
import { stop as stopPreview } from "../music/previewPlayer";
import { objectParticleForList } from "../utils/korean";
import ReportDialog from "../safety/ReportDialog";

export default function ProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [error, setError] = useState(null);
  const [requesting, setRequesting] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    setProfile(null);
    setError(null);
    tasteApi.getProfile(userId).then(setProfile).catch((err) => setError(err.message));
  }, [userId]);

  // 화면을 벗어나거나 다른 사람의 프로필로 넘어가면 재생을 멈춘다.
  useEffect(() => stopPreview, [userId]);

  if (error) {
    return (
      <div className="screen-center">
        <div className="card">
          <p className="error">{error}</p>
          <button className="secondary" type="button" onClick={() => navigate(-1)}>
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  const startChat = async () => {
    setError(null);
    setRequesting(true);
    try {
      const room = await chatApi.request(profile.userId);
      navigate("/chats/" + room.roomId);
    } catch (err) {
      setError(err.message);
    } finally {
      setRequesting(false);
    }
  };

  const blockUser = async () => {
    const message = `${profile.nickname}님을 차단할까요?\n\n차단하면 서로의 지도·검색·프로필·대화에서 완전히 보이지 않게 됩니다. 설정에서 언제든 해제할 수 있습니다.`;
    if (!window.confirm(message)) {
      return;
    }
    await safetyApi.block(profile.userId);
    navigate("/", { replace: true });
  };

  if (!profile) {
    return <div className="screen-center">불러오는 중...</div>;
  }

  const hasCommonTaste = profile.commonArtists.length > 0 || profile.commonGenres.length > 0;

  return (
    <div className="profile-page">
      <header className="map-header">
        <button className="link-button" type="button" onClick={() => navigate(-1)}>
          ← 뒤로
        </button>
        <span className="map-header__logo">{profile.nickname}</span>
        {!profile.mine && (
          <span className="profile-actions">
            <button className="link-button" type="button" onClick={() => setReportOpen(true)}>
              신고
            </button>
            <button className="link-button link-button--danger" type="button" onClick={blockUser}>
              차단
            </button>
          </span>
        )}
      </header>

      <div className="profile-body">
        {notice && <p className="map-dock__status">{notice}</p>}

        {profile.restricted && (
          <p className="restricted-note">
            이 사용자는 프로필을 대화 중인 사람에게만 공개하고 있습니다.
            대화를 요청해 수락되면 음악 취향을 볼 수 있습니다.
          </p>
        )}

        {profile.nowPlaying && (
          <section className="profile-section">
            <h2>지금 듣는 곡</h2>
            <div className="pin-track">
              {profile.nowPlaying.albumArtUrl && (
                <img className="pin-track__art" src={profile.nowPlaying.albumArtUrl} alt="" />
              )}
              <div className="pin-track__text">
                <strong>{profile.nowPlaying.title}</strong>
                <em>{profile.nowPlaying.artistName}</em>
                {profile.nowPlaying.albumName && (
                  <span className="pin-track__album">{profile.nowPlaying.albumName}</span>
                )}
              </div>
              <PreviewButton
                url={profile.nowPlaying.previewUrl}
                className="preview-button--large"
              />
              <SaveTrackButton track={profile.nowPlaying}/>
            </div>
          </section>
        )}

        {!profile.mine && !profile.restricted && (
          <section className="profile-section">
            <h2>공통 취향</h2>
            {hasCommonTaste ? (
              <>
                {profile.commonArtists.length > 0 && (
                  <p className="common-line">
                    두 분 모두 <strong>{profile.commonArtists.map((a) => a.name).join(", ")}</strong>
                    {objectParticleForList(profile.commonArtists.map((a) => a.name))} 좋아합니다.
                  </p>
                )}
                <div className="chip-grid">
                  {profile.commonArtists.map((artist) => (
                    <span key={artist.id} className="chip chip--common">
                      {artist.name}
                    </span>
                  ))}
                  {profile.commonGenres.map((genre) => (
                    <span key={`g-${genre.id}`} className="chip chip--common">
                      {genre.name}
                    </span>
                  ))}
                </div>
              </>
            ) : (
              <p className="hint">겹치는 취향이 아직 없습니다.</p>
            )}
          </section>
        )}

        {!profile.restricted && (
        <section className="profile-section">
          <h2>좋아하는 가수</h2>
          {profile.favoriteArtists.length > 0 ? (
            <div className="artist-grid">
              {profile.favoriteArtists.map((artist) => (
                <div key={artist.id} className="artist-card artist-card--static">
                  {artist.imageUrl ? (
                    <img src={artist.imageUrl} alt="" />
                  ) : (
                    <div className="artist-card__fallback">{artist.name.slice(0, 1)}</div>
                  )}
                  <span>{artist.name}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="hint">아직 등록하지 않았습니다.</p>
          )}
        </section>
        )}

        {!profile.restricted && (
        <section className="profile-section">
          <h2>선호 장르</h2>
          <div className="chip-grid">
            {profile.favoriteGenres.map((genre) => (
              <span key={genre.id} className="chip chip--on">
                {genre.name}
              </span>
            ))}
          </div>
        </section>
        )}

        {profile.mine ? (
          <button className="secondary" type="button" onClick={() => navigate("/onboarding")}>
            취향 다시 고르기
          </button>
        ) : (
          <button className="primary" type="button" onClick={startChat} disabled={requesting}>
            {requesting ? "요청 중..." : "대화 요청하기"}
          </button>
        )}
      </div>

      {reportOpen && (
        <ReportDialog
          targetUserId={profile.userId}
          targetNickname={profile.nickname}
          onClose={() => setReportOpen(false)}
          onDone={(blocked) => {
            setReportOpen(false);
            if (blocked) {
              navigate("/", { replace: true });
            } else {
              setNotice("신고가 접수되었습니다.");
            }
          }}
        />
      )}
    </div>
  );
}
