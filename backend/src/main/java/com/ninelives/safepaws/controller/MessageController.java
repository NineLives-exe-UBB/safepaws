package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.ConversationResponse;
import com.ninelives.safepaws.dto.MessageRequest;
import com.ninelives.safepaws.dto.MessageResponse;
import com.ninelives.safepaws.dto.UnreadCountResponse;
import com.ninelives.safepaws.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Messages about announcements (ADR 015). Every endpoint needs a token.
 */
@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/announcements/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable Long id, @Valid @RequestBody MessageRequest request) {
        return messageService.send(id, request);
    }

    @GetMapping("/announcements/{id}/messages")
    public List<MessageResponse> conversation(@PathVariable Long id,
                                              @RequestParam(name = "with", required = false) Long with) {
        return messageService.readConversation(id, with);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations() {
        return messageService.conversations();
    }

    @GetMapping("/messages/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(messageService.unreadCount());
    }
}
