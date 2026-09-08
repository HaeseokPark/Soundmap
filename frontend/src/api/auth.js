import { client } from "./client";

export const authApi = {
  signup: (payload) => client.post("/api/auth/signup", payload).then((r) => r.data),

  login: (payload) => client.post("/api/auth/login", payload).then((r) => r.data),

  me: () => client.get("/api/auth/me").then((r) => r.data),

  checkEmail: (email) =>
    client.get("/api/auth/check-email", { params: { email } }).then((r) => r.data.available),

  checkNickname: (nickname) =>
    client.get("/api/auth/check-nickname", { params: { nickname } }).then((r) => r.data.available),
};
