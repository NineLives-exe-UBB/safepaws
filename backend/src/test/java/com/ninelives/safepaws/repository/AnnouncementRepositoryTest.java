package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a throwaway PostgreSQL in Docker with the real Flyway schema.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AnnouncementRepositoryTest {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @BeforeEach
    void startFromAnEmptyTable() {
        announcementRepository.deleteAllInBatch();
    }

    private static Announcement newAnnouncement() {
        Announcement announcement = new Announcement();
        announcement.setType(AnnouncementType.FOUND);
        announcement.setSpecies(Species.DOG);
        announcement.setPetName("Oscar");
        announcement.setEventDate(LocalDateTime.of(2026, 11, 1, 9, 15));
        announcement.setLatitude(46.7712);
        announcement.setLongitude(23.6236);
        announcement.setArea("Mărăști, Cluj-Napoca");
        announcement.setContactName("Ion Radu");
        announcement.setContactPhone("0740123456");
        return announcement;
    }

    @Test
    void savedAnnouncementGetsAnIdActiveStatusAndCreationTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        Announcement saved = announcementRepository.saveAndFlush(newAnnouncement());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(AnnouncementStatus.ACTIVE);
        assertThat(saved.getCreatedAt()).isAfter(before);
    }

    @Test
    void savedAnnouncementCanBeReadBackWithEveryField() {
        Long id = announcementRepository.saveAndFlush(newAnnouncement()).getId();

        Announcement found = announcementRepository.findById(id).orElseThrow();

        assertThat(found.getType()).isEqualTo(AnnouncementType.FOUND);
        assertThat(found.getSpecies()).isEqualTo(Species.DOG);
        assertThat(found.getPetName()).isEqualTo("Oscar");
        assertThat(found.getEventDate()).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 15));
        assertThat(found.getLatitude()).isEqualTo(46.7712);
        assertThat(found.getLongitude()).isEqualTo(23.6236);
        assertThat(found.getArea()).isEqualTo("Mărăști, Cluj-Napoca");
        assertThat(found.getContactName()).isEqualTo("Ion Radu");
        assertThat(found.getContactPhone()).isEqualTo("0740123456");
    }

    @Test
    void findByIdOfAMissingAnnouncementIsEmpty() {
        assertThat(announcementRepository.findById(999_999L)).isEmpty();
    }
}