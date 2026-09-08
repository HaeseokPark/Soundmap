import { client } from "./client";
export const discoveryApi = {
  saved: (page = 0) =>
    client
      .get("/api/me/saved-tracks", { params: { page } })
      .then((r) => r.data),
  savedIds: () => client.get("/api/me/saved-tracks/ids").then((r) => r.data),
  save: (id) => client.put(`/api/me/saved-tracks/${id}`),
  unsave: (id) => client.delete(`/api/me/saved-tracks/${id}`),
  history: (month, page = 0) =>
    client
      .get("/api/me/music-history", { params: { month, page } })
      .then((r) => r.data),
  deleteHistory: (id) => client.delete(`/api/me/music-history/${id}`),
  chart: (radiusMeters) =>
    client
      .get("/api/music/local-chart", { params: { radiusMeters } })
      .then((r) => r.data),
};
export const notificationApi = {
  inbox: (page = 0) =>
    client
      .get("/api/me/notifications", { params: { page } })
      .then((r) => r.data),
  read: (id) => client.post(`/api/me/notifications/${id}/read`),
  readAll: () => client.post("/api/me/notifications/read-all"),
  config: () =>
    client.get("/api/me/notifications/push-config").then((r) => r.data),
  subscribe: (subscription) =>
    client.put("/api/me/notifications/push", subscription),
  unsubscribe: (endpoint) =>
    client.delete("/api/me/notifications/push", { data: { endpoint } }),
  unsubscribeAll: () => client.delete("/api/me/notifications/push/all"),
};
