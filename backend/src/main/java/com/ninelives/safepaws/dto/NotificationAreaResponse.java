package com.ninelives.safepaws.dto;

/**
 * Body for GET /notifications/area and PUT /notifications/area response.
 */
public record NotificationAreaResponse(
        Double latitude,
        Double longitude,
        Integer radiusKm
) {
}
