package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import db.migration.V4__Seed_demo_photos;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks the photos loaded by the demo photo migration, against a real PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DemoPhotosMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private long photosOf(String contactPhone) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM photo p
                JOIN announcement a ON a.id = p.announcement_id
                WHERE a.contact_phone = ?
                """, Long.class, contactPhone);
    }

    @Test
    void everyAnnouncementHasExactlyOneMainPhoto() {
        long withoutOneMainPhoto = count("""
                SELECT count(*) FROM announcement a
                WHERE (SELECT count(*) FROM photo p WHERE p.announcement_id = a.id AND p.is_main) <> 1
                """);

        assertThat(count("SELECT count(*) FROM announcement")).isPositive();
        assertThat(withoutOneMainPhoto).isZero();
    }

    @Test
    void someAnnouncementsHaveAGallery() {
        assertThat(photosOf("0700000009")).isEqualTo(2);
        assertThat(photosOf("0700000013")).isEqualTo(2);
        assertThat(photosOf("0700000021")).isEqualTo(2);
    }

    @Test
    void storedPhotosAreJpegsUnder500KilobytesWithTheRightSize() {
        long invalid = count("""
                SELECT count(*) FROM photo
                WHERE content_type <> 'image/jpeg'
                   OR substring(data FROM 1 FOR 2) <> '\\xFFD8'::bytea
                   OR file_size <> octet_length(data)
                   OR file_size >= 500 * 1024
                """);

        assertThat(count("SELECT count(*) FROM photo")).isPositive();
        assertThat(invalid).isZero();
    }

    @Test
    void anAnnouncementThatAlreadyHasAMainPhotoKeepsIt() throws Exception {
        Context context = mock(Context.class);
        when(context.getConnection()).thenReturn(DataSourceUtils.getConnection(dataSource));

        new V4__Seed_demo_photos().migrate(context);

        assertThat(photosOf("0700000021")).isEqualTo(4);
        assertThat(count("""
                SELECT count(*) FROM announcement a
                WHERE (SELECT count(*) FROM photo p WHERE p.announcement_id = a.id AND p.is_main) <> 1
                """)).isZero();
    }
}
