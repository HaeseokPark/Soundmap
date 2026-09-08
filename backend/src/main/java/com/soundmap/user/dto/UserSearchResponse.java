package com.soundmap.user.dto;

import com.soundmap.user.entity.User;

/**
 * 닉네임 검색 결과 한 건.
 *
 * <p>대화를 걸려면 상대의 id 가 필요할 뿐이라 닉네임과 id 만 내보낸다.
 * 취향이나 재생곡은 기존 프로필 API 가 공개 범위를 따져서 준다.
 */
public record UserSearchResponse(
        Long userId,
        String nickname
) {
    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(user.getId(), user.getNickname());
    }
}
