package com.soundmap.taste;

import com.soundmap.global.jwt.AuthPrincipal;
import com.soundmap.taste.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TasteController {

    private final TasteService tasteService;

    /** 온보딩 화면의 가수 목록. q 를 비우면 기본 목록을 준다. */
    @GetMapping("/artists")
    public List<ArtistResponse> artists(@RequestParam(required = false) String q) {
        return tasteService.searchArtists(q);
    }

    @GetMapping("/genres")
    public List<GenreResponse> genres() {
        return tasteService.findAllGenres();
    }

    @GetMapping("/me/taste")
    public TasteResponse myTaste(@AuthenticationPrincipal AuthPrincipal me) {
        return tasteService.findTaste(me.userId());
    }

    @PutMapping("/me/taste")
    public TasteResponse replaceMyTaste(@AuthenticationPrincipal AuthPrincipal me,
                                        @Valid @RequestBody TasteRequest request) {
        return tasteService.replaceTaste(me.userId(), request);
    }

    @GetMapping("/users/{userId}/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal AuthPrincipal me,
                                       @PathVariable Long userId) {
        return tasteService.findProfile(me.userId(), userId);
    }
}
