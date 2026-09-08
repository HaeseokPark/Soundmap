package com.soundmap.user;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.user.dto.UserSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserSearchService userSearchService;

    /**
     * 닉네임으로 사람 찾기. 정확히 일치할 때만 결과가 나온다.
     *
     * <p>지도 반경 밖에 있는 사람과 대화를 열기 위한 경로다.
     */
    @GetMapping("/api/users/search")
    public List<UserSearchResponse> search(@AuthenticationPrincipal AuthPrincipal me,
                                           @RequestParam String nickname) {
        return userSearchService.searchByNickname(me.userId(), nickname);
    }
}
