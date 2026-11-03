package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementResponse(
        Long id,
        AnnouncementType type,
        Species species,
        String petName,
        String breed,
        String color,
        String distinctiveMarks,
        LocalDateTime eventDate,
        Double latitude,
        Double longitude,
        String area,
        String contactName,
        String contactPhone,
        AnnouncementStatus status,
        LocalDateTime createdAt,
        List<PhotoResponse> photos
) {
}
