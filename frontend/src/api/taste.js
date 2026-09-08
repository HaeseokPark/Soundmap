import { client } from "./client";

export const tasteApi = {
  listGenres: () => client.get("/api/genres").then((r) => r.data),

  searchArtists: (q) => client.get("/api/artists", { params: { q } }).then((r) => r.data),

  getMyTaste: () => client.get("/api/me/taste").then((r) => r.data),

  replaceMyTaste: (artistIds, genreIds) =>
    client.put("/api/me/taste", { artistIds, genreIds }).then((r) => r.data),

  getProfile: (userId) => client.get(`/api/users/${userId}/profile`).then((r) => r.data),
};
