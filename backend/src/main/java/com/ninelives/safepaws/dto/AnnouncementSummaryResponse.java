package com.ninelives.safepaws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementSummaryResponse(
        Long id,
        AnnouncementType type,
        String petName,
        Species species,
        String area,
        Double latitude,
        Double longitude,
        // only sent when the request has lat and lng
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Double distanceKm,
        LocalDateTime eventDate,
        AnnouncementStatus status,
        List<PhotoSummary> photos
) {

    public record PhotoSummary(
            Long id,
            String url,
            boolean isMain
    ) {
    }
}
