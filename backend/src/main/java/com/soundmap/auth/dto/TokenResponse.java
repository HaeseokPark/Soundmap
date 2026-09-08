package com.soundmap.auth.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static TokenResponse of(String accessToken, long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
