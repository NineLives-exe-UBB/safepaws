package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.LoginRequest;
import com.ninelives.safepaws.dto.LoginResponse;
import com.ninelives.safepaws.dto.RegisterRequest;
import com.ninelives.safepaws.dto.UserResponse;
import com.ninelives.safepaws.exception.EmailAlreadyRegisteredException;
import com.ninelives.safepaws.exception.InvalidCredentialsException;
import com.ninelives.safepaws.exception.UnauthorizedException;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MAX_PASSWORD_UTF8_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    /**
     * Creates the account. The request already holds the normalized email.
     * Registering does not log the user in: the frontend calls /auth/login next.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.displayName());
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // two registrations with the same email at the same moment: the unique constraint wins
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_UTF8_BYTES) {
            throw new InvalidCredentialsException();
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        JwtService.GeneratedToken generatedToken = jwtService.generateToken(user.getId());
        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
        return new LoginResponse(generatedToken.token(), generatedToken.expiresAt(), userResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(UnauthorizedException::new);
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
