package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the notification table, the notification area, the optional phone and the demo messages
 * and sightings, against a real PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class NotificationsMigrationTest {

    // Mitzi, Ana's lost kitten, has the demo messages and sightings
    private static final String MITZI = "SELECT id FROM announcement WHERE contact_phone = '0700000001'";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long mitzi;
    private long ana;
    private long mihai;
    private long elena;

    @BeforeEach
    void findDemoRows() {
        mitzi = jdbcTemplate.queryForObject(MITZI, Long.class);
        ana = userId("ana@safepaws.dev");
        mihai = userId("mihai@safepaws.dev");
        elena = userId("elena@safepaws.dev");
    }

    private long userId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private void insertNotification(long userId) {
        jdbcTemplate.update("INSERT INTO notification (user_id, announcement_id) VALUES (?, ?)", userId, mitzi);
    }

    private void setArea(Double latitude, Double longitude, Integer radiusKm) {
        jdbcTemplate.update("UPDATE users SET notify_latitude = ?, notify_longitude = ?, notify_radius_km = ? WHERE id = ?",
                latitude, longitude, radiusKm, mihai);
    }

    @Test
    void newNotificationIsUnread() {
        insertNotification(mihai);

        assertThat(count("SELECT count(*) FROM notification WHERE user_id = ? AND read_at IS NULL", mihai)).isEqualTo(1);
    }

    @Test
    void aUserGetsOneNotificationPerAnnouncement() {
        insertNotification(mihai);

        assertThatThrownBy(() -> insertNotification(mihai))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void notificationNeedsAnExistingUser() {
        assertThatThrownBy(() -> insertNotification(-1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAnAnnouncementDeletesItsNotifications() {
        insertNotification(mihai);

        jdbcTemplate.update("DELETE FROM announcement WHERE id = ?", mitzi);

        assertThat(count("SELECT count(*) FROM notification WHERE announcement_id = ?", mitzi)).isZero();
    }

    @Test
    void notificationsAreIndexed() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'notification'", String.class);

        assertThat(indexes).contains(
                "idx_notification_user_id", "idx_notification_unread", "idx_notification_announcement_id");
    }

    @Test
    void demoAccountsHaveNoNotificationArea() {
        assertThat(count("SELECT count(*) FROM users WHERE notify_latitude IS NOT NULL")).isZero();
    }

    @Test
    void notificationAreaCanBeSetAndRemoved() {
        assertThatCode(() -> {
            setArea(46.7571, 23.5547, 3);
            setArea(null, null, null);
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "46.75, , 3", ", 23.55, 3", "46.75, 23.55, ",
            "91, 23.55, 3", "-91, 23.55, 3", "46.75, 181, 3", "46.75, -181, 3",
            "46.75, 23.55, 0", "46.75, 23.55, 21"})
    void notificationAreaMustBeCompleteAndValid(Double latitude, Double longitude, Integer radiusKm) {
        assertThatThrownBy(() -> setArea(latitude, longitude, radiusKm))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void contactPhoneIsOptional() {
        assertThatCode(() -> jdbcTemplate.update("""
                INSERT INTO announcement (type, species, event_date, area, contact_name, owner_id)
                VALUES ('FOUND', 'DOG', LOCALTIMESTAMP, 'Centru, Cluj-Napoca', 'Ana Pop', ?)
                """, ana)).doesNotThrowAnyException();
    }

    @Test
    void mitziHasATrailOfSightingsFromOtherUsers() {
        String sightings = "SELECT count(*) FROM sighting s JOIN announcement a ON a.id = s.announcement_id WHERE a.id = ? AND ";

        assertThat(count(sightings + "true", mitzi)).isEqualTo(4);
        assertThat(count(sightings + "s.reporter_id = a.owner_id", mitzi)).isZero();
        assertThat(count(sightings + "(s.seen_at <= a.event_date OR s.seen_at > LOCALTIMESTAMP)", mitzi)).isZero();
        assertThat(count(sightings + "s.created_at < s.seen_at", mitzi)).isZero();
    }

    @Test
    void anaHasTwoConversationsAboutMitzi() {
        List<Long> others = jdbcTemplate.queryForList("""
                SELECT DISTINCT CASE WHEN sender_id = ? THEN recipient_id ELSE sender_id END
                FROM message WHERE announcement_id = ? ORDER BY 1
                """, Long.class, ana, mitzi);

        assertThat(others).containsExactlyInAnyOrder(mihai, elena);
        assertThat(count("SELECT count(*) FROM message WHERE announcement_id = ? AND ? NOT IN (sender_id, recipient_id)",
                mitzi, ana)).isZero();
    }

    @Test
    void demoMessagesHaveUnreadOnesAndValidTimes() {
        assertThat(count("SELECT count(*) FROM message WHERE recipient_id = ? AND read_at IS NULL", ana)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM message WHERE recipient_id = ? AND read_at IS NULL", elena)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM message WHERE read_at < created_at OR created_at > LOCALTIMESTAMP")).isZero();
    }
}
