package com.soundmap.safety.repository;

import com.soundmap.safety.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /** 같은 상대를 짧은 시간에 반복 신고하는 것을 막는 데 쓴다. */
    boolean existsByReporterIdAndTargetUserIdAndCreatedAtAfter(
            Long reporterId, Long targetUserId, LocalDateTime after);
}
