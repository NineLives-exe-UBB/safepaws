package com.ninelives.safepaws.dto;

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
        LocalDateTime eventDate,
        AnnouncementStatus status,
        List<Photo> photos
) {

    public record Photo(
            Long id,
            String url,
            boolean isMain
    ) {
    }
}
