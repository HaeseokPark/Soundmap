import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { notificationApi } from "../api/discovery";
import { useChatSocket } from "../chat/ChatSocketContext";
import FeatureNav from "../components/FeatureNav";
import PushSettings from "../notifications/PushSettings";
export default function NotificationsPage() {
  const navigate = useNavigate();
  const { notificationVersion, refreshNotifications } = useChatSocket();
  const [items, setItems] = useState([]),
    [page, setPage] = useState(0),
    [hasMore, setHasMore] = useState(false),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(false);
  const load = useCallback(async (next = 0) => {
    setLoading(true);
    setError("");
    try {
      const data = await notificationApi.inbox(next);
      setItems((prev) =>
        next
          ? [
              ...prev,
              ...data.items.filter((n) => !prev.some((p) => p.id === n.id)),
            ]
          : data.items,
      );
      setPage(next);
      setHasMore(data.hasMore);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => {
    load();
  }, [load, notificationVersion]);
  const open = async (item) => {
    try {
      await notificationApi.read(item.id);
      refreshNotifications();
      navigate(`/chats/${item.roomId}`);
    } catch (e) {
      setError(e.message);
    }
  };
  const readAll = async () => {
    try {
      await notificationApi.readAll();
      await load();
      refreshNotifications();
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <div className="discovery-page">
      <header className="map-header">
        <Link className="link-button" to="/">
          ← 지도
        </Link>
        <h1>알림함</h1>
      </header>
      <FeatureNav />
      <main className="discovery-body">
        <PushSettings />
        <div className="discovery-toolbar">
          <button
            type="button"
            className="secondary"
            onClick={readAll}
            disabled={loading}
          >
            모두 읽음
          </button>
          <button
            type="button"
            className="secondary"
            onClick={() => load()}
            disabled={loading}
          >
            새로고침
          </button>
        </div>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {!loading && !items.length && (
          <p className="discovery-empty">아직 알림이 없습니다.</p>
        )}
        <ul className="notification-list">
          {items.map((item) => (
            <li key={item.id}>
              <button
                type="button"
                className={
                  item.readAt
                    ? "notification-item"
                    : "notification-item notification-item--unread"
                }
                onClick={() => open(item)}
              >
                <strong>
                  {!item.readAt && "● "}
                  {item.title}
                </strong>
                <small>
                  {new Date(item.createdAt + "+09:00").toLocaleString("ko-KR")}
                </small>
              </button>
            </li>
          ))}
        </ul>
        {loading && <p role="status">불러오는 중…</p>}
        {hasMore && (
          <button
            type="button"
            className="secondary"
            disabled={loading}
            onClick={() => load(page + 1)}
          >
            이전 알림 더 보기
          </button>
        )}
      </main>
    </div>
  );
}
