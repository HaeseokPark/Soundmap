package com.soundmap.settings.entity;

import com.soundmap.settings.ProfileScope;
import com.soundmap.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공개 범위 설정.
 *
 * <p>행이 없으면 기본값으로 본다. 기존 사용자에게 굳이 행을 만들어 주지 않아도
 * 되고, 설정을 건드린 사람만 저장되기 때문이다.
 */
@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 위치 공유. 끄면 저장된 좌표를 지우고 새 좌표도 받지 않는다.
     * 서버가 보관하지 않는 정보는 유출될 수도 없다.
     */
    @Column(name = "share_location", nullable = false)
    private boolean shareLocation;

    /**
     * 현재 듣는 곡 공개. 끄면 남의 화면에서만 감춘다.
     * 위치와 달리 "지금 뭘 듣는지"는 내 화면에는 계속 필요하기 때문이다.
     */
    @Column(name = "share_now_playing", nullable = false)
    private boolean shareNowPlaying;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_scope", nullable = false, length = 20)
    private ProfileScope profileScope;

    public UserSettings(User user) {
        this.user = user;
        this.shareLocation = true;
        this.shareNowPlaying = true;
        this.profileScope = ProfileScope.PUBLIC;
    }

    public void update(boolean shareLocation, boolean shareNowPlaying, ProfileScope profileScope) {
        this.shareLocation = shareLocation;
        this.shareNowPlaying = shareNowPlaying;
        this.profileScope = profileScope;
    }
}
