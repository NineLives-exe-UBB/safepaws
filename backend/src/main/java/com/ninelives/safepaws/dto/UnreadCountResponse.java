package com.ninelives.safepaws.dto;

/**
 * Body of GET /messages/unread-count, checked by the header every 30 seconds (ADR 015).
 */
public record UnreadCountResponse(long count) {
}
