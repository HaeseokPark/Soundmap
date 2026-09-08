package com.soundmap.auth;

import com.soundmap.auth.dto.LoginRequest;
import com.soundmap.auth.dto.SignupRequest;
import com.soundmap.auth.dto.TokenResponse;
import com.soundmap.auth.dto.UserResponse;
import com.soundmap.global.error.ApiException;
import com.soundmap.global.error.ErrorCode;
import com.soundmap.global.jwt.JwtTokenProvider;
import com.soundmap.user.entity.User;
import com.soundmap.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build());

        return issueToken(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // 이메일이 없는 경우와 같은 메시지를 준다. 어떤 이메일이 가입되어 있는지 알려주지 않기 위해서다.
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueToken(user);
    }

    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(normalizeEmail(email));
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    private TokenResponse issueToken(User user) {
        String token = tokenProvider.createToken(user.getId(), user.getEmail());
        return TokenResponse.of(token, tokenProvider.getExpiresInSeconds(), UserResponse.from(user));
    }

    /** 대소문자만 다른 이메일로 중복 가입되는 것을 막는다. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
