package com.ninelives.safepaws.dto;

import java.time.LocalDateTime;

public record PhotoResponse(
        Long id,
        Long announcementId,
        String contentType,
        Long fileSize,
        String originalFileName,
        boolean isMain,
        String url,
        LocalDateTime uploadedAt
) {
    public static String urlFor(Long photoId) {
        return "/photos/" + photoId;
    }
}
