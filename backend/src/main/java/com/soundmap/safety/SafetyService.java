package com.soundmap.safety;

import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.safety.dto.SafetyDtos.BlockedUserResponse;
import com.soundmap.safety.entity.Block;
import com.soundmap.safety.entity.Report;
import com.soundmap.safety.repository.BlockRepository;
import com.soundmap.safety.repository.ReportRepository;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 차단과 신고.
 *
 * <p>지도, 검색, 프로필, 채팅이 모두 같은 차단 집합을 물어보도록 여기 한 곳에 모았다.
 * 걸러내는 곳이 여러 군데로 흩어지면 한 곳을 빠뜨렸을 때 그게 곧 구멍이 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyService {

    /** 같은 상대를 이 시간 안에 다시 신고하면 중복으로 본다. */
    private static final Duration REPORT_COOLDOWN = Duration.ofHours(24);

    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    /** 나에게 보이지 않아야 할 사용자 id 집합. 양방향 차단을 모두 포함한다. */
    public Set<Long> hiddenUserIdsFor(Long userId) {
        return Set.copyOf(blockRepository.findRelatedUserIds(userId));
    }

    public boolean isBlockedBetween(Long first, Long second) {
        return blockRepository.existsBetween(first, second);
    }

    @Transactional
    public void block(Long blockerId, Long targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new ApiException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (blockRepository.existsByIdBlockerIdAndIdBlockedId(blockerId, targetUserId)) {
            return; // 이미 차단한 상대. 다시 눌러도 조용히 넘어간다.
        }
        blockRepository.save(new Block(blockerId, targetUserId));
    }

    @Transactional
    public void unblock(Long blockerId, Long targetUserId) {
        blockRepository.deleteByIdBlockerIdAndIdBlockedId(blockerId, targetUserId);
    }

    public List<BlockedUserResponse> findMyBlocks(Long userId) {
        List<Long> blockedIds = blockRepository.findBlockedUserIds(userId);
        if (blockedIds.isEmpty()) {
            return List.of();
        }

        Map<Long, User> users = userRepository.findAllById(blockedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return blockedIds.stream()
                .map(users::get)
                .filter(java.util.Objects::nonNull)
                .map(user -> new BlockedUserResponse(user.getId(), user.getNickname()))
                .toList();
    }

    /**
     * 신고 접수.
     *
     * <p>신고와 차단은 별개로 둔다. 신고만 하고 계속 보고 싶은 경우도 있고,
     * 차단만 하고 신고까지는 원치 않는 경우도 있다. 화면에서 함께 제안할 뿐이다.
     */
    @Transactional
    public void report(Long reporterId, Long targetUserId, ReportReason reason, String detail) {
        if (reporterId.equals(targetUserId)) {
            throw new ApiException(ErrorCode.CANNOT_REPORT_SELF);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime since = LocalDateTime.now().minus(REPORT_COOLDOWN);
        if (reportRepository.existsByReporterIdAndTargetUserIdAndCreatedAtAfter(
                reporterId, targetUserId, since)) {
            throw new ApiException(ErrorCode.DUPLICATE_REPORT);
        }

        reportRepository.save(new Report(reporterId, targetUserId, reason, detail));
        log.info("신고 접수: reporter={}, target={}, reason={}", reporterId, targetUserId, reason);
    }
}
