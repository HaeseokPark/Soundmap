package com.soundmap.music;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.music.dto.NowPlayingRequest;
import com.soundmap.music.dto.NowPlayingResponse;
import com.soundmap.music.dto.TrackSearchResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    /** 곡 검색. 외부 API 를 그대로 호출하므로 결과는 아직 저장되지 않는다. */
    @GetMapping("/tracks/search")
    public List<TrackSearchResult> search(@RequestParam("q") String query) {
        return musicService.search(query);
    }

    /** 지금 듣는 곡 등록 (수동 체크인). */
    @PutMapping("/me/now-playing")
    public NowPlayingResponse checkIn(@AuthenticationPrincipal AuthPrincipal me,
                                      @Valid @RequestBody NowPlayingRequest request) {
        return musicService.checkIn(me.userId(), request);
    }

    @GetMapping("/me/now-playing")
    public ResponseEntity<NowPlayingResponse> getMyNowPlaying(@AuthenticationPrincipal AuthPrincipal me) {
        return musicService.findMyNowPlaying(me.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/me/now-playing")
    public ResponseEntity<Void> clearNowPlaying(@AuthenticationPrincipal AuthPrincipal me) {
        musicService.clearNowPlaying(me.userId());
        return ResponseEntity.noContent().build();
    }
}
