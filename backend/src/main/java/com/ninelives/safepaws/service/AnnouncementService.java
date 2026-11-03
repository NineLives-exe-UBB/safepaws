package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.dto.PhotoResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.PhotoMetadata;
import com.ninelives.safepaws.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final PhotoRepository photoRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               PhotoRepository photoRepository) {
        this.announcementRepository = announcementRepository;
        this.photoRepository = photoRepository;
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request) {
        Announcement saved = announcementRepository.save(toEntity(request));
        // photos are uploaded separately, after the announcement exists
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementSummaryResponse> findAll() {
        List<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDescIdDesc();
        if (announcements.isEmpty()) {
            return List.of();
        }

        // one query for the photos of every announcement, instead of one per announcement
        Map<Long, List<PhotoMetadata>> photosByAnnouncement = photoRepository
                .findMetadataByAnnouncementIds(announcements.stream().map(Announcement::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(PhotoMetadata::announcementId));

        return announcements.stream()
                .map(announcement -> toSummaryResponse(announcement,
                        photosByAnnouncement.getOrDefault(announcement.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse findById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        return toResponse(announcement, photoRepository.findMetadataByAnnouncementIds(List.of(id)));
    }

    private Announcement toEntity(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setType(request.type());
        announcement.setSpecies(request.species());
        announcement.setPetName(request.petName());
        announcement.setBreed(request.breed());
        announcement.setColor(request.color());
        announcement.setDistinctiveMarks(request.distinctiveMarks());
        announcement.setEventDate(request.eventDate());
        announcement.setLatitude(request.latitude());
        announcement.setLongitude(request.longitude());
        announcement.setArea(request.area());
        announcement.setContactName(request.contactName());
        announcement.setContactPhone(request.contactPhone());
        // status defaults to ACTIVE and createdAt is set on insert
        return announcement;
    }

    private AnnouncementResponse toResponse(Announcement announcement, List<PhotoMetadata> photos) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getType(),
                announcement.getSpecies(),
                announcement.getPetName(),
                announcement.getBreed(),
                announcement.getColor(),
                announcement.getDistinctiveMarks(),
                announcement.getEventDate(),
                announcement.getLatitude(),
                announcement.getLongitude(),
                announcement.getArea(),
                announcement.getContactName(),
                announcement.getContactPhone(),
                announcement.getStatus(),
                announcement.getCreatedAt(),
                photos.stream()
                        .map(photo -> new PhotoResponse(
                                photo.id(),
                                photo.announcementId(),
                                photo.contentType(),
                                photo.fileSize(),
                                photo.originalFileName(),
                                photo.isMain(),
                                photoUrl(photo.id()),
                                photo.uploadedAt()))
                        .toList()
        );
    }

    private AnnouncementSummaryResponse toSummaryResponse(Announcement announcement, List<PhotoMetadata> photos) {
        return new AnnouncementSummaryResponse(
                announcement.getId(),
                announcement.getType(),
                announcement.getPetName(),
                announcement.getSpecies(),
                announcement.getArea(),
                announcement.getEventDate(),
                announcement.getStatus(),
                photos.stream()
                        .map(photo -> new AnnouncementSummaryResponse.Photo(
                                photo.id(), photoUrl(photo.id()), photo.isMain()))
                        .toList()
        );
    }

    private static String photoUrl(Long photoId) {
        return "/photos/" + photoId;
    }
}

