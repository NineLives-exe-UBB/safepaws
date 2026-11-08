package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.dto.AnnouncementFilter;
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
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The list filters against a real PostgreSQL (with unaccent), each test rolled back afterwards.
 * Results are identified by their area.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AnnouncementSpecificationsTest {

    private static final Sort NEWEST = Sort.by(Sort.Order.desc("eventDate"), Sort.Order.desc("id"));

    @Autowired
    private AnnouncementRepository announcementRepository;

    @BeforeEach
    void startFromAnEmptyTable() {
        announcementRepository.deleteAllInBatch();
    }

    private Announcement save(String area, AnnouncementType type, Species species, AnnouncementStatus status,
                              String color, String breed, Double latitude, Double longitude, int day) {
        Announcement announcement = new Announcement();
        announcement.setType(type);
        announcement.setSpecies(species);
        announcement.setStatus(status);
        announcement.setColor(color);
        announcement.setBreed(breed);
        announcement.setLatitude(latitude);
        announcement.setLongitude(longitude);
        announcement.setArea(area);
        announcement.setEventDate(LocalDateTime.of(2026, 10, day, 12, 0));
        announcement.setContactName("Ana Pop");
        announcement.setContactPhone("0712345678");
        return announcementRepository.saveAndFlush(announcement);
    }

    private Announcement save(String area, int day) {
        return save(area, AnnouncementType.LOST, Species.CAT, AnnouncementStatus.ACTIVE,
                null, null, 46.77, 23.59, day);
    }

    private List<String> areas(AnnouncementFilter filter) {
        return announcementRepository.findAll(AnnouncementSpecifications.matching(filter), NEWEST).stream()
                .map(Announcement::getArea)
                .toList();
    }

    private static AnnouncementFilter filter(AnnouncementType type, Species species, AnnouncementStatus status) {
        return new AnnouncementFilter(type, species, status, null, null, null, null, null, null, null);
    }

    private static AnnouncementFilter textFilter(String color, String breed, String area) {
        return new AnnouncementFilter(null, null, null, color, breed, area, null, null, null, null);
    }

    @Test
    void withoutParametersOnlyActiveAnnouncementsComeBackNewestEventFirst() {
        save("older", 1);
        save("newer", 3);
        save("resolved", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.RESOLVED, null, null, null, null, 2);

        assertThat(areas(AnnouncementFilter.none())).containsExactly("newer", "older");
    }

    @Test
    void statusResolvedReturnsOnlyResolvedAnnouncements() {
        save("active", 1);
        save("resolved", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.RESOLVED, null, null, null, null, 2);

        assertThat(areas(filter(null, null, AnnouncementStatus.RESOLVED))).containsExactly("resolved");
    }

    @Test
    void typeAndSpeciesMatchExactlyAndCombine() {
        save("lost cat", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.ACTIVE, null, null, null, null, 1);
        save("lost dog", AnnouncementType.LOST, Species.DOG, AnnouncementStatus.ACTIVE, null, null, null, null, 2);
        save("found dog", AnnouncementType.FOUND, Species.DOG, AnnouncementStatus.ACTIVE, null, null, null, null, 3);

        assertThat(areas(filter(AnnouncementType.FOUND, null, null))).containsExactly("found dog");
        assertThat(areas(filter(null, Species.DOG, null))).containsExactly("found dog", "lost dog");
        assertThat(areas(filter(AnnouncementType.LOST, Species.DOG, null))).containsExactly("lost dog");
    }

    @Test
    void colorIsAContainsSearchThatIgnoresCase() {
        save("grey", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.ACTIVE, "grey", null, null, null, 1);
        save("dark grey", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.ACTIVE, "Dark Grey", null, null, null, 2);
        save("gray", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.ACTIVE, "Gray", null, null, null, 3);

        assertThat(areas(textFilter("GREY", null, null))).containsExactly("dark grey", "grey");
    }

    @Test
    void breedSearchSkipsAnnouncementsWithoutABreed() {
        save("terrier", AnnouncementType.FOUND, Species.DOG, AnnouncementStatus.ACTIVE,
                null, "Jack Russell Terrier", null, null, 1);
        save("no breed", 2);

        assertThat(areas(textFilter(null, "terrier", null))).containsExactly("terrier");
    }

    @Test
    void areaSearchIgnoresDiacriticsBothWays() {
        save("Mănăștur, Cluj-Napoca", 1);
        save("Iosefin, Timisoara", 2);
        save("Fabric, Timișoara", 3);

        assertThat(areas(textFilter(null, null, "manastur"))).containsExactly("Mănăștur, Cluj-Napoca");
        assertThat(areas(textFilter(null, null, "timisoara"))).containsExactly("Fabric, Timișoara", "Iosefin, Timisoara");
        assertThat(areas(textFilter(null, null, "Timișoara"))).containsExactly("Fabric, Timișoara", "Iosefin, Timisoara");
    }

    @Test
    void percentAndUnderscoreAreMatchedLiterally() {
        save("100% Centru", 1);
        save("Centru", 2);
        save("Zona_Nord", 3);
        save("ZonaXNord", 4);

        assertThat(areas(textFilter(null, null, "%"))).containsExactly("100% Centru");
        assertThat(areas(textFilter(null, null, "a_N"))).containsExactly("Zona_Nord");
    }

    @Test
    void locationSearchLeavesOutAnnouncementsWithoutAPosition() {
        save("with position", 1);
        save("no position", AnnouncementType.LOST, Species.CAT, AnnouncementStatus.ACTIVE, null, null, null, null, 2);

        AnnouncementFilter filter = new AnnouncementFilter(null, null, null, null, null, null, 46.77, 23.59, null, null);

        assertThat(areas(filter)).containsExactly("with position");
        assertThat(areas(AnnouncementFilter.none())).containsExactly("no position", "with position");
    }

    @Test
    void escapeLikeEscapesTheWildcardsAndTheEscapeCharacter() {
        assertThat(AnnouncementSpecifications.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
    }
}
