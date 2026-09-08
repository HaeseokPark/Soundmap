package com.soundmap.settings;

/** 프로필 공개 범위. */
public enum ProfileScope {

    /** 누구나 내 취향과 재생곡을 볼 수 있다. */
    PUBLIC,

    /**
     * 대화가 수락된 상대에게만 전부 보여준다.
     * 그 외에는 닉네임과 취향 일치도까지만 보인다.
     */
    CHAT_ONLY
}
