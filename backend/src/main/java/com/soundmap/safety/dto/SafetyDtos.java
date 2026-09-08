package com.soundmap.safety.dto;

import com.soundmap.safety.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 차단 · 신고 관련 요청/응답 모음. */
public final class SafetyDtos {

    private SafetyDtos() {
    }

    public record BlockRequest(
            @NotNull(message = "차단할 상대를 지정해 주세요.")
            Long targetUserId
    ) {
    }

    public record BlockedUserResponse(Long userId, String nickname) {
    }

    public record ReportRequest(
            @NotNull(message = "신고할 상대를 지정해 주세요.")
            Long targetUserId,

            @NotNull(message = "신고 사유를 선택해 주세요.")
            ReportReason reason,

            @Size(max = 500, message = "설명은 500자를 넘을 수 없습니다.")
            String detail,

            /** 신고와 함께 차단할지. 화면에서 기본 체크로 제안한다. */
            boolean alsoBlock
    ) {
    }

    public record ReportReasonOption(String code, String label) {
    }
}
