package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.SightingRequest;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.model.*;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.SightingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SightingServiceTest {

    @Mock
    private SightingRepository sightingRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementOwnershipService ownershipService;

    @InjectMocks
    private SightingService sightingService;

    private User user(long id) {
        User user = new User("user@example.com", "hash", "User " + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Announcement announcement() {
        Announcement announcement = new Announcement();
        ReflectionTestUtils.setField(announcement, "id", 10L);
        announcement.setOwner(user(1L));
        announcement.setType(AnnouncementType.LOST);
        announcement.setStatus(AnnouncementStatus.ACTIVE);
        announcement.setEventDate(LocalDateTime.now().minusDays(2));
        return announcement;
    }

    private SightingRequest request() {
        return new SightingRequest(
                46.7712,
                23.6236,
                LocalDateTime.now().minusHours(1),
                "I saw the pet near the park"
        );
    }

    private Sighting sighting() {
        SightingRequest request = request();
        Sighting sighting = new Sighting(
                announcement(),
                user(2L),
                request.latitude(),
                request.longitude(),
                request.seenAt(),
                request.note()
        );
        ReflectionTestUtils.setField(sighting, "id", 50L);
        ReflectionTestUtils.setField(sighting, "createdAt", LocalDateTime.now());
        return sighting;
    }

    @Test
    void createsSightingForActiveLostAnnouncement() {
        when(ownershipService.currentUser()).thenReturn(user(2L));
        when(announcementRepository.findById(10L))
                .thenReturn(Optional.of(announcement()));

        when(sightingRepository.saveAndFlush(any(Sighting.class)))
                .thenAnswer(invocation -> {
                    Sighting saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 50L);
                    ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
                    return saved;
                });

        var response = sightingService.create(10L, request());

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.latitude()).isEqualTo(46.7712);
        assertThat(response.longitude()).isEqualTo(23.6236);
        assertThat(response.reporter().id()).isEqualTo(2L);
        assertThat(response.reporter().displayName()).isEqualTo("User 2");
        verify(sightingRepository).saveAndFlush(any(Sighting.class));
    }

    @Test
    void rejectsSightingsForFoundAnnouncements() {
        Announcement announcement = announcement();
        announcement.setType(AnnouncementType.FOUND);

        when(ownershipService.currentUser()).thenReturn(user(2L));
        when(announcementRepository.findById(10L))
                .thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> sightingService.create(10L, request()))
                .isInstanceOf(InvalidRequestException.class);

        verify(sightingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSightingsForResolvedAnnouncements() {
        Announcement announcement = announcement();
        announcement.setStatus(AnnouncementStatus.RESOLVED);

        when(ownershipService.currentUser()).thenReturn(user(2L));
        when(announcementRepository.findById(10L))
                .thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> sightingService.create(10L, request()))
                .isInstanceOf(InvalidRequestException.class);

        verify(sightingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSightingsBeforePetWasLost() {
        when(ownershipService.currentUser()).thenReturn(user(2L));
        when(announcementRepository.findById(10L))
                .thenReturn(Optional.of(announcement()));

        SightingRequest invalid = new SightingRequest(
                46.77, 23.62,
                LocalDateTime.now().minusDays(3),
                null
        );

        assertThatThrownBy(() -> sightingService.create(10L, invalid))
                .isInstanceOf(InvalidRequestException.class);

        verify(sightingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsFutureSightings() {
        when(ownershipService.currentUser()).thenReturn(user(2L));
        when(announcementRepository.findById(10L))
                .thenReturn(Optional.of(announcement()));

        SightingRequest invalid = new SightingRequest(
                46.77, 23.62,
                LocalDateTime.now().plusDays(1),
                null
        );

        assertThatThrownBy(() -> sightingService.create(10L, invalid))
                .isInstanceOf(InvalidRequestException.class);

        verify(sightingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingAnnouncements() {
        when(ownershipService.currentUser()).thenReturn(user(2L));
        when(announcementRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sightingService.create(999L, request()))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void listsSightingsWithReporterInformation() {
        when(announcementRepository.findById(10L))
                .thenReturn(Optional.of(announcement()));
        when(sightingRepository.findByAnnouncementId(10L))
                .thenReturn(List.of(sighting()));

        var result = sightingService.findByAnnouncementId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(50L);
        assertThat(result.getFirst().reporter().displayName())
                .isEqualTo("User 2");
    }

    @Test
    void reporterCanDeleteSighting() {
        when(ownershipService.currentUserId()).thenReturn(2L);
        Sighting sighting = sighting();
        when(sightingRepository.findById(50L))
                .thenReturn(Optional.of(sighting));

        sightingService.delete(50L);

        verify(sightingRepository).delete(sighting);
    }

    @Test
    void announcementOwnerCanDeleteSighting() {
        when(ownershipService.currentUserId()).thenReturn(1L);
        Sighting sighting = sighting();
        when(sightingRepository.findById(50L))
                .thenReturn(Optional.of(sighting));

        sightingService.delete(50L);

        verify(sightingRepository).delete(sighting);
    }

    @Test
    void otherUsersCannotDeleteSighting() {
        when(ownershipService.currentUserId()).thenReturn(3L);
        when(sightingRepository.findById(50L))
                .thenReturn(Optional.of(sighting()));

        assertThatThrownBy(() -> sightingService.delete(50L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));

        verify(sightingRepository, never()).delete(any());
    }

    @Test
    void deletingMissingSightingReturns404() {
        when(ownershipService.currentUserId()).thenReturn(2L);
        when(sightingRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sightingService.delete(999L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
