package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.AnnouncementFilter;
import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.dto.ErrorResponse;
import com.ninelives.safepaws.dto.PhotoResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidFilterException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.AnnouncementSpecifications;
import com.ninelives.safepaws.repository.PhotoMetadata;
import com.ninelives.safepaws.repository.PhotoRepository;
import com.ninelives.safepaws.model.AnnouncementStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private static final double EARTH_RADIUS_KM = 6371.0;

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
    public List<AnnouncementSummaryResponse> findAll(AnnouncementFilter filter) {
        List<ErrorResponse.FieldError> errors = filter.validate();
        if (!errors.isEmpty()) {
            throw new InvalidFilterException(errors);
        }

        AnnouncementFilter.Sort sort = filter.sortOrDefault();
        List<Announcement> announcements = announcementRepository.findAll(
                AnnouncementSpecifications.matching(filter), databaseOrder(sort));

        List<Match> matches = announcements.stream()
                .map(announcement -> new Match(announcement, distanceFrom(filter, announcement)))
                .filter(match -> match.distanceKm() == null || match.distanceKm() <= filter.radiusKmOrDefault())
                .toList();
        if (sort == AnnouncementFilter.Sort.CLOSEST) {
            // a stable sort: equal distances keep the database order, newest event first
            matches = matches.stream().sorted(Comparator.comparingDouble(Match::distanceKm)).toList();
        }
        if (matches.isEmpty()) {
            return List.of();
        }

        // one query for the photos of every announcement, instead of one per announcement
        Map<Long, List<PhotoMetadata>> photosByAnnouncement = photoRepository
                .findMetadataByAnnouncementIds(matches.stream().map(match -> match.announcement().getId()).toList())
                .stream()
                .collect(Collectors.groupingBy(PhotoMetadata::announcementId));

        return matches.stream()
                .map(match -> toSummaryResponse(match.announcement(), roundToOneDecimal(match.distanceKm()),
                        photosByAnnouncement.getOrDefault(match.announcement().getId(), List.of())))
                .toList();
    }

    /** An announcement that passed the filter, with its exact distance when a location was sent. */
    private record Match(Announcement announcement, Double distanceKm) {
    }

    /** "newest" and "oldest" are about eventDate, the date shown on the card; ties go by id. */
    private static Sort databaseOrder(AnnouncementFilter.Sort sort) {
        return sort == AnnouncementFilter.Sort.OLDEST
                ? Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("eventDate"), Sort.Order.desc("id"));
    }

    private static Double distanceFrom(AnnouncementFilter filter, Announcement announcement) {
        if (!filter.hasLocation()) {
            return null;
        }
        return distanceKm(filter.lat(), filter.lng(), announcement.getLatitude(), announcement.getLongitude());
    }

    /** Great-circle (haversine) distance between two points, in kilometres. */
    static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    private static Double roundToOneDecimal(Double value) {
        return value == null ? null : Math.round(value * 10) / 10.0;
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse findById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        return toResponse(announcement, photoRepository.findMetadataByAnnouncementIds(List.of(id)));
    }


    @Transactional
    public AnnouncementResponse updateStatus(Long id, AnnouncementStatus status) {

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));

        announcement.setStatus(status);

        Announcement saved = announcementRepository.save(announcement);

        return toResponse(saved,
                photoRepository.findMetadataByAnnouncementIds(List.of(id)));
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
                                PhotoResponse.urlFor(photo.id()),
                                photo.uploadedAt()))
                        .toList()
        );
    }

    private AnnouncementSummaryResponse toSummaryResponse(Announcement announcement, Double distanceKm,
                                                          List<PhotoMetadata> photos) {
        return new AnnouncementSummaryResponse(
                announcement.getId(),
                announcement.getType(),
                announcement.getPetName(),
                announcement.getSpecies(),
                announcement.getArea(),
                announcement.getLatitude(),
                announcement.getLongitude(),
                distanceKm,
                announcement.getEventDate(),
                announcement.getStatus(),
                photos.stream()
                        .map(photo -> new AnnouncementSummaryResponse.Photo(
                                photo.id(), PhotoResponse.urlFor(photo.id()), photo.isMain()))
                        .toList()
        );
    }
}

