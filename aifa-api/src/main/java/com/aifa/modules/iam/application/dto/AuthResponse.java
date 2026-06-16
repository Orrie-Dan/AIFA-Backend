package com.aifa.modules.iam.application.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken, String refreshToken, String tokenType, long expiresInSeconds, UUID userId) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds, UUID userId) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, userId);
    }
}
