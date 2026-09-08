import { client } from "./client";

export const chatApi = {
  /** 대화 요청. 이미 방이 있으면 그 방이 돌아온다. */
  request: (targetUserId) =>
    client.post("/api/chats/requests", { targetUserId }).then((r) => r.data),

  listRooms: () => client.get("/api/chats").then((r) => r.data),

  getRoom: (roomId) => client.get(`/api/chats/${roomId}`).then((r) => r.data),

  accept: (roomId) => client.post(`/api/chats/${roomId}/accept`).then((r) => r.data),

  reject: (roomId) => client.post(`/api/chats/${roomId}/reject`),

  getMessages: (roomId, before) =>
    client
      .get(`/api/chats/${roomId}/messages`, { params: { before } })
      .then((r) => r.data),

  /** WebSocket 이 끊겼을 때를 위한 전송 경로. */
  sendMessage: (roomId, content) =>
    client.post(`/api/chats/${roomId}/messages`, { content }).then((r) => r.data),

  sendTrack: (roomId, track) => client.post(`/api/chats/${roomId}/tracks`,track).then(r=>r.data),

  markRead: (roomId) => client.post(`/api/chats/${roomId}/read`),

  unreadCount: () => client.get("/api/chats/unread-count").then((r) => r.data.unreadCount),
};
