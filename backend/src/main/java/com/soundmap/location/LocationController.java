package com.soundmap.location;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.location.dto.LocationRequest;
import com.soundmap.location.dto.LocationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PutMapping
    public LocationResponse updateMyLocation(@AuthenticationPrincipal AuthPrincipal me,
                                             @Valid @RequestBody LocationRequest request) {
        return locationService.updateMyLocation(me.userId(), request);
    }

    @GetMapping
    public ResponseEntity<LocationResponse> getMyLocation(@AuthenticationPrincipal AuthPrincipal me) {
        return locationService.findMyLocation(me.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 위치 공유 끄기. 저장된 좌표를 지운다. */
    @DeleteMapping
    public ResponseEntity<Void> clearMyLocation(@AuthenticationPrincipal AuthPrincipal me) {
        locationService.clearMyLocation(me.userId());
        return ResponseEntity.noContent().build();
    }
}
