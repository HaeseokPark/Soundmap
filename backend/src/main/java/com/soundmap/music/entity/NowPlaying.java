package com.soundmap.music.entity;

import com.soundmap.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 사용자가 "지금 듣는 중"으로 올려둔 곡. 사용자당 한 곡만 유지한다. */
@Entity
@Table(name = "now_playing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NowPlaying {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public NowPlaying(User user, Track track) {
        this.user = user;
        this.track = track;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeTrack(Track track) {
        this.track = track;
        this.updatedAt = LocalDateTime.now();
    }
}
