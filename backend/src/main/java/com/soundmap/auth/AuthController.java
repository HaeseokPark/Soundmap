package com.soundmap.auth;

import com.soundmap.auth.dto.AvailabilityResponse;
import com.soundmap.auth.dto.LoginRequest;
import com.soundmap.auth.dto.SignupRequest;
import com.soundmap.auth.dto.TokenResponse;
import com.soundmap.auth.dto.UserResponse;
import com.soundmap.global.jwt.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 토큰이 아직 유효한지 확인하고 내 정보를 돌려준다. 새로고침할 때 프론트가 호출한다. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal me) {
        return authService.getMe(me.userId());
    }

    @GetMapping("/check-email")
    public AvailabilityResponse checkEmail(@RequestParam String email) {
        return new AvailabilityResponse(authService.isEmailAvailable(email));
    }

    @GetMapping("/check-nickname")
    public AvailabilityResponse checkNickname(@RequestParam String nickname) {
        return new AvailabilityResponse(authService.isNicknameAvailable(nickname));
    }
}
