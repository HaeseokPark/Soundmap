package com.soundmap.settings;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.settings.dto.SettingsDtos.SettingsRequest;
import com.soundmap.settings.dto.SettingsDtos.SettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public SettingsResponse mySettings(@AuthenticationPrincipal AuthPrincipal me) {
        return settingsService.findMySettings(me.userId());
    }

    @PutMapping
    public SettingsResponse update(@AuthenticationPrincipal AuthPrincipal me,
                                   @Valid @RequestBody SettingsRequest request) {
        return settingsService.update(me.userId(), request);
    }
}
