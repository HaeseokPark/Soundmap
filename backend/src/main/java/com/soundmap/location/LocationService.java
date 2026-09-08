package com.soundmap.location;

import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.location.dto.LocationRequest;
import com.soundmap.location.dto.LocationResponse;
import com.soundmap.location.entity.UserLocation;
import com.soundmap.location.repository.UserLocationRepository;
import com.soundmap.settings.SettingsService;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final UserLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final SettingsService settingsService;
    private final LocationProperties properties;

    /**
     * 좌표를 갱신한다. 원본 좌표는 이 메서드를 벗어나지 않는다 -
     * 격자로 스냅한 결과만 엔티티에 넘긴다.
     */
    @Transactional
    public LocationResponse updateMyLocation(Long userId, LocationRequest request) {
        // 위치 공유를 꺼 둔 사용자의 좌표는 애초에 받지 않는다.
        if (!settingsService.allowsLocationSharing(userId)) {
            throw new ApiException(ErrorCode.LOCATION_SHARING_DISABLED);
        }

        BigDecimal step = properties.gridSizeDegrees();
        BigDecimal gridLat = GeoGrid.snap(request.lat(), step);
        BigDecimal gridLon = GeoGrid.snap(request.lon(), step);

        UserLocation location = locationRepository.findById(userId)
                .map(existing -> {
                    existing.moveTo(gridLat, gridLon);
                    return existing;
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
                    return locationRepository.save(new UserLocation(user, gridLat, gridLon));
                });

        return LocationResponse.from(location);
    }

    public Optional<LocationResponse> findMyLocation(Long userId) {
        return locationRepository.findById(userId).map(LocationResponse::from);
    }

    @Transactional
    public void clearMyLocation(Long userId) {
        locationRepository.deleteById(userId);
    }
}
