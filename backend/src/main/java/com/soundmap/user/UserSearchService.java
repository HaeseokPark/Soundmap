package com.soundmap.user;

import com.soundmap.safety.SafetyService;
import com.soundmap.user.dto.UserSearchResponse;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 닉네임으로 사람 찾기.
 *
 * <p>지도는 반경 안(최대 10km)에 있고 24시간 안에 위치를 공유한 사람만 보여준다.
 * 그래서 멀리 있는 사람과는 서로 영영 만날 수 없다. 이 검색은 그 경우에
 * 닉네임을 아는 사람끼리 대화를 열 수 있게 하는 통로다.
 *
 * <p><b>정확히 일치할 때만</b> 찾는다. 앞글자 일치를 허용하면 한두 글자로
 * 전체 사용자 목록을 훑을 수 있다. 이미 닉네임을 아는 사이에서만 쓰이도록
 * 좁혀 두는 편이 근접 기반이라는 앱의 전제도 덜 흔든다.
 */
@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserRepository userRepository;
    private final SafetyService safetyService;

    /**
     * @return 찾으면 한 건, 없으면 빈 목록. 자기 자신과 차단 관계인 상대는 없는 것으로 본다.
     */
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchByNickname(Long meId, String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        return userRepository.findByNickname(trimmed)
                .filter(found -> !found.getId().equals(meId))
                // 차단한 상대는 "없음"으로 답한다. 차단당한 사실을 알려 주지 않기 위해서다.
                .filter(found -> !safetyService.isBlockedBetween(meId, found.getId()))
                .map(UserSearchResponse::from)
                .map(List::of)
                .orElseGet(List::of);
    }
}
