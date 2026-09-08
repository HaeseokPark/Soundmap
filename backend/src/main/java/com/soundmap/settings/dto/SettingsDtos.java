package com.soundmap.settings.dto;

import com.soundmap.settings.ProfileScope;
import com.soundmap.settings.entity.UserSettings;
import jakarta.validation.constraints.NotNull;

public final class SettingsDtos {

    private SettingsDtos() {
    }

    public record SettingsRequest(
            boolean shareLocation,
            boolean shareNowPlaying,

            @NotNull(message = "프로필 공개 범위를 선택해 주세요.")
            ProfileScope profileScope
    ) {
    }

    public record SettingsResponse(
            boolean shareLocation,
            boolean shareNowPlaying,
            ProfileScope profileScope
    ) {
        public static SettingsResponse from(UserSettings settings) {
            return new SettingsResponse(
                    settings.isShareLocation(),
                    settings.isShareNowPlaying(),
                    settings.getProfileScope());
        }

        /** 설정을 한 번도 건드리지 않은 사용자에게 보여줄 값. */
        public static SettingsResponse defaults() {
            return new SettingsResponse(true, true, ProfileScope.PUBLIC);
        }
    }
}
