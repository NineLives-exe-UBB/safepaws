package com.ninelives.safepaws.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AnnouncementRequest(

        @NotNull(message = "Type is required")
        AnnouncementType type,

        @NotNull(message = "Species is required")
        Species species,

        @Size(max = 50, message = "Pet name must be at most 50 characters")
        String petName,

        @Size(max = 100, message = "Breed must be at most 100 characters")
        String breed,

        @Size(max = 100, message = "Color must be at most 100 characters")
        String color,

        @Size(max = 500, message = "Distinctive marks must be at most 500 characters")
        String distinctiveMarks,

        @NotNull(message = "Event date is required")
        @PastOrPresent(message = "Event date cannot be in the future")
        LocalDateTime eventDate,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude,

        @NotBlank(message = "Area is required")
        @Size(max = 150, message = "Area must be at most 150 characters")
        String area,

        @NotBlank(message = "Contact name is required")
        @Size(max = 100, message = "Contact name must be at most 100 characters")
        String contactName,

        @NotBlank(message = "Contact phone is required")
        @Size(max = 20, message = "Contact phone must be at most 20 characters")
        @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "Contact phone must be a valid phone number")
        String contactPhone
) {

    /** A map pin needs both coordinates, so they are sent together or not at all. */
    @JsonIgnore
    @AssertTrue(message = "Latitude and longitude must be provided together")
    public boolean isCoordinates() {
        return (latitude == null) == (longitude == null);
    }
}
