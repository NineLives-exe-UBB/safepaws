package com.ninelives.safepaws.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /announcements/{id}/messages. Someone who is not the owner leaves out
 * {@code recipientId}: the message goes to the owner. The owner gives the person they answer.
 */
public record MessageRequest(

        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String body,

        Long recipientId
) {

    public MessageRequest {
        // spaces around the text are not part of the message
        body = body == null ? null : body.strip();
    }
}
