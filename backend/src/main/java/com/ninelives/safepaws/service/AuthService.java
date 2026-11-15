package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.RegisterRequest;
import com.ninelives.safepaws.dto.UserResponse;
import com.ninelives.safepaws.exception.EmailAlreadyRegisteredException;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
