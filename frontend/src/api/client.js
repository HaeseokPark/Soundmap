import axios from "axios";

const TOKEN_KEY = "soundmap.accessToken";

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export const client = axios.create({
  // 배포 시에는 비워 둔다. nginx 가 같은 도메인에서 /api 를 백엔드로 넘겨주므로
  // 상대 경로로 호출되고 CORS 자체가 필요 없어진다.
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  timeout: 10000,
});

// 모든 요청에 Authorization 헤더를 붙인다.
client.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 서버가 내려주는 에러 형태를 화면에서 쓰기 좋은 Error 로 바꾼다.
 * 백엔드 ErrorResponse: { code, message, fieldErrors }
 */
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data;

    // 토큰이 만료됐거나 잘못됐으면 지운다. 화면 전환은 AuthContext 가 처리한다.
    if (data?.code === "EXPIRED_TOKEN" || data?.code === "INVALID_TOKEN") {
      tokenStorage.clear();
    }

    const normalized = new Error(
      data?.message ?? "서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요."
    );
    normalized.code = data?.code ?? "NETWORK_ERROR";
    normalized.fieldErrors = data?.fieldErrors ?? null;
    normalized.status = error.response?.status ?? 0;
    return Promise.reject(normalized);
  }
);
