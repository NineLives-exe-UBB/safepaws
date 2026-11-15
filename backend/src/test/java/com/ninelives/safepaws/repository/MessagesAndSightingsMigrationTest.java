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
 * Checks the message and sighting tables, against a real PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MessagesAndSightingsMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long announcementId;
    private long ana;
    private long mihai;

    @BeforeEach
    void findDemoRows() {
        announcementId = jdbcTemplate.queryForObject(
                "SELECT id FROM announcement WHERE contact_phone = '0700000001'", Long.class);
        ana = userId("ana@safepaws.dev");
        mihai = userId("mihai@safepaws.dev");
    }

    private long userId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private void insertMessage(long senderId, long recipientId, String body) {
        jdbcTemplate.update("INSERT INTO message (announcement_id, sender_id, recipient_id, body) VALUES (?, ?, ?, ?)",
                announcementId, senderId, recipientId, body);
    }

    private void insertSighting(double latitude, double longitude) {
        jdbcTemplate.update("""
                INSERT INTO sighting (announcement_id, reporter_id, latitude, longitude, seen_at, note)
                VALUES (?, ?, ?, ?, LOCALTIMESTAMP - INTERVAL '1 hour', 'Near the bus stop')
                """, announcementId, mihai, latitude, longitude);
    }

    @Test
    void validMessageAndSightingAreSaved() {
        assertThatCode(() -> {
            insertMessage(mihai, ana, "I think I saw Mitzi near the park");
            insertSighting(46.7550, 23.5520);
        }).doesNotThrowAnyException();
    }

    @Test
    void messageCannotBeSentToYourself() {
        assertThatThrownBy(() -> insertMessage(ana, ana, "Hello"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void messageBodyCannotBeBlank() {
        assertThatThrownBy(() -> insertMessage(mihai, ana, "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void messageNeedsExistingUsers() {
        assertThatThrownBy(() -> insertMessage(-1, ana, "Hello"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"91, 23.55", "-91, 23.55", "46.75, 181", "46.75, -181"})
    void sightingCoordinatesMustBeValid(double latitude, double longitude) {
        assertThatThrownBy(() -> insertSighting(latitude, longitude))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAnAnnouncementDeletesItsMessagesAndSightings() {
        insertMessage(mihai, ana, "I think I saw Mitzi near the park");
        insertSighting(46.7550, 23.5520);

        jdbcTemplate.update("DELETE FROM announcement WHERE id = ?", announcementId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM message WHERE announcement_id = ?", Long.class, announcementId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sighting WHERE announcement_id = ?", Long.class, announcementId)).isZero();
    }

    @Test
    void everyNewLinkIsIndexed() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename IN ('message', 'sighting')", String.class);

        assertThat(indexes).contains(
                "idx_message_announcement_id",
                "idx_message_sender_id",
                "idx_message_recipient_id",
                "idx_sighting_announcement_id",
                "idx_sighting_reporter_id");
    }
}
