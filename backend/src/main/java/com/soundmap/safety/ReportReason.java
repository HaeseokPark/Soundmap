package com.soundmap.safety;

import lombok.Getter;

/** 신고 사유. 화면의 선택지와 1:1로 대응한다. */
@Getter
public enum ReportReason {

    HARASSMENT("괴롭힘 · 위협"),
    SEXUAL("성적인 메시지 · 사진"),
    SPAM("스팸 · 광고"),
    IMPERSONATION("사칭"),
    INAPPROPRIATE_PROFILE("부적절한 프로필"),
    OTHER("기타");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }
}
