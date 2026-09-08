package com.soundmap.safety;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.safety.dto.SafetyDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @PostMapping("/blocks")
    public ResponseEntity<Void> block(@AuthenticationPrincipal AuthPrincipal me,
                                      @Valid @RequestBody BlockRequest request) {
        safetyService.block(me.userId(), request.targetUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/blocks/{targetUserId}")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal AuthPrincipal me,
                                        @PathVariable Long targetUserId) {
        safetyService.unblock(me.userId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    public List<BlockedUserResponse> myBlocks(@AuthenticationPrincipal AuthPrincipal me) {
        return safetyService.findMyBlocks(me.userId());
    }

    /** 신고 사유 선택지. 화면이 문구를 직접 들고 있지 않도록 서버에서 내려준다. */
    @GetMapping("/reports/reasons")
    public List<ReportReasonOption> reportReasons() {
        return Arrays.stream(ReportReason.values())
                .map(reason -> new ReportReasonOption(reason.name(), reason.getLabel()))
                .toList();
    }

    @PostMapping("/reports")
    public ResponseEntity<Void> report(@AuthenticationPrincipal AuthPrincipal me,
                                       @Valid @RequestBody ReportRequest request) {
        safetyService.report(me.userId(), request.targetUserId(), request.reason(), request.detail());

        // 신고한 상대를 계속 보고 싶지 않은 것이 보통이라 함께 차단할 수 있게 한다.
        if (request.alsoBlock()) {
            safetyService.block(me.userId(), request.targetUserId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
