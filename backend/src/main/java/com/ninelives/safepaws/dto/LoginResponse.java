package com.ninelives.safepaws.dto;

import java.time.LocalDateTime;

public record LoginResponse(
        String token,
        LocalDateTime expiresAt,
        UserResponse user
) {
}