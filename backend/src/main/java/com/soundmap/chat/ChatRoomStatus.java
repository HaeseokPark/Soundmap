package com.soundmap.chat;

/**
 * 채팅방 상태.
 *
 * <p>수락 전에는 메시지를 주고받을 수 없다. 낯선 사람이 일방적으로 말을 걸 수 있으면
 * 위치 기반 서비스에서 곧바로 괴롭힘 통로가 되기 때문이다.
 */
public enum ChatRoomStatus {

    /** 요청만 보낸 상태. 받은 쪽에서 수락해야 대화가 열린다. */
    PENDING,

    ACCEPTED,

    /** 거절됨. 요청자가 다시 요청할 수 없다. */
    REJECTED
}
