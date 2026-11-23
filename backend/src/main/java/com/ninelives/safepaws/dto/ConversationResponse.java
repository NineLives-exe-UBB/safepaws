package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;

import java.time.LocalDateTime;

/**
 * One line of GET /conversations: a conversation about an announcement with one other person.
 */
public record ConversationResponse(
        Long announcementId,
        String petName,
        Species species,
        AnnouncementType type,
        OtherUser otherUser,
        LastMessage lastMessage,
        long unreadCount
) {

    public record OtherUser(Long id, String displayName) {
    }

    public record LastMessage(String body, LocalDateTime createdAt, boolean fromMe) {
    }
}
