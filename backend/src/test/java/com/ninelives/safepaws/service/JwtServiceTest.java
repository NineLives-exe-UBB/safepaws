package com.ninelives.safepaws.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-key-for-safepaws-automated-tests-32b";

    @Test
    void generateAndExtractUserIdRoundTripSucceeds() {
        JwtService jwtService = new JwtService(SECRET);

        JwtService.GeneratedToken generated = jwtService.generateToken(42L);

        assertThat(generated.token()).isNotBlank();
        assertThat(generated.expiresAt()).isNotNull();
        assertThat(jwtService.extractUserId(generated.token())).contains(42L);
    }

    @Test
    void extractUserIdReturnsEmptyForExpiredTamperedOrBlankToken() {
        JwtService jwtService = new JwtService(SECRET);

        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.extractUserId(expiredToken)).isEmpty();
        assertThat(jwtService.extractUserId("not-a-jwt")).isEmpty();
        assertThat(jwtService.extractUserId("")).isEmpty();
        assertThat(jwtService.extractUserId(null)).isEmpty();
    }

    @Test
    void constructorRejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtService("   "))
                .isInstanceOf(IllegalStateException.class);
    }
}