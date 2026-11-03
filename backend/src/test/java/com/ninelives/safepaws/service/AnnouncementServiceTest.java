package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service logic only: the repository is mocked, so no database is involved.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 11, 1, 9, 15);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    /** An announcement as it comes back from the database: id and createdAt are set by JPA. */
    private static Announcement storedAnnouncement(Long id, Species species, String petName) {
        Announcement announcement = new Announcement();
        ReflectionTestUtils.setField(announcement, "id", id);
        ReflectionTestUtils.setField(announcement, "createdAt", CREATED_AT);
        announcement.setType(AnnouncementType.LOST);
        announcement.setSpecies(species);
        announcement.setPetName(petName);
        announcement.setBreed("Golden Retriever");
        announcement.setColor("Golden");
        announcement.setDistinctiveMarks("Red nylon collar with bell");
        announcement.setEventDate(EVENT_DATE);
        announcement.setLatitude(46.7712);
        announcement.setLongitude(23.6236);
        announcement.setArea("Mărăști, Cluj-Napoca");
        announcement.setContactName("Ion Radu");
        announcement.setContactPhone("0740123456");
        return announcement;
    }

    @Test
    void createCopiesEveryRequestFieldIntoTheSavedEntity() {
        AnnouncementRequest request = new AnnouncementRequest(AnnouncementType.LOST, Species.DOG, "Oscar",
                "Golden Retriever", "Golden", "Red nylon collar with bell", EVENT_DATE, 46.7712, 23.6236,
                "Mărăști, Cluj-Napoca", "Ion Radu", "0740123456");
        when(announcementRepository.save(any())).thenReturn(storedAnnouncement(15L, Species.DOG, "Oscar"));

        announcementService.create(request);

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getType()).isEqualTo(AnnouncementType.LOST);
        assertThat(saved.getSpecies()).isEqualTo(Species.DOG);
        assertThat(saved.getPetName()).isEqualTo("Oscar");
        assertThat(saved.getBreed()).isEqualTo("Golden Retriever");
        assertThat(saved.getColor()).isEqualTo("Golden");
        assertThat(saved.getDistinctiveMarks()).isEqualTo("Red nylon collar with bell");
        assertThat(saved.getEventDate()).isEqualTo(EVENT_DATE);
        assertThat(saved.getLatitude()).isEqualTo(46.7712);
        assertThat(saved.getLongitude()).isEqualTo(23.6236);
        assertThat(saved.getArea()).isEqualTo("Mărăști, Cluj-Napoca");
        assertThat(saved.getContactName()).isEqualTo("Ion Radu");
        assertThat(saved.getContactPhone()).isEqualTo("0740123456");
        assertThat(saved.getStatus()).isEqualTo(AnnouncementStatus.ACTIVE);
    }

    @Test
    void createReturnsTheSavedAnnouncement() {
        AnnouncementRequest request = new AnnouncementRequest(AnnouncementType.LOST, Species.DOG, "Oscar",
                null, null, null, EVENT_DATE, null, null, "Mărăști, Cluj-Napoca", "Ion Radu", "0740123456");
        when(announcementRepository.save(any())).thenReturn(storedAnnouncement(15L, Species.DOG, "Oscar"));

        AnnouncementResponse response = announcementService.create(request);

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.status()).isEqualTo(AnnouncementStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.species()).isEqualTo(Species.DOG);
        assertThat(response.photos()).isEmpty();
    }

    @Test
    void findAllReturnsSummariesInRepositoryOrder() {
        when(announcementRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(
                storedAnnouncement(13L, Species.DOG, "Max"),
                storedAnnouncement(11L, Species.CAT, "Tom"),
                storedAnnouncement(14L, Species.OTHER, null)));

        List<AnnouncementSummaryResponse> result = announcementService.findAll();

        assertThat(result).extracting(AnnouncementSummaryResponse::id).containsExactly(13L, 11L, 14L);
        AnnouncementSummaryResponse first = result.getFirst();
        assertThat(first.type()).isEqualTo(AnnouncementType.LOST);
        assertThat(first.petName()).isEqualTo("Max");
        assertThat(first.area()).isEqualTo("Mărăști, Cluj-Napoca");
        assertThat(first.eventDate()).isEqualTo(EVENT_DATE);
        assertThat(first.status()).isEqualTo(AnnouncementStatus.ACTIVE);
        assertThat(first.photos()).isEmpty();
    }

    @Test
    void findAllUsesTheSpeciesDisplayName() {
        when(announcementRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(
                storedAnnouncement(1L, Species.CAT, null),
                storedAnnouncement(2L, Species.DOG, null),
                storedAnnouncement(3L, Species.OTHER, null)));

        assertThat(announcementService.findAll())
                .extracting(AnnouncementSummaryResponse::species)
                .containsExactly("Cat", "Dog", "Other");
    }

    @Test
    void findAllWithNoAnnouncementsReturnsAnEmptyList() {
        when(announcementRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of());

        assertThat(announcementService.findAll()).isEmpty();
    }

    @Test
    void findByIdReturnsEveryField() {
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(storedAnnouncement(1L, Species.CAT, "Mitzi")));

        AnnouncementResponse response = announcementService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(AnnouncementType.LOST);
        assertThat(response.species()).isEqualTo(Species.CAT);
        assertThat(response.petName()).isEqualTo("Mitzi");
        assertThat(response.breed()).isEqualTo("Golden Retriever");
        assertThat(response.color()).isEqualTo("Golden");
        assertThat(response.distinctiveMarks()).isEqualTo("Red nylon collar with bell");
        assertThat(response.eventDate()).isEqualTo(EVENT_DATE);
        assertThat(response.latitude()).isEqualTo(46.7712);
        assertThat(response.longitude()).isEqualTo(23.6236);
        assertThat(response.area()).isEqualTo("Mărăști, Cluj-Napoca");
        assertThat(response.contactName()).isEqualTo("Ion Radu");
        assertThat(response.contactPhone()).isEqualTo("0740123456");
        assertThat(response.status()).isEqualTo(AnnouncementStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.photos()).isEmpty();
    }

    @Test
    void findByIdThrowsWhenTheAnnouncementDoesNotExist() {
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.findById(999L))
                .isInstanceOf(AnnouncementNotFoundException.class)
                .hasMessage("Announcement not found with id: 999");
    }
}