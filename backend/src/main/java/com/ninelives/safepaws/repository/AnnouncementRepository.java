package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>,
        JpaSpecificationExecutor<Announcement> {
}
