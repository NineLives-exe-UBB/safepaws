package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.Message;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record MessageResponse(
        Long id,
        Long senderId,
        Long recipientId,
        String body,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getRecipient().getId(),
                message.getBody(),
                toSeconds(message.getCreatedAt()),
                toSeconds(message.getReadAt()));
    }

    /** The API shows times as YYYY-MM-DDTHH:mm:ss, without fractions of a second. */
    public static LocalDateTime toSeconds(LocalDateTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.SECONDS);
    }
}
