package com.ninelives.safepaws.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    private final SecretKey signingKey;

    public record GeneratedToken(String token, LocalDateTime expiresAt) {
    }

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must not be blank");
        }
        this.signingKey = deriveHs256Key(secret);
    }

    private static SecretKey deriveHs256Key(String secret) {
        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length >= 32) {
            return new SecretKeySpec(rawBytes, "HmacSHA256");
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(rawBytes);
            return new SecretKeySpec(hashed, "HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public GeneratedToken generateToken(Long userId) {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAtInstant = issuedAt.plus(TOKEN_VALIDITY);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAtInstant))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiresAtInstant, ZoneOffset.UTC);
        return new GeneratedToken(token, expiresAt);
    }

    public Optional<Long> extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}