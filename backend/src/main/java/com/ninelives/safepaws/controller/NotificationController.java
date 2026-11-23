package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.NotificationAreaRequest;
import com.ninelives.safepaws.dto.NotificationAreaResponse;
import com.ninelives.safepaws.dto.NotificationResponse;
import com.ninelives.safepaws.dto.UnreadCountResponse;
import com.ninelives.safepaws.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notification area management and in-app notifications (ADR 015, V9).
 * Every endpoint needs a token; the security config already answers 401 without one.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── Notification area ────────────────────────────────────────────────────

    @GetMapping("/area")
    public NotificationAreaResponse getArea() {
        return notificationService.getArea();
    }

    @PutMapping("/area")
    public NotificationAreaResponse saveArea(@Valid @RequestBody NotificationAreaRequest request) {
        return notificationService.saveArea(request);
    }

    @DeleteMapping("/area")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArea() {
        notificationService.deleteArea();
    }

    // ── Notification list ────────────────────────────────────────────────────

    @GetMapping
    public List<NotificationResponse> listNotifications() {
        return notificationService.listNotifications();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notificationService.unreadCount());
    }

    // ── Mark as read ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        notificationService.markAllAsRead();
    }
}
