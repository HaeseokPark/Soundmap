package com.soundmap.settings;

import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.location.repository.UserLocationRepository;
import com.soundmap.settings.dto.SettingsDtos.SettingsRequest;
import com.soundmap.settings.dto.SettingsDtos.SettingsResponse;
import com.soundmap.settings.entity.UserSettings;
import com.soundmap.settings.repository.UserSettingsRepository;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final UserLocationRepository locationRepository;

    public SettingsResponse findMySettings(Long userId) {
        return settingsRepository.findById(userId)
                .map(SettingsResponse::from)
                .orElseGet(SettingsResponse::defaults);
    }

    /**
     * 설정 저장.
     *
     * <p>위치 공유를 끄면 저장된 좌표를 지운다. 화면에서만 감추면 서버에는 남아 있어서,
     * 유출이나 실수로 다시 노출될 여지가 생긴다.
     */
    @Transactional
    public SettingsResponse update(Long userId, SettingsRequest request) {
        UserSettings settings = settingsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
                    return settingsRepository.save(new UserSettings(user));
                });

        settings.update(request.shareLocation(), request.shareNowPlaying(), request.profileScope());

        if (!request.shareLocation()) {
            locationRepository.deleteById(userId);
        }

        return SettingsResponse.from(settings);
    }

    /** 위치 등록을 받아도 되는지. 꺼 둔 사용자의 좌표는 애초에 저장하지 않는다. */
    public boolean allowsLocationSharing(Long userId) {
        return settingsRepository.findById(userId)
                .map(UserSettings::isShareLocation)
                .orElse(true);
    }

    public ProfileScope profileScopeOf(Long userId) {
        return settingsRepository.findById(userId)
                .map(UserSettings::getProfileScope)
                .orElse(ProfileScope.PUBLIC);
    }

    /** 현재 곡을 감추기로 한 사용자들. 지도에 마커를 그릴 때 한 번에 확인한다. */
    public Set<Long> userIdsHidingNowPlaying(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(settingsRepository.findUserIdsHidingNowPlaying(userIds));
    }

    public boolean hidesNowPlaying(Long userId) {
        return settingsRepository.findById(userId)
                .map(settings -> !settings.isShareNowPlaying())
                .orElse(false);
    }
}
