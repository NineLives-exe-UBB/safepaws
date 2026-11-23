package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.ConversationResponse;
import com.ninelives.safepaws.dto.MessageRequest;
import com.ninelives.safepaws.dto.MessageResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.Message;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.MessageRepository;
import com.ninelives.safepaws.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Messages between the owner of an announcement and the people who write about it (ADR 015).
 * Every method needs a logged-in user; the security config already answers 401 without a token.
 */
@Service
public class MessageService {

    static final String CANNOT_READ = "You can only read your own conversations";
    static final String CANNOT_WRITE = "You can only write in your own conversations";

    private final MessageRepository messageRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final AnnouncementOwnershipService ownershipService;

    public MessageService(MessageRepository messageRepository,
                          AnnouncementRepository announcementRepository,
                          UserRepository userRepository,
                          AnnouncementOwnershipService ownershipService) {
        this.messageRepository = messageRepository;
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.ownershipService = ownershipService;
    }

    /**
     * Someone who is not the owner always writes to the owner. The owner answers someone who has
     * already written, so nobody can start a conversation with their own announcement.
     */
    @Transactional
    public MessageResponse send(Long announcementId, MessageRequest request) {
        User me = ownershipService.currentUser();
        Announcement announcement = findAnnouncement(announcementId);
        Long ownerId = announcement.getOwner().getId();

        Long recipientId;
        if (me.getId().equals(ownerId)) {
            if (request.recipientId() == null) {
                throw new InvalidRequestException("recipientId", "Recipient is required");
            }
            if (!messageRepository.existsByAnnouncement_IdAndSender_IdAndRecipient_Id(
                    announcementId, request.recipientId(), ownerId)) {
                throw new InvalidRequestException("recipientId", "Recipient has not written about this announcement");
            }
            recipientId = request.recipientId();
        } else {
            // the only conversation a non-owner can write in is their own one with the owner
            if (request.recipientId() != null && !request.recipientId().equals(ownerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, CANNOT_WRITE);
            }
            recipientId = ownerId;
        }

        Message message = new Message(announcement, me, userRepository.getReferenceById(recipientId), request.body());
        return MessageResponse.from(messageRepository.saveAndFlush(message));
    }

    /**
     * The conversation between me and {@code withUserId} about the announcement, oldest first.
     * A non-owner may leave out {@code withUserId}: it means the owner. Opening it marks what I
     * received in it as read.
     */
    @Transactional
    public List<MessageResponse> readConversation(Long announcementId, Long withUserId) {
        Long me = ownershipService.currentUserId();
        Announcement announcement = findAnnouncement(announcementId);
        Long ownerId = announcement.getOwner().getId();

        Long otherId;
        if (withUserId != null) {
            otherId = withUserId;
        } else if (!me.equals(ownerId)) {
            otherId = ownerId;
        } else {
            throw new InvalidRequestException("with", "Conversation partner is required");
        }

        // every conversation is between the owner and one other person
        if (!me.equals(ownerId) && !otherId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CANNOT_READ);
        }

        messageRepository.markAsRead(announcementId, me, otherId, LocalDateTime.now());
        return messageRepository.findConversation(announcementId, me, otherId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    /** All my conversations, the one with the newest message first. */
    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations() {
        Long me = ownershipService.currentUserId();

        // messages come newest first, so the first one of each conversation is its last message
        Map<ConversationKey, ConversationBuilder> byConversation = new LinkedHashMap<>();
        for (Message message : messageRepository.findAllOfUser(me)) {
            boolean fromMe = message.getSender().getId().equals(me);
            User other = fromMe ? message.getRecipient() : message.getSender();
            ConversationBuilder builder = byConversation.computeIfAbsent(
                    new ConversationKey(message.getAnnouncement().getId(), other.getId()),
                    key -> new ConversationBuilder(message, other, fromMe));
            if (!fromMe && message.getReadAt() == null) {
                builder.unreadCount++;
            }
        }

        List<ConversationResponse> result = new ArrayList<>();
        byConversation.values().forEach(builder -> result.add(builder.build()));
        return result;
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return messageRepository.countByRecipient_IdAndReadAtIsNull(ownershipService.currentUserId());
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id).orElseThrow(() -> new AnnouncementNotFoundException(id));
    }

    private record ConversationKey(Long announcementId, Long otherUserId) {
    }

    private static final class ConversationBuilder {
        private final Message last;
        private final User other;
        private final boolean lastFromMe;
        private long unreadCount;

        private ConversationBuilder(Message last, User other, boolean lastFromMe) {
            this.last = last;
            this.other = other;
            this.lastFromMe = lastFromMe;
        }

        private ConversationResponse build() {
            Announcement announcement = last.getAnnouncement();
            return new ConversationResponse(
                    announcement.getId(),
                    announcement.getPetName(),
                    announcement.getSpecies(),
                    announcement.getType(),
                    new ConversationResponse.OtherUser(other.getId(), other.getDisplayName()),
                    new ConversationResponse.LastMessage(last.getBody(),
                            MessageResponse.toSeconds(last.getCreatedAt()), lastFromMe),
                    unreadCount);
        }
    }
}
