package com.soundmap.safety.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 차단 관계.
 *
 * <p>차단은 한쪽만 눌러도 <b>양방향</b>으로 동작한다. A가 B를 차단하면 B의 지도에서도
 * A가 사라진다. 차단당한 쪽에게 "차단당했다"는 사실이 드러나지 않아야 하고,
 * 계정을 새로 만들지 않는 한 다시 접근할 수 없어야 하기 때문이다.
 *
 * <p>조회는 두 방향 모두에서 일어나므로 blocked_id 에도 인덱스를 둔다.
 */
@Entity
@Table(
        name = "blocks",
        indexes = @Index(name = "idx_blocks_blocked", columnList = "blocked_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block {

    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Block(Long blockerId, Long blockedId) {
        this.id = new Key(blockerId, blockedId);
        this.createdAt = LocalDateTime.now();
    }

    public Long getBlockerId() {
        return id.getBlockerId();
    }

    public Long getBlockedId() {
        return id.getBlockedId();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "blocker_id", nullable = false)
        private Long blockerId;

        @Column(name = "blocked_id", nullable = false)
        private Long blockedId;

        public Key(Long blockerId, Long blockedId) {
            this.blockerId = blockerId;
            this.blockedId = blockedId;
        }
    }
}
