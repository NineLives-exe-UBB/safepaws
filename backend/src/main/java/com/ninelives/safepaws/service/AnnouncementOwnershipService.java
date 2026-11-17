package com.ninelives.safepaws.service;

import com.ninelives.safepaws.exception.UnauthorizedException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnnouncementOwnershipService {

    private final UserRepository userRepository;

    public AnnouncementOwnershipService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long currentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new UnauthorizedException();
        }

        return userId;
    }

    public User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(UnauthorizedException::new);
    }

    public void requireOwner(Announcement announcement) {
        Long userId = currentUserId();

        if (announcement.getOwner() == null
                || !userId.equals(announcement.getOwner().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the owner can modify this announcement"
            );
        }
    }
}
