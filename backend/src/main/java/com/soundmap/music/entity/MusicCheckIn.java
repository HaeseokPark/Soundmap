package com.soundmap.music.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="music_check_ins", indexes={@Index(columnList="user_id,created_at"), @Index(columnList="created_at")})
public class MusicCheckIn {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="track_id") private Track track;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private boolean publiclyShared;
    public MusicCheckIn(Long userId, Track track, boolean publiclyShared) {
        this.userId=userId; this.track=track; this.publiclyShared=publiclyShared;
        createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
    }
}
