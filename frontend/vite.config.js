import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const BACKEND = "http://localhost:8080";

/**
 * preview 프록시는 배포 구성을 로컬에서 그대로 재현하기 위한 것이다.
 *
 * 배포에서는 nginx 가 프론트와 API 를 같은 도메인으로 서비스한다. 그 상태를
 * 미리 확인하지 않으면 "로컬에서는 되는데 서버에서만 API 가 전부 실패하는"
 * 상황을 배포 후에야 발견하게 된다.
 *
 *   npm run build && npm run preview
 *
 * 로 열면 nginx 뒤에 있는 것과 같은 조건(상대 경로 + WebSocket 업그레이드)이 된다.
 */
export default defineConfig({
  plugins: [react()],
  preview: {
    proxy: {
      "/api": {
        target: BACKEND,
        changeOrigin: true,
      },
      "/ws": {
        target: BACKEND,
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
