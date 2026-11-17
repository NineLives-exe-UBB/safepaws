package com.ninelives.safepaws.service;

import com.ninelives.safepaws.exception.UnauthorizedException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementOwnershipServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnnouncementOwnershipService ownershipService;

    private static User user(Long id) {
        User user = new User("owner@example.com", "hashed-password", "Test Owner");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Announcement announcement(User owner) {
        Announcement announcement = new Announcement();
        announcement.setOwner(owner);
        return announcement;
    }

    private static void loginAs(Long userId) {
        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserIdReturnsLoggedInUser() {
        loginAs(5L);

        assertThat(ownershipService.currentUserId()).isEqualTo(5L);
    }

    @Test
    void currentUserIdRejectsUnauthenticatedUser() {
        assertThatThrownBy(() -> ownershipService.currentUserId())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void currentUserReturnsLoggedInUser() {
        User owner = user(5L);
        loginAs(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));

        assertThat(ownershipService.currentUser()).isSameAs(owner);
    }

    @Test
    void currentUserRejectsUnknownUser() {
        loginAs(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownershipService.currentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void ownerCanModifyOwnAnnouncement() {
        loginAs(5L);
        Announcement announcement = announcement(user(5L));

        assertThatCode(() -> ownershipService.requireOwner(announcement))
                .doesNotThrowAnyException();
    }

    @Test
    void differentUserCannotModifyAnnouncement() {
        loginAs(8L);
        Announcement announcement = announcement(user(5L));

        assertThatThrownBy(() -> ownershipService.requireOwner(announcement))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void announcementWithoutOwnerCannotBeModified() {
        loginAs(5L);
        Announcement announcement = announcement(null);

        assertThatThrownBy(() -> ownershipService.requireOwner(announcement))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void unauthenticatedUserCannotModifyAnnouncement() {
        Announcement announcement = announcement(user(5L));

        assertThatThrownBy(() -> ownershipService.requireOwner(announcement))
                .isInstanceOf(UnauthorizedException.class);
    }
}
