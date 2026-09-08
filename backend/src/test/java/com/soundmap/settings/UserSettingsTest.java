package com.soundmap.settings;

import com.soundmap.settings.entity.UserSettings;
import com.soundmap.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSettingsTest {

    private static User someUser() {
        return User.builder()
                .email("a@soundmap.dev")
                .passwordHash("hash")
                .nickname("테스터")
                .build();
    }

    @Test
    @DisplayName("기본값은 전부 공개다")
    void defaultsAreOpen() {
        UserSettings settings = new UserSettings(someUser());

        assertTrue(settings.isShareLocation());
        assertTrue(settings.isShareNowPlaying());
        assertEquals(ProfileScope.PUBLIC, settings.getProfileScope());
    }

    @Test
    @DisplayName("설정을 바꾸면 그대로 반영된다")
    void updateApplies() {
        UserSettings settings = new UserSettings(someUser());

        settings.update(false, false, ProfileScope.CHAT_ONLY);

        assertEquals(false, settings.isShareLocation());
        assertEquals(false, settings.isShareNowPlaying());
        assertEquals(ProfileScope.CHAT_ONLY, settings.getProfileScope());
    }

    @Test
    @DisplayName("항목은 서로 독립적이다")
    void togglesAreIndependent() {
        UserSettings settings = new UserSettings(someUser());

        // 곡만 감추고 위치는 계속 공유하는 조합이 가능해야 한다.
        settings.update(true, false, ProfileScope.PUBLIC);

        assertTrue(settings.isShareLocation());
        assertEquals(false, settings.isShareNowPlaying());
    }
}
