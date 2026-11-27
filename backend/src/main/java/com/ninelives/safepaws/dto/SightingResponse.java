package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.Sighting;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record SightingResponse(
        Long id,
        Double latitude,
        Double longitude,
        LocalDateTime seenAt,
        String note,
        Reporter reporter,
        LocalDateTime createdAt
) {

    public record Reporter(
            Long id,
            String displayName
    ) {
    }

    public static SightingResponse from(Sighting sighting) {
        return new SightingResponse(
                sighting.getId(),
                sighting.getLatitude(),
                sighting.getLongitude(),
                sighting.getSeenAt().truncatedTo(ChronoUnit.SECONDS),
                sighting.getNote(),
                new Reporter(
                        sighting.getReporter().getId(),
                        sighting.getReporter().getDisplayName()
                ),
                sighting.getCreatedAt().truncatedTo(ChronoUnit.SECONDS)
        );
    }
}
