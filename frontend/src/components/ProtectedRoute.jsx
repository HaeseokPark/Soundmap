import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

/**
 * @param requireTaste 취향 등록이 끝난 사용자만 들여보낸다.
 *                     온보딩 화면 자체는 false 로 둬야 무한 리다이렉트가 나지 않는다.
 */
export default function ProtectedRoute({ children, requireTaste = true }) {
  const { isAuthenticated, hasTaste, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="screen-center">불러오는 중...</div>;
  }

  if (!isAuthenticated) {
    // 로그인 후 원래 가려던 곳으로 돌려보내기 위해 위치를 넘긴다.
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (requireTaste && hasTaste === false) {
    return <Navigate to="/onboarding" replace />;
  }

  return children;
}
