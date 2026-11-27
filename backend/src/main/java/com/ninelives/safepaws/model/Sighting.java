package com.ninelives.safepaws.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sighting")
public class Sighting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "seen_at", nullable = false)
    private LocalDateTime seenAt;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Sighting() {
    }

    public Sighting(Announcement announcement, User reporter,
                    Double latitude, Double longitude,
                    LocalDateTime seenAt, String note) {
        this.announcement = announcement;
        this.reporter = reporter;
        this.latitude = latitude;
        this.longitude = longitude;
        this.seenAt = seenAt;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Announcement getAnnouncement() {
        return announcement;
    }

    public User getReporter() {
        return reporter;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public LocalDateTime getSeenAt() {
        return seenAt;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
