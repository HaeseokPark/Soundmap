import { client } from "./client";

export const musicApi = {
  searchTracks: (query) =>
    client.get("/api/tracks/search", { params: { q: query } }).then((r) => r.data),

  checkIn: (track) => client.put("/api/me/now-playing", track).then((r) => r.data),

  // 곡을 아직 안 올렸으면 서버가 204 를 준다. 그때는 null 로 다룬다.
  getMyNowPlaying: () =>
    client.get("/api/me/now-playing").then((r) => (r.status === 204 ? null : r.data)),

  clearNowPlaying: () => client.delete("/api/me/now-playing"),
};

export const locationApi = {
  update: (lat, lon) => client.put("/api/me/location", { lat, lon }).then((r) => r.data),

  getMine: () =>
    client.get("/api/me/location").then((r) => (r.status === 204 ? null : r.data)),

  clear: () => client.delete("/api/me/location"),
};

export const mapApi = {
  // radiusMeters / artistId / genreId 를 그대로 넘긴다. undefined 인 값은 axios 가 빼준다.
  getPins: (params) => client.get("/api/map/pins", { params }).then((r) => r.data),
};
