package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request) {
        Announcement saved = announcementRepository.save(toEntity(request));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementSummaryResponse> findAll() {
        return announcementRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse findById(Long id) {
        return announcementRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
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

    private AnnouncementResponse toResponse(Announcement announcement) {
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
                // photos are uploaded separately, after the announcement exists
                List.of()
        );
    }

    private AnnouncementSummaryResponse toSummaryResponse(Announcement announcement) {
        return new AnnouncementSummaryResponse(
                announcement.getId(),
                announcement.getType(),
                announcement.getPetName(),
                announcement.getSpecies().getDisplayName(),
                announcement.getArea(),
                announcement.getEventDate(),
                announcement.getStatus(),
                List.of()
        );
    }
}

