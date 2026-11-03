package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    boolean existsByAnnouncement_IdAndIsMainTrue(Long announcementId);

    @Query("""
            select new com.ninelives.safepaws.repository.PhotoMetadata(
                p.id, p.announcement.id, p.contentType, p.fileSize, p.originalFileName, p.isMain, p.uploadedAt)
            from Photo p
            where p.announcement.id in :announcementIds
            order by p.isMain desc, p.id asc
            """)
    List<PhotoMetadata> findMetadataByAnnouncementIds(@Param("announcementIds") Collection<Long> announcementIds);
}
