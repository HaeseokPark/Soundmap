import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { disablePush, currentSubscription } from "../notifications/push";
import { authApi } from "../api/auth";
import { tokenStorage } from "../api/client";
import { tasteApi } from "../api/taste";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // 취향을 등록했는지. null 은 "아직 확인하지 않음".
  const [hasTaste, setHasTaste] = useState(null);
  // 새로고침 직후에는 토큰이 아직 유효한지 모른다. 확인이 끝날 때까지 화면을 잡아둔다.
  const [loading, setLoading] = useState(true);

  const loadTaste = useCallback(async () => {
    const taste = await tasteApi.getMyTaste();
    const registered = taste.artists.length > 0;
    setHasTaste(registered);
    return registered;
  }, []);

  useEffect(() => {
    const token = tokenStorage.get();
    if (!token) {
      setLoading(false);
      return;
    }

    authApi
      .me()
      .then(async (me) => {
        setUser(me);
        await loadTaste();
      })
      .catch(() => {
        tokenStorage.clear();
        setUser(null);
        setHasTaste(null);
      })
      .finally(() => setLoading(false));
  }, [loadTaste]);

  const applyToken = useCallback(
    async (tokenResponse) => {
      const oldSubscription=await currentSubscription();
      if(oldSubscription) await oldSubscription.unsubscribe();
      tokenStorage.set(tokenResponse.accessToken);
      setUser(tokenResponse.user);
      await loadTaste();
      return tokenResponse.user;
    },
    [loadTaste]
  );

  const login = useCallback(
    async (credentials) => applyToken(await authApi.login(credentials)),
    [applyToken]
  );

  const signup = useCallback(
    async (payload) => applyToken(await authApi.signup(payload)),
    [applyToken]
  );

  const logout = useCallback(async () => {
    await disablePush();
    tokenStorage.clear();
    setUser(null);
    setHasTaste(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      hasTaste,
      login,
      signup,
      logout,
      refreshTaste: loadTaste,
      isAuthenticated: user !== null,
    }),
    [user, loading, hasTaste, login, signup, logout, loadTaste]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error("useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.");
  }
  return context;
}
