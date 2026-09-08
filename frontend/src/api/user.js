import { client } from "./client";

export const userApi = {
  /**
   * 닉네임으로 사람 찾기. 정확히 일치할 때만 결과가 온다.
   * 없으면 빈 배열이다.
   */
  searchByNickname: (nickname) =>
    client.get("/api/users/search", { params: { nickname } }).then((r) => r.data),
};
