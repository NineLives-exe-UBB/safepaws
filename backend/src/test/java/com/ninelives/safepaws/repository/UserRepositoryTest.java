package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a throwaway PostgreSQL with the real Flyway schema, including the V6 demo accounts.
 * Each test is rolled back afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private static final String HASH = "$2a$10$G1ImbrhJGVGqqR1klzQNFeytHmxZM.94vxa1rLR5LsEY0K2at.rvO";

    @Autowired
    private UserRepository userRepository;

    @Test
    void savedUserGetsAnIdAndCreationTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        User saved = userRepository.saveAndFlush(new User("new@safepaws.dev", HASH, "New User"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfter(before);
    }

    @Test
    void findByEmailFindsADemoAccount() {
        User ana = userRepository.findByEmail("ana@safepaws.dev").orElseThrow();

        assertThat(ana.getDisplayName()).isEqualTo("Ana Pop");
        assertThat(ana.getPasswordHash()).startsWith("$2a$10$").hasSize(60);
    }

    @Test
    void findByEmailOfAnUnknownEmailIsEmpty() {
        assertThat(userRepository.findByEmail("nobody@safepaws.dev")).isEmpty();
    }

    @Test
    void existsByEmailTellsWhetherTheEmailIsTaken() {
        assertThat(userRepository.existsByEmail("mihai@safepaws.dev")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@safepaws.dev")).isFalse();
    }

    @Test
    void theDatabaseRejectsADuplicateEmail() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("ana@safepaws.dev", HASH, "Copy")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAnEmailThatIsNotNormalized() {
        assertThatThrownBy(() -> userRepository.saveAndFlush(new User(" Upper@SafePaws.dev", HASH, "Upper")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
