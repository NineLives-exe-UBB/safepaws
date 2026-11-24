package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * My last 50 notifications, newest first. Fetches the announcement eagerly so the
     * controller does not need an extra query per notification.
     */
    @Query("""
            select n from Notification n
            join fetch n.announcement
            where n.user.id = :userId
            order by n.createdAt desc, n.id desc
            limit 50
            """)
    List<Notification> findTop50ByUserIdNewestFirst(@Param("userId") Long userId);

    /** How many of my notifications I have not read (uses idx_notification_unread). */
    long countByUser_IdAndReadAtIsNull(Long userId);

    /**
     * Find a notification by id that belongs to the user, so we can mark it read.
     * Returns empty when the notification does not exist OR belongs to someone else
     * (the API returns 404 in both cases so nobody can discover which ids exist).
     */
    @Query("select n from Notification n where n.id = :id and n.user.id = :userId")
    Optional<Notification> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** Marks all unread notifications of a user as read in one query. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n set n.readAt = :now
            where n.user.id = :userId and n.readAt is null
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Whether the user already has a notification for this announcement (uq_notification_user_announcement). */
    boolean existsByUser_IdAndAnnouncement_Id(Long userId, Long announcementId);
}
