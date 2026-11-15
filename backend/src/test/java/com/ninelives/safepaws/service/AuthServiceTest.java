package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.RegisterRequest;
import com.ninelives.safepaws.dto.UserResponse;
import com.ninelives.safepaws.exception.EmailAlreadyRegisteredException;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String HASH = "$2a$10$hashedhashedhashedhashedhashedhashedhashedhashedhashe";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private static User stored(Long id, User user) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void registerSavesTheHashNeverThePassword() {
        when(passwordEncoder.encode("SafePaws1!")).thenReturn(HASH);
        when(userRepository.saveAndFlush(any())).thenAnswer(call -> stored(1L, call.getArgument(0)));

        authService.register(new RegisterRequest(" Ana@SafePaws.dev ", "SafePaws1!", " Ana Pop "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo(HASH);
        assertThat(saved.getEmail()).isEqualTo("ana@safepaws.dev");
        assertThat(saved.getDisplayName()).isEqualTo("Ana Pop");
    }

    @Test
    void registerReturnsTheUserWithoutThePassword() {
        when(passwordEncoder.encode(any())).thenReturn(HASH);
        when(userRepository.saveAndFlush(any())).thenAnswer(call -> stored(4L, call.getArgument(0)));

        UserResponse response = authService.register(new RegisterRequest("ana@safepaws.dev", "SafePaws1!", "Ana"));

        assertThat(response).isEqualTo(new UserResponse(4L, "ana@safepaws.dev", "Ana"));
    }

    @Test
    void registerChecksTheNormalizedEmail() {
        when(userRepository.existsByEmail("ana@safepaws.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(" ANA@safepaws.dev", "SafePaws1!", "Ana")))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email is already registered");
        verify(userRepository, never()).saveAndFlush(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerTurnsAUniqueConstraintRaceIntoEmailAlreadyRegistered() {
        when(passwordEncoder.encode(any())).thenReturn(HASH);
        when(userRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_users_email"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ana@safepaws.dev", "SafePaws1!", "Ana")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
