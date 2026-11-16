package com.ninelives.safepaws.dto;

public record LoginRequest(
        String email,
        String password
) {
}