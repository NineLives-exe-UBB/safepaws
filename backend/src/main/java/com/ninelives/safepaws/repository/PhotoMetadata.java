package com.ninelives.safepaws.repository;

import java.time.LocalDateTime;

public record PhotoMetadata(
        Long id,
        Long announcementId,
        String contentType,
        Long fileSize,
        String originalFileName,
        boolean isMain,
        LocalDateTime uploadedAt
) {
}
