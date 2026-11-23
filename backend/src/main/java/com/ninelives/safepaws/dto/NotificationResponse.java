package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Notification;
import com.ninelives.safepaws.model.Species;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * One item in GET /notifications (last 50, newest first).
 */
public record NotificationResponse(
        Long id,
        AnnouncementSummary announcement,
        LocalDateTime createdAt,
        boolean read
) {

    public record AnnouncementSummary(
            Long id,
            AnnouncementType type,
            Species species,
            String petName,
            String area
    ) {
    }

    public static NotificationResponse from(Notification n) {
        var a = n.getAnnouncement();
        return new NotificationResponse(
                n.getId(),
                new AnnouncementSummary(a.getId(), a.getType(), a.getSpecies(), a.getPetName(), a.getArea()),
                toSeconds(n.getCreatedAt()),
                n.getReadAt() != null
        );
    }

    /** Times are YYYY-MM-DDTHH:mm:ss, without fractions of a second. */
    private static LocalDateTime toSeconds(LocalDateTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.SECONDS);
    }
}
