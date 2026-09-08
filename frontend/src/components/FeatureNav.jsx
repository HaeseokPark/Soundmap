import { NavLink } from "react-router-dom";
import { useChatSocket } from "../chat/ChatSocketContext";
export default function FeatureNav() {
  const { notificationCount } = useChatSocket();
  return (
    <nav className="feature-nav" aria-label="주요 메뉴">
      <NavLink to="/" end>
        지도
      </NavLink>
      <NavLink to="/music/library">보관함</NavLink>
      <NavLink to="/music/history">음악 기록</NavLink>
      <NavLink to="/music/chart">동네 차트</NavLink>
      <NavLink to="/notifications">
        알림
        {notificationCount > 0 && (
          <span className="unread-badge">
            {notificationCount > 99 ? "99+" : notificationCount}
          </span>
        )}
      </NavLink>
    </nav>
  );
}
