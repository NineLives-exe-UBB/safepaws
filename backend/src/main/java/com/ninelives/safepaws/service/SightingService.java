package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.SightingRequest;
import com.ninelives.safepaws.dto.SightingResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Sighting;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.SightingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SightingService {

    private final SightingRepository sightingRepository;
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementOwnershipService ownershipService;

    public SightingService(SightingRepository sightingRepository,
                           AnnouncementRepository announcementRepository,
                           AnnouncementOwnershipService ownershipService) {
        this.sightingRepository = sightingRepository;
        this.announcementRepository = announcementRepository;
        this.ownershipService = ownershipService;
    }

    @Transactional
    public SightingResponse create(Long announcementId, SightingRequest request) {

        var reporter = ownershipService.currentUser();

        Announcement announcement = findAnnouncement(announcementId);

        if (announcement.getType() != AnnouncementType.LOST) {
            throw new InvalidRequestException(
                    "announcementId",
                    "Sightings can only be added to LOST announcements"
            );
        }

        if (announcement.getStatus() != AnnouncementStatus.ACTIVE) {
            throw new InvalidRequestException(
                    "announcementId",
                    "Sightings can only be added to ACTIVE announcements"
            );
        }

        if (request.seenAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException(
                    "seenAt",
                    "Sighting date cannot be in the future"
            );
        }

        if (request.seenAt().isBefore(announcement.getEventDate())) {
            throw new InvalidRequestException(
                    "seenAt",
                    "Sighting date cannot be before the pet was lost"
            );
        }

        Sighting sighting = new Sighting(
                announcement,
                reporter,
                request.latitude(),
                request.longitude(),
                request.seenAt(),
                request.note()
        );

        return SightingResponse.from(sightingRepository.saveAndFlush(sighting));
    }

    @Transactional(readOnly = true)
    public List<SightingResponse> findByAnnouncementId(Long announcementId) {

        findAnnouncement(announcementId);

        return sightingRepository.findByAnnouncementId(announcementId)
                .stream()
                .map(SightingResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long sightingId) {

        Long currentUserId = ownershipService.currentUserId();

        Sighting sighting = sightingRepository.findById(sightingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sighting not found"
                ));

        Long reporterId = sighting.getReporter().getId();
        Long ownerId = sighting.getAnnouncement().getOwner().getId();

        if (!currentUserId.equals(reporterId) &&
                !currentUserId.equals(ownerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the reporter or announcement owner can delete this sighting"
            );
        }

        sightingRepository.delete(sighting);
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
    }
}
