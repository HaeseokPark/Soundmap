import { client } from "./client";

export const safetyApi = {
  block: (targetUserId) => client.post("/api/blocks", { targetUserId }),

  unblock: (targetUserId) => client.delete(`/api/blocks/${targetUserId}`),

  listBlocks: () => client.get("/api/blocks").then((r) => r.data),

  reportReasons: () => client.get("/api/reports/reasons").then((r) => r.data),

  report: ({ targetUserId, reason, detail, alsoBlock }) =>
    client.post("/api/reports", { targetUserId, reason, detail, alsoBlock }),
};

export const settingsApi = {
  get: () => client.get("/api/me/settings").then((r) => r.data),

  update: (settings) => client.put("/api/me/settings", settings).then((r) => r.data),
};
