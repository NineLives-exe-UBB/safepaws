package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.NotificationAreaRequest;
import com.ninelives.safepaws.dto.NotificationAreaResponse;
import com.ninelives.safepaws.dto.NotificationResponse;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.Notification;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.NotificationRepository;
import com.ninelives.safepaws.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification area management and in-app notifications for new announcements (ADR 015, ticket V9).
 *
 * <p>When a new announcement is posted, the caller (AnnouncementService) calls
 * {@link #notifyUsersInArea(Announcement)} so every user whose area contains the announcement's
 * location gets a notification row. The author never gets one for their own announcement, and an
 * announcement without coordinates sends none.
 */
@Service
public class NotificationService {

    static final String AREA_NOT_SET = "Notification area is not set";
    static final String NOTIFICATION_NOT_FOUND = "Notification not found with id: ";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AnnouncementOwnershipService ownershipService;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                AnnouncementOwnershipService ownershipService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.ownershipService = ownershipService;
    }

    // ── Notification area ────────────────────────────────────────────────────

    /** Returns the current user's notification area, or 404 if none is set. */
    @Transactional(readOnly = true)
    public NotificationAreaResponse getArea() {
        User user = ownershipService.currentUser();
        if (!user.hasNotifyArea()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, AREA_NOT_SET);
        }
        return toAreaResponse(user);
    }

    /** Saves (or replaces) the current user's notification area. */
    @Transactional
    public NotificationAreaResponse saveArea(NotificationAreaRequest request) {
        User user = ownershipService.currentUser();
        user.setNotifyLatitude(request.latitude());
        user.setNotifyLongitude(request.longitude());
        user.setNotifyRadiusKm(request.radiusKm());
        userRepository.save(user);
        return toAreaResponse(user);
    }

    /** Removes the current user's notification area; old notifications stay. */
    @Transactional
    public void deleteArea() {
        User user = ownershipService.currentUser();
        user.clearNotifyArea();
        userRepository.save(user);
    }

    private static NotificationAreaResponse toAreaResponse(User user) {
        return new NotificationAreaResponse(
                user.getNotifyLatitude(),
                user.getNotifyLongitude(),
                user.getNotifyRadiusKm());
    }

    // ── Notification list ────────────────────────────────────────────────────

    /** My last 50 notifications, newest first. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        Long userId = ownershipService.currentUserId();
        return notificationRepository.findTop50ByUserIdNewestFirst(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** How many of my notifications I have not read. */
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUser_IdAndReadAtIsNull(ownershipService.currentUserId());
    }

    // ── Mark as read ─────────────────────────────────────────────────────────

    /**
     * Marks one notification as read. Returns 404 when it does not exist or belongs to
     * someone else (so nobody can discover which ids exist).
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = ownershipService.currentUserId();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, NOTIFICATION_NOT_FOUND + notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /** Marks all of the current user's unread notifications as read. */
    @Transactional
    public void markAllAsRead() {
        Long userId = ownershipService.currentUserId();
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    // ── Trigger ──────────────────────────────────────────────────────────────

    /**
     * Called by AnnouncementService after a new announcement is saved. Creates one notification
     * for every user whose area contains the announcement's location, except the author.
     *
     * <p>An announcement without coordinates triggers no notifications (the location is unknown).
     */
    @Transactional
    public void notifyUsersInArea(Announcement announcement) {
        if (announcement.getLatitude() == null || announcement.getLongitude() == null) {
            return;
        }

        Long authorId = announcement.getOwner() != null ? announcement.getOwner().getId() : null;
        double announcementLat = announcement.getLatitude();
        double announcementLng = announcement.getLongitude();

        // Load only users who have an area set. This is typically a small set, so loading all
        // is acceptable (same reasoning as the text filter table scan in AnnouncementService).
        List<User> candidates = userRepository.findAllWithNotifyArea();
        for (User candidate : candidates) {
            if (candidate.getId().equals(authorId)) {
                continue; // the author does not get a notification for their own announcement
            }
            double distanceKm = AnnouncementService.distanceKm(
                    candidate.getNotifyLatitude(), candidate.getNotifyLongitude(),
                    announcementLat, announcementLng);
            if (distanceKm <= candidate.getNotifyRadiusKm()) {
                // The unique constraint uq_notification_user_announcement prevents duplicates
                // if this is called more than once; we guard with existsBy to avoid a DB error.
                if (!notificationRepository.existsByUser_IdAndAnnouncement_Id(
                        candidate.getId(), announcement.getId())) {
                    notificationRepository.save(new Notification(candidate, announcement));
                }
            }
        }
    }
}
