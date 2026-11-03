package com.ninelives.safepaws.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private AnnouncementType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false, length = 20)
    private Species species;

    @Column(name = "pet_name", length = 50)
    private String petName;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "distinctive_marks", length = 500)
    private String distinctiveMarks;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "area", nullable = false, length = 150)
    private String area;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AnnouncementStatus status = AnnouncementStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Announcement() {
    }

    public Long getId() {
        return id;
    }

    public AnnouncementType getType() {
        return type;
    }

    public void setType(AnnouncementType type) {
        this.type = type;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDistinctiveMarks() {
        return distinctiveMarks;
    }

    public void setDistinctiveMarks(String distinctiveMarks) {
        this.distinctiveMarks = distinctiveMarks;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public AnnouncementStatus getStatus() {
        return status;
    }

    public void setStatus(AnnouncementStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
