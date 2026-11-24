package com.ninelives.safepaws.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A user account. The table is "users" because "user" is a reserved word in PostgreSQL.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always trimmed and lowercase; the database rejects anything else. */
    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    /** BCrypt hash; the password itself is never stored. */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    /** Notification area: all three are either set together or all null (ck_users_notify_area_complete). */
    @Column(name = "notify_latitude")
    private Double notifyLatitude;

    @Column(name = "notify_longitude")
    private Double notifyLongitude;

    /** Whole number of km from 1 to 20 (ck_users_notify_radius_km). */
    @Column(name = "notify_radius_km")
    private Integer notifyRadiusKm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Double getNotifyLatitude() {
        return notifyLatitude;
    }

    public void setNotifyLatitude(Double notifyLatitude) {
        this.notifyLatitude = notifyLatitude;
    }

    public Double getNotifyLongitude() {
        return notifyLongitude;
    }

    public void setNotifyLongitude(Double notifyLongitude) {
        this.notifyLongitude = notifyLongitude;
    }

    public Integer getNotifyRadiusKm() {
        return notifyRadiusKm;
    }

    public void setNotifyRadiusKm(Integer notifyRadiusKm) {
        this.notifyRadiusKm = notifyRadiusKm;
    }

    /** Whether this user has a notification area set. */
    public boolean hasNotifyArea() {
        return notifyLatitude != null && notifyLongitude != null && notifyRadiusKm != null;
    }

    /** Clears the notification area. */
    public void clearNotifyArea() {
        this.notifyLatitude = null;
        this.notifyLongitude = null;
        this.notifyRadiusKm = null;
    }
}
