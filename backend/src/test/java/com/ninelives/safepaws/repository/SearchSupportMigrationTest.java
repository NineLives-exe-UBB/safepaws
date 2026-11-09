package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks what the search support migration adds, against a real PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SearchSupportMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @CsvSource({
            "Mănăștur, Manastur",
            "Florești, Floresti",
            "Târgu Mureș, Targu Mures",
            "Întregalde, Intregalde",
            // Older Romanian text uses cedilla letters instead of comma-below ones
            "Ţară şi, Tara si",
            "Manastur, Manastur"
    })
    void unaccentRemovesRomanianDiacritics(String text, String expected) {
        String result = jdbcTemplate.queryForObject("SELECT unaccent(?)", String.class, text);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void listFilterColumnsAreIndexed() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'announcement'", String.class);

        assertThat(indexes).contains(
                "idx_announcement_type",
                "idx_announcement_species",
                "idx_announcement_status");
    }
}
