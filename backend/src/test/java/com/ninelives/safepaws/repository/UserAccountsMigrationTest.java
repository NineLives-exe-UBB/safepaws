package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the users table, the demo accounts and the announcement owners, against a real PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserAccountsMigrationTest {

    private static final List<String> DEMO_EMAILS =
            List.of("ana@safepaws.dev", "mihai@safepaws.dev", "elena@safepaws.dev");

    // The demo announcements from V2 and V3 use the placeholder phones 0700000001 to 0700000022
    private static final String DEMO_ANNOUNCEMENT = "contact_phone BETWEEN '0700000001' AND '0700000022'";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private void insertUser(String email, String displayName) {
        jdbcTemplate.update("INSERT INTO users (email, password_hash, display_name) VALUES (?, ?, ?)",
                email, "$2a$10$" + "x".repeat(53), displayName);
    }

    @Test
    void demoAccountsExistWithBcryptHashes() {
        List<String> displayNames = jdbcTemplate.queryForList(
                "SELECT display_name FROM users WHERE email IN (?, ?, ?) ORDER BY email",
                String.class, DEMO_EMAILS.toArray());

        assertThat(displayNames).containsExactly("Ana Pop", "Elena Moldovan", "Mihai Rus");
        assertThat(count("SELECT count(*) FROM users WHERE password_hash !~ '^\\$2a\\$10\\$.{53}$'")).isZero();
        assertThat(count("SELECT count(DISTINCT password_hash) FROM users")).isEqualTo(count("SELECT count(*) FROM users"));
    }

    @Test
    void everyDemoAnnouncementHasAnOwner() {
        assertThat(count("SELECT count(*) FROM announcement WHERE " + DEMO_ANNOUNCEMENT)).isEqualTo(22);
        assertThat(count("SELECT count(*) FROM announcement WHERE owner_id IS NULL AND " + DEMO_ANNOUNCEMENT)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@safepaws.dev", "mihai@safepaws.dev", "elena@safepaws.dev"})
    void everyDemoAccountOwnsAVariedSetOfAnnouncements(String email) {
        String owned = "SELECT count(*) FROM announcement a JOIN users u ON u.id = a.owner_id WHERE u.email = ? AND ";

        assertThat(count(owned + "a.type = 'LOST'", email)).isPositive();
        assertThat(count(owned + "a.type = 'FOUND'", email)).isPositive();
        assertThat(count(owned + "a.status = 'ACTIVE'", email)).isPositive();
        assertThat(count(owned + "a.status = 'RESOLVED'", email)).isPositive();
        assertThat(count(owned + "(SELECT count(*) FROM photo p WHERE p.announcement_id = a.id) = 2", email)).isPositive();
    }

    @Test
    void everyAnnouncementNeedsAnOwner() {
        assertThat(count("SELECT count(*) FROM announcement WHERE owner_id IS NULL")).isZero();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE announcement SET owner_id = NULL WHERE contact_phone = '0700000001'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ownerColumnIsIndexed() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'announcement'", String.class);

        assertThat(indexes).contains("idx_announcement_owner_id");
    }

    @Test
    void ownerMustBeAnExistingUser() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE announcement SET owner_id = -1 WHERE contact_phone = '0700000001'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailMustBeUnique() {
        assertThatThrownBy(() -> insertUser("ana@safepaws.dev", "Another Ana"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana@safepaws.dev", " ana2@safepaws.dev", "ana3@safepaws.dev "})
    void emailMustBeTrimmedAndLowercase(String email) {
        assertThatThrownBy(() -> insertUser(email, "Ana"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void displayNameCannotBeBlank() {
        assertThatThrownBy(() -> insertUser("blank@safepaws.dev", "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
