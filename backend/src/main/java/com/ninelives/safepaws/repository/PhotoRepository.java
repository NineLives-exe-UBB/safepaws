package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    boolean existsByAnnouncement_IdAndIsMainTrue(Long announcementId);
}
