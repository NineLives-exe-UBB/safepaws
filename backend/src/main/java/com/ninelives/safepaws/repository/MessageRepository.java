package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Whether {@code senderId} has ever written to {@code recipientId} about the announcement. */
    boolean existsByAnnouncement_IdAndSender_IdAndRecipient_Id(Long announcementId, Long senderId, Long recipientId);

    /** Every message between the two users about the announcement, oldest first. */
    @Query("""
            select m from Message m
            where m.announcement.id = :announcementId
              and ((m.sender.id = :userId and m.recipient.id = :otherId)
                or (m.sender.id = :otherId and m.recipient.id = :userId))
            order by m.createdAt asc, m.id asc
            """)
    List<Message> findConversation(@Param("announcementId") Long announcementId,
                                   @Param("userId") Long userId,
                                   @Param("otherId") Long otherId);

    /** Marks what {@code userId} received from {@code otherId} about the announcement as read. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Message m set m.readAt = :now
            where m.announcement.id = :announcementId
              and m.recipient.id = :userId
              and m.sender.id = :otherId
              and m.readAt is null
            """)
    int markAsRead(@Param("announcementId") Long announcementId,
                   @Param("userId") Long userId,
                   @Param("otherId") Long otherId,
                   @Param("now") LocalDateTime now);

    /** Every message the user sent or received, newest first, with what the inbox shows already loaded. */
    @Query("""
            select m from Message m
            join fetch m.announcement
            join fetch m.sender
            join fetch m.recipient
            where m.sender.id = :userId or m.recipient.id = :userId
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findAllOfUser(@Param("userId") Long userId);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);
}
