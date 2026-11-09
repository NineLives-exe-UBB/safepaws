package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the demo data left by the migrations is complete, against a real PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DemoDataMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    @Test
    void everyAnnouncementIsFilledIn() {
        long incomplete = count("""
                SELECT count(*) FROM announcement
                WHERE breed IS NULL OR color IS NULL OR distinctive_marks IS NULL
                   OR latitude IS NULL OR longitude IS NULL
                   OR (type = 'LOST' AND pet_name IS NULL)
                """);

        assertThat(count("SELECT count(*) FROM announcement")).isPositive();
        assertThat(incomplete).isZero();
    }

    @Test
    void everyAreaFollowsTheNeighborhoodAndCityFormat() {
        assertThat(count("SELECT count(*) FROM announcement WHERE area NOT LIKE '%, %'")).isZero();
    }

    @Test
    void datesAreRecentAndInOrder() {
        long invalid = count("""
                SELECT count(*) FROM announcement
                WHERE event_date > LOCALTIMESTAMP
                   OR created_at > LOCALTIMESTAMP
                   OR created_at < event_date
                   OR event_date < LOCALTIMESTAMP - INTERVAL '6 weeks'
                """);

        assertThat(invalid).isZero();
    }
}
