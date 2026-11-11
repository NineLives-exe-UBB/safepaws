package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.AnnouncementStatus;
import jakarta.validation.constraints.NotNull;

public record AnnouncementStatusRequest(
        @NotNull(message = "Status is required")
        AnnouncementStatus status
) {
}
