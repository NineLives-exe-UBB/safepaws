package com.ninelives.safepaws.service;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.dto.NotificationAreaRequest;
import com.ninelives.safepaws.dto.NotificationAreaResponse;
import com.ninelives.safepaws.dto.NotificationResponse;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.NotificationRepository;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NotificationService against a real PostgreSQL (V9 schema). Each test rolls back.
 * The demo accounts start with no notification area.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, NotificationService.class, AnnouncementOwnershipService.class})
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long ana;
    private Long mihai;
    private Long mitzi; // announcement 1 owned by Ana

    @BeforeEach
    void findDemoData() {
        ana = idOf("ana@safepaws.dev");
        mihai = idOf("mihai@safepaws.dev");
        mitzi = jdbcTemplate.queryForObject(
                "SELECT id FROM announcement WHERE contact_phone = '0700000001'", Long.class);
    }

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    private Long idOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private void logInAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    // ── Area CRUD ─────────────────────────────────────────────────────────────

    @Test
    void noAreaByDefault() {
        logInAs(ana);
        assertThatThrownBy(() -> notificationService.getArea())
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void saveAreaThenGetArea() {
        logInAs(ana);

        notificationService.saveArea(new NotificationAreaRequest(46.77, 23.62, 5));
        NotificationAreaResponse area = notificationService.getArea();

        assertThat(area.latitude()).isEqualTo(46.77);
        assertThat(area.longitude()).isEqualTo(23.62);
        assertThat(area.radiusKm()).isEqualTo(5);
    }

    @Test
    void deleteAreaMakesItMissing() {
        logInAs(ana);
        notificationService.saveArea(new NotificationAreaRequest(46.77, 23.62, 5));

        notificationService.deleteArea();

        assertThatThrownBy(() -> notificationService.getArea())
                .isInstanceOf(ResponseStatusException.class);
    }

    // ── Notifications from new announcements ──────────────────────────────────

    @Test
    void userInAreaGetsNotifiedWhenAnnouncementIsPosted() {
        // Give Mihai an area that includes Mănăștur (announcement location)
        logInAs(mihai);
        notificationService.saveArea(new NotificationAreaRequest(46.7712, 23.6236, 5));

        // A new announcement posted by Ana (not Mihai)
        Announcement ann = newAnnouncement(ana, 46.7712, 23.6236);

        logInAs(mihai);
        notificationService.notifyUsersInArea(ann);

        assertThat(notificationService.listNotifications()).hasSize(1);
        NotificationResponse notification = notificationService.listNotifications().get(0);
        assertThat(notification.read()).isFalse();
        assertThat(notification.announcement().id()).isEqualTo(ann.getId());
    }

    @Test
    void authorDoesNotGetNotifiedForOwnAnnouncement() {
        logInAs(ana);
        notificationService.saveArea(new NotificationAreaRequest(46.7712, 23.6236, 5));

        Announcement ann = newAnnouncement(ana, 46.7712, 23.6236);
        notificationService.notifyUsersInArea(ann);

        assertThat(notificationService.listNotifications()).isEmpty();
    }

    @Test
    void announcementWithoutCoordinatesNotifiesNobody() {
        logInAs(mihai);
        notificationService.saveArea(new NotificationAreaRequest(46.7712, 23.6236, 5));

        Announcement ann = newAnnouncement(ana, null, null);
        notificationService.notifyUsersInArea(ann);

        assertThat(notificationService.listNotifications()).isEmpty();
    }

    // ── Mark as read ──────────────────────────────────────────────────────────

    @Test
    void markAsReadSetsReadAtAndUnreadCountDrops() {
        logInAs(mihai);
        notificationService.saveArea(new NotificationAreaRequest(46.7712, 23.6236, 5));
        Announcement ann = newAnnouncement(ana, 46.7712, 23.6236);
        notificationService.notifyUsersInArea(ann);
        assertThat(notificationService.unreadCount()).isEqualTo(1);

        Long notifId = notificationService.listNotifications().get(0).id();
        notificationService.markAsRead(notifId);

        assertThat(notificationService.unreadCount()).isZero();
        assertThat(notificationService.listNotifications().get(0).read()).isTrue();
    }

    @Test
    void markAllAsReadClearsEverything() {
        logInAs(mihai);
        notificationService.saveArea(new NotificationAreaRequest(46.7712, 23.6236, 5));
        notificationService.notifyUsersInArea(newAnnouncement(ana, 46.7712, 23.6236));
        notificationService.notifyUsersInArea(newAnnouncement(ana, 46.7720, 23.6230));

        assertThat(notificationService.unreadCount()).isEqualTo(2);

        notificationService.markAllAsRead();

        assertThat(notificationService.unreadCount()).isZero();
    }

    @Test
    void markAsReadReturns404ForAnotherUsersNotification() {
        // Create a notification for Mihai
        logInAs(mihai);
        notificationService.saveArea(new NotificationAreaRequest(46.7712, 23.6236, 5));
        Announcement ann = newAnnouncement(ana, 46.7712, 23.6236);
        notificationService.notifyUsersInArea(ann);
        Long notifId = notificationService.listNotifications().get(0).id();

        // Ana tries to mark Mihai's notification as read
        logInAs(ana);
        assertThatThrownBy(() -> notificationService.markAsRead(notifId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Announcement newAnnouncement(Long ownerId, Double lat, Double lng) {
        User owner = userRepository.findById(ownerId).orElseThrow();
        Announcement a = new Announcement();
        a.setType(AnnouncementType.LOST);
        a.setSpecies(Species.CAT);
        a.setEventDate(LocalDateTime.now().minusHours(1));
        a.setArea("Mănăștur, Cluj-Napoca");
        a.setContactName("Test User");
        a.setStatus(AnnouncementStatus.ACTIVE);
        a.setOwner(owner);
        a.setLatitude(lat);
        a.setLongitude(lng);
        return announcementRepository.saveAndFlush(a);
    }
}
