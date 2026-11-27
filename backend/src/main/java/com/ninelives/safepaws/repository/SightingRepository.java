package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.Sighting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SightingRepository extends JpaRepository<Sighting, Long> {

    @Query("""
            SELECT s FROM Sighting s
            JOIN FETCH s.reporter
            WHERE s.announcement.id = :announcementId
            ORDER BY s.seenAt ASC, s.id ASC
            """)
    List<Sighting> findByAnnouncementId(@Param("announcementId") Long announcementId);
}
