import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { chatApi } from "../api/chat";
import { userApi } from "../api/user";
import { useChatSocket } from "../chat/ChatSocketContext";

export default function ChatListPage() {
  const navigate = useNavigate();
  const { roomsVersion, refreshUnread } = useChatSocket();

  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 닉네임으로 사람 찾기. 지도는 반경 안(최대 10km)만 보여주므로
  // 멀리 있는 사람과는 이 경로로만 대화를 열 수 있다.
  const [nicknameQuery, setNicknameQuery] = useState("");
  const [found, setFound] = useState(null); // null=검색 전, []=없음
  const [searching, setSearching] = useState(false);

  const load = useCallback(() => {
    chatApi
      .listRooms()
      .then(setRooms)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  // roomsVersion 은 상대가 요청·수락·전송할 때마다 올라간다.
  useEffect(() => {
    load();
  }, [load, roomsVersion]);

  const accept = async (roomId) => {
    await chatApi.accept(roomId);
    load();
    refreshUnread();
    navigate(`/chats/${roomId}`);
  };

  const reject = async (roomId) => {
    await chatApi.reject(roomId);
    load();
  };

  const searchNickname = async (event) => {
    event.preventDefault();
    const trimmed = nicknameQuery.trim();
    if (!trimmed) return;

    setError(null);
    setSearching(true);
    try {
      setFound(await userApi.searchByNickname(trimmed));
    } catch (err) {
      setError(err.message);
      setFound(null);
    } finally {
      setSearching(false);
    }
  };

  /** 이미 방이 있으면 서버가 그 방을 돌려주므로 중복 요청이 생기지 않는다. */
  const startChatWith = async (userId) => {
    setError(null);
    try {
      const room = await chatApi.request(userId);
      navigate(`/chats/${room.roomId}`);
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) return <div className="screen-center">불러오는 중...</div>;

  const pending = rooms.filter((r) => r.status === "PENDING" && !r.requestedByMe);
  const rest = rooms.filter((r) => !(r.status === "PENDING" && !r.requestedByMe));

  return (
    <div className="chat-page">
      <header className="map-header">
        <button className="link-button" type="button" onClick={() => navigate("/")}>
          ← 지도
        </button>
        <span className="map-header__logo">대화</span>
      </header>

      <div className="chat-list-body">
        {error && <p className="error">{error}</p>}

        <section className="chat-section">
          <h2>닉네임으로 찾기</h2>
          <form className="nickname-search" onSubmit={searchNickname}>
            <input
              className="search-input"
              value={nicknameQuery}
              onChange={(e) => setNicknameQuery(e.target.value)}
              placeholder="닉네임을 정확히 입력하세요"
              aria-label="닉네임 검색"
            />
            <button className="primary" type="submit" disabled={searching}>
              {searching ? "찾는 중..." : "찾기"}
            </button>
          </form>

          {found?.length === 0 && (
            <p className="hint">
              그런 닉네임을 쓰는 사람이 없습니다. 띄어쓰기까지 정확해야 합니다.
            </p>
          )}

          {found?.map((person) => (
            <div key={person.userId} className="request-row">
              <div className="request-row__text">
                <strong>{person.nickname}</strong>
              </div>
              <div className="request-row__actions">
                <button
                  className="primary"
                  type="button"
                  onClick={() => startChatWith(person.userId)}
                >
                  대화 요청
                </button>
              </div>
            </div>
          ))}
        </section>

        {pending.length > 0 && (
          <section className="chat-section">
            <h2>받은 대화 요청</h2>
            {pending.map((room) => (
              <div key={room.roomId} className="request-row">
                <div className="request-row__text">
                  <strong>{room.partnerNickname}</strong>
                  <em>음악 취향 {room.matchScore}% 일치</em>
                </div>
                <div className="request-row__actions">
                  <button className="primary" type="button" onClick={() => accept(room.roomId)}>
                    수락
                  </button>
                  <button className="secondary" type="button" onClick={() => reject(room.roomId)}>
                    거절
                  </button>
                </div>
              </div>
            ))}
          </section>
        )}

        <section className="chat-section">
          <h2>대화</h2>
          {rest.length === 0 ? (
            <p className="hint">
              아직 대화가 없습니다. 지도에서 마음에 드는 사람에게 말을 걸어보세요.
            </p>
          ) : (
            <ul className="room-list">
              {rest.map((room) => (
                <li key={room.roomId}>
                  <button
                    className="room-row"
                    type="button"
                    onClick={() => navigate(`/chats/${room.roomId}`)}
                  >
                    <span className="room-row__text">
                      <span className="room-row__title">
                        <strong>{room.partnerNickname}</strong>
                        <span className="match-badge">{room.matchScore}%</span>
                        {room.status === "PENDING" && (
                          <span className="pending-badge">수락 대기</span>
                        )}
                      </span>
                      <em>
                        {room.lastMessageContent ??
                          (room.requestedByMe
                            ? "상대의 수락을 기다리는 중입니다."
                            : "대화를 시작해 보세요.")}
                      </em>
                    </span>

                    <span className="room-row__meta">
                      {room.lastMessageAt && (
                        <span className="room-row__time">{formatTime(room.lastMessageAt)}</span>
                      )}
                      {room.unreadCount > 0 && (
                        <span className="unread-badge">{room.unreadCount}</span>
                      )}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}

function formatTime(isoString) {
  const date = new Date(isoString);
  const diffMinutes = Math.floor((Date.now() - date.getTime()) / 60000);
  if (diffMinutes < 1) return "방금";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  if (diffMinutes < 60 * 24) return `${Math.floor(diffMinutes / 60)}시간 전`;
  return date.toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" });
}
