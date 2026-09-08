import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import TrackSearchPanel from "../map/TrackSearchPanel";
import TrackCard from "../music/TrackCard";
import { stop as stopPreview } from "../music/previewPlayer";
import { chatApi } from "../api/chat";
import { useAuth } from "../auth/AuthContext";
import { useChatSocket } from "../chat/ChatSocketContext";
import { objectParticleForList } from "../utils/korean";

/**
 * 한 번에 받아오는 메시지 수. 백엔드 기본값과 같아야 한다.
 * 받은 개수가 이보다 적으면 더 위에 남은 메시지가 없다는 뜻이다.
 */
const PAGE_SIZE = 50;

export default function ChatRoomPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { subscribeToRoom, sendOverSocket, connected, refreshUnread, roomsVersion } = useChatSocket();

  const [room, setRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [trackSearch, setTrackSearch] = useState(false);
  useEffect(() => stopPreview, [roomId]);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [hasOlder, setHasOlder] = useState(false);
  const bottomRef = useRef(null);
  const listRef = useRef(null);
  // 위쪽에 메시지를 끼워 넣기 직전의 스크롤 기준점. 넣은 뒤 자리를 복원한다.
  const keepScrollRef = useRef(null);

  const load = useCallback(async () => {
    const [detail, history] = await Promise.all([
      chatApi.getRoom(roomId),
      chatApi.getMessages(roomId),
    ]);
    setRoom(detail);
    setMessages(history);
    // 꽉 채워 왔다면 위에 더 있을 수 있다.
    setHasOlder(history.length >= PAGE_SIZE);
    refreshUnread();
  }, [roomId, refreshUnread]);

  useEffect(() => {
    load().catch((err) => setError(err.message));
  }, [load]);

  // 상대가 요청을 수락하면 열려 있는 채팅에서도 곡 추천과 입력창을 활성화한다.
  useEffect(() => {
    let active = true;
    chatApi.getRoom(roomId).then(detail => { if (active) setRoom(detail); }).catch(() => {});
    return () => { active = false; };
  }, [roomId, roomsVersion]);

  // 실시간 수신. 연결이 늦게 붙을 수 있어 connected 가 바뀔 때 다시 구독한다.
  useEffect(() => {
    if (!connected) return undefined;

    return subscribeToRoom(roomId, (incoming) => {
      setMessages((prev) => {
        // 내가 보낸 메시지는 이미 화면에 있다. id 로 중복을 막는다.
        if (prev.some((m) => m.id === incoming.id)) return prev;
        return [...prev, { ...incoming, mine: incoming.senderId === user.id }];
      });
      chatApi.markRead(roomId).catch(() => {});
    });
  }, [connected, roomId, subscribeToRoom, user.id]);

  useEffect(() => {
    const list = listRef.current;

    // 이전 메시지를 위에 끼워 넣은 경우. 맨 아래로 내리면 읽던 자리를 잃는다.
    if (keepScrollRef.current != null && list) {
      list.scrollTop = list.scrollHeight - keepScrollRef.current;
      keepScrollRef.current = null;
      return;
    }

    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  /** 위로 거슬러 올라가며 이전 메시지를 더 받아온다. */
  const loadOlder = async () => {
    const oldest = messages[0];
    if (!oldest || loadingOlder) return;

    setLoadingOlder(true);
    const list = listRef.current;
    // 넣은 뒤 늘어난 높이만큼 밀어 주려면, 바닥에서부터의 거리를 기억해야 한다.
    const fromBottom = list ? list.scrollHeight - list.scrollTop : null;

    try {
      const older = await chatApi.getMessages(roomId, oldest.id);
      if (older.length < PAGE_SIZE) setHasOlder(false);
      if (older.length > 0) {
        keepScrollRef.current = fromBottom;
        setMessages((prev) => [...older, ...prev]);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingOlder(false);
    }
  };

  const send = async (e) => {
    e.preventDefault();
    const content = draft.trim();
    if (!content || sending) return;

    setError(null);
    setSending(true);
    setDraft("");

    try {
      // WebSocket 이 붙어 있으면 그쪽으로, 아니면 REST 로 보낸다.
      if (!sendOverSocket(roomId, content)) {
        const saved = await chatApi.sendMessage(roomId, content);
        setMessages((prev) =>
          prev.some((m) => m.id === saved.id) ? prev : [...prev, saved]
        );
      }
    } catch (err) {
      setError(err.message);
      setDraft(content);
    } finally {
      setSending(false);
    }
  };

  const sendTrack = async (track) => {
    if(sending)return;
    setSending(true);setError(null);
    try {
      const saved=await chatApi.sendTrack(roomId,track);
      setMessages(prev=>prev.some(m=>m.id===saved.id)?prev:[...prev,saved]);
      setTrackSearch(false);
    } catch(e){setError(e.message);}finally{setSending(false);}
  };

  const accept = async () => {
    const updated = await chatApi.accept(roomId);
    setRoom(updated);
  };

  if (error && !room) {
    return (
      <div className="screen-center">
        <div className="card">
          <p className="error">{error}</p>
          <button className="secondary" type="button" onClick={() => navigate("/chats")}>
            대화 목록으로
          </button>
        </div>
      </div>
    );
  }

  if (!room) return <div className="screen-center">불러오는 중...</div>;

  return (
    <div className="chat-page">
      <header className="map-header">
        <button className="link-button" type="button" onClick={() => navigate("/chats")}>
          ← 대화
        </button>
        <span className="map-header__logo">{room.partnerNickname}</span>
        <span className="map-header__count">{room.matchScore}% 일치</span>
        <button
          className="link-button"
          type="button"
          onClick={() => navigate(`/users/${room.partnerId}`)}
        >
          프로필
        </button>
      </header>

      {(room.commonArtists.length > 0 || room.partnerNowPlayingTitle) && (
        <div className="chat-banner">
          {room.commonArtists.length > 0 && (
            <p>
              두 분 모두 <strong>{room.commonArtists.map((a) => a.name).join(", ")}</strong>
              {objectParticleForList(room.commonArtists.map((a) => a.name))} 좋아합니다.
              {room.commonArtists.length > 1 && ` (공통 관심 아티스트 ${room.commonArtists.length}명)`}
            </p>
          )}
          {room.partnerNowPlayingTitle && (
            <p className="chat-banner__now">
              지금 듣는 곡: {room.partnerNowPlayingTitle} · {room.partnerNowPlayingArtist}
            </p>
          )}
        </div>
      )}

      <div className="message-list" ref={listRef}>
        {hasOlder && (
          <button
            className="load-older"
            type="button"
            onClick={loadOlder}
            disabled={loadingOlder}
          >
            {loadingOlder ? "불러오는 중..." : "이전 메시지 보기"}
          </button>
        )}

        {messages.length === 0 && (
          <p className="hint">
            {room.canSend
              ? "첫 메시지를 보내보세요."
              : "수락하면 대화를 시작할 수 있습니다."}
          </p>
        )}
        {messages.map((message) => (
          <div
            key={message.id}
            className={message.mine ? "bubble bubble--mine" : "bubble"}
          >
            {message.track?<TrackCard track={message.track}/>:<span className="bubble__text">{message.content}</span>}
            <span className="bubble__time">{formatTime(message.createdAt)}</span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {trackSearch&&<TrackSearchPanel title="대화에 보낼 곡" busy={sending} onSelect={sendTrack} onClose={()=>setTrackSearch(false)}/>}
      {room.canSend ? (
        <form className="composer" onSubmit={send}>
          {error && <p className="error">{error}</p>}
          <div className="composer__row">
            <button type="button" className="secondary" disabled={sending} onClick={()=>setTrackSearch(true)}>♪ 곡 추천</button>
            <input
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder={connected ? "메시지를 입력하세요" : "연결 중..."}
              maxLength={1000}
            />
            <button className="primary" type="submit" disabled={!draft.trim() || sending}>
              보내기
            </button>
          </div>
        </form>
      ) : (
        <div className="composer">
          {room.requestedByMe ? (
            <p className="hint">상대가 수락하면 대화를 시작할 수 있습니다.</p>
          ) : (
            <div className="composer__row">
              <button className="primary" type="button" onClick={accept}>
                대화 수락하기
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function formatTime(isoString) {
  return new Date(isoString).toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
}
