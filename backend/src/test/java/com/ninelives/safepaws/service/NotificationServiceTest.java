package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.NotificationAreaRequest;
import com.ninelives.safepaws.dto.NotificationAreaResponse;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.Notification;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.NotificationRepository;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationService: area management and notification triggering rules.
 * Ana (1) owns announcement 10, located in Mănăștur.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long ANA = 1L;
    private static final long MIHAI = 2L;
    private static final long ANNOUNCEMENT = 10L;

    // Mănăștur, Cluj-Napoca – the announcement location
    private static final double ANN_LAT = 46.7712;
    private static final double ANN_LNG = 23.6236;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AnnouncementOwnershipService ownershipService;

    @InjectMocks
    private NotificationService notificationService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static User user(long id) {
        User u = new User("u" + id + "@safepaws.dev", "hash", "User " + id);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static User userWithArea(long id, double lat, double lng, int radiusKm) {
        User u = user(id);
        u.setNotifyLatitude(lat);
        u.setNotifyLongitude(lng);
        u.setNotifyRadiusKm(radiusKm);
        return u;
    }

    private static Announcement announcement(long id, Long ownerId, Double lat, Double lng) {
        Announcement a = new Announcement();
        ReflectionTestUtils.setField(a, "id", id);
        if (ownerId != null) {
            a.setOwner(user(ownerId));
        }
        a.setLatitude(lat);
        a.setLongitude(lng);
        return a;
    }

    // ── Notification area ────────────────────────────────────────────────────

    @Test
    void getAreaReturns404WhenNoneSet() {
        when(ownershipService.currentUser()).thenReturn(user(ANA));

        assertThatThrownBy(() -> notificationService.getArea())
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getAreaReturnsTheArea() {
        User ana = userWithArea(ANA, 46.77, 23.62, 5);
        when(ownershipService.currentUser()).thenReturn(ana);

        NotificationAreaResponse area = notificationService.getArea();

        assertThat(area.latitude()).isEqualTo(46.77);
        assertThat(area.longitude()).isEqualTo(23.62);
        assertThat(area.radiusKm()).isEqualTo(5);
    }

    @Test
    void saveAreaPersistsAndReturnsIt() {
        User ana = user(ANA);
        when(ownershipService.currentUser()).thenReturn(ana);
        when(userRepository.save(ana)).thenReturn(ana);

        NotificationAreaResponse saved = notificationService.saveArea(
                new NotificationAreaRequest(46.77, 23.62, 3));

        assertThat(saved.latitude()).isEqualTo(46.77);
        assertThat(saved.longitude()).isEqualTo(23.62);
        assertThat(saved.radiusKm()).isEqualTo(3);
        verify(userRepository).save(ana);
    }

    @Test
    void deleteAreaClearsAllThreeColumns() {
        User ana = userWithArea(ANA, 46.77, 23.62, 5);
        when(ownershipService.currentUser()).thenReturn(ana);

        notificationService.deleteArea();

        assertThat(ana.hasNotifyArea()).isFalse();
        verify(userRepository).save(ana);
    }

    // ── Notification triggering ───────────────────────────────────────────────

    @Test
    void announcementWithoutCoordinatesTriggersNoNotifications() {
        Announcement noCoords = announcement(ANNOUNCEMENT, ANA, null, null);

        notificationService.notifyUsersInArea(noCoords);

        verify(userRepository, never()).findAllWithNotifyArea();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void authorDoesNotGetANotificationForTheirOwnAnnouncement() {
        // Ana (the author) has an area that includes the announcement location
        User ana = userWithArea(ANA, ANN_LAT, ANN_LNG, 20);
        when(userRepository.findAllWithNotifyArea()).thenReturn(List.of(ana));

        Announcement ann = announcement(ANNOUNCEMENT, ANA, ANN_LAT, ANN_LNG);
        notificationService.notifyUsersInArea(ann);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void userInsideTheRadiusGetsANotification() {
        // Mihai is 0 km away – same coordinates
        User mihai = userWithArea(MIHAI, ANN_LAT, ANN_LNG, 5);
        when(userRepository.findAllWithNotifyArea()).thenReturn(List.of(mihai));
        when(notificationRepository.existsByUser_IdAndAnnouncement_Id(MIHAI, ANNOUNCEMENT))
                .thenReturn(false);

        Announcement ann = announcement(ANNOUNCEMENT, ANA, ANN_LAT, ANN_LNG);
        notificationService.notifyUsersInArea(ann);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void userOutsideTheRadiusGetsNoNotification() {
        // Mihai is in Timișoara, ~300 km from Cluj – radius 5 km
        User mihai = userWithArea(MIHAI, 45.7489, 21.2087, 5);
        when(userRepository.findAllWithNotifyArea()).thenReturn(List.of(mihai));

        Announcement ann = announcement(ANNOUNCEMENT, ANA, ANN_LAT, ANN_LNG);
        notificationService.notifyUsersInArea(ann);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void duplicateNotificationIsNotSaved() {
        User mihai = userWithArea(MIHAI, ANN_LAT, ANN_LNG, 5);
        when(userRepository.findAllWithNotifyArea()).thenReturn(List.of(mihai));
        when(notificationRepository.existsByUser_IdAndAnnouncement_Id(MIHAI, ANNOUNCEMENT))
                .thenReturn(true); // already exists

        Announcement ann = announcement(ANNOUNCEMENT, ANA, ANN_LAT, ANN_LNG);
        notificationService.notifyUsersInArea(ann);

        verify(notificationRepository, never()).save(any());
    }

    // ── Mark as read ─────────────────────────────────────────────────────────

    @Test
    void markAsReadReturns404ForUnknownOrOtherUsersNotification() {
        when(ownershipService.currentUserId()).thenReturn(ANA);
        when(notificationRepository.findByIdAndUserId(999L, ANA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void markAsReadSetsReadAt() {
        when(ownershipService.currentUserId()).thenReturn(ANA);
        Notification n = new Notification(user(ANA), announcement(ANNOUNCEMENT, MIHAI, ANN_LAT, ANN_LNG));
        when(notificationRepository.findByIdAndUserId(eq(1L), eq(ANA))).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L);

        assertThat(n.getReadAt()).isNotNull();
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsReadIsIdempotentWhenAlreadyRead() {
        when(ownershipService.currentUserId()).thenReturn(ANA);
        Notification n = new Notification(user(ANA), announcement(ANNOUNCEMENT, MIHAI, ANN_LAT, ANN_LNG));
        n.setReadAt(java.time.LocalDateTime.now().minusHours(1));
        when(notificationRepository.findByIdAndUserId(eq(1L), eq(ANA))).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(ownershipService.currentUserId()).thenReturn(ANA);
        when(notificationRepository.countByUser_IdAndReadAtIsNull(ANA)).thenReturn(5L);

        assertThat(notificationService.unreadCount()).isEqualTo(5L);
    }
}
