package com.soundmap.map;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.map.dto.MapPin;
import com.soundmap.map.dto.PinFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    /**
     * 지도와 목록이 같은 데이터를 쓴다. 검색 결과를 두 방식으로 보여주기 위해
     * 응답을 나누지 않고 한 번만 받아 화면에서 갈라 쓴다.
     */
    @GetMapping("/pins")
    public List<MapPin> pins(@AuthenticationPrincipal AuthPrincipal me,
                             @RequestParam(required = false) Integer radiusMeters,
                             @RequestParam(required = false) Long artistId,
                             @RequestParam(required = false) Long genreId) {
        return mapService.findVisiblePins(me.userId(), PinFilter.of(radiusMeters, artistId, genreId));
    }
}
