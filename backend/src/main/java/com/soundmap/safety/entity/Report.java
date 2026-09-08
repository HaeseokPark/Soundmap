package com.soundmap.safety.entity;

import com.soundmap.safety.ReportReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 기록.
 *
 * <p>신고는 지우지 않고 쌓아 둔다. 같은 사용자에 대한 신고가 여러 건 모이는 것
 * 자체가 판단 근거가 되기 때문이다. 처리 여부는 운영자가 별도로 본다.
 */
@Entity
@Table(
        name = "reports",
        indexes = @Index(name = "idx_reports_target", columnList = "target_user_id, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    /** 신고자가 적은 부연 설명. 없을 수 있다. */
    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Report(Long reporterId, Long targetUserId, ReportReason reason, String detail) {
        this.reporterId = reporterId;
        this.targetUserId = targetUserId;
        this.reason = reason;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }
}
