package com.ninelives.safepaws.service;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.config.PasswordConfig;
import com.ninelives.safepaws.dto.RegisterRequest;
import com.ninelives.safepaws.dto.UserResponse;
import com.ninelives.safepaws.exception.EmailAlreadyRegisteredException;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Register with the real BCrypt encoder and the real users table. Rolled back after each test.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, PasswordConfig.class, AuthService.class})
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registeredPasswordIsStoredAsABcryptHashThatMatches() {
        UserResponse response = authService.register(
                new RegisterRequest("  New.User@SafePaws.dev ", "SafePaws1!", "New User"));

        User stored = userRepository.findById(response.id()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("new.user@safepaws.dev");
        assertThat(stored.getPasswordHash()).startsWith("$2a$10$").hasSize(60).isNotEqualTo("SafePaws1!");
        assertThat(passwordEncoder.matches("SafePaws1!", stored.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("SafePaws2!", stored.getPasswordHash())).isFalse();
    }

    @Test
    void registeringADemoEmailInAnotherCaseIsRejected() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest("ANA@safepaws.dev", "SafePaws1!", "Ana")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void theEncoderAcceptsTheV6DemoPasswords() {
        // the demo accounts were hashed outside the app; login (Alexandru) relies on this matching
        for (String email : new String[]{"ana@safepaws.dev", "mihai@safepaws.dev", "elena@safepaws.dev"}) {
            User demo = userRepository.findByEmail(email).orElseThrow();
            assertThat(passwordEncoder.matches("SafePaws1!", demo.getPasswordHash())).as(email).isTrue();
        }
    }
}
