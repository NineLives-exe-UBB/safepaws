package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The optional query parameters of GET /announcements. Every field may be null (= not sent).
 * Blank text is treated as not sent.
 */
public record AnnouncementFilter(
        AnnouncementType type,
        Species species,
        AnnouncementStatus status,
        String color,
        String breed,
        String area,
        Double lat,
        Double lng,
        Double radiusKm,
        String sort
) {

    public static final double DEFAULT_RADIUS_KM = 10;
    public static final double MIN_RADIUS_KM = 1;
    public static final double MAX_RADIUS_KM = 100;

    public enum Sort {
        NEWEST, OLDEST, CLOSEST;

        /** The value used in the API, e.g. {@code newest}. */
        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public AnnouncementFilter {
        color = blankToNull(color);
        breed = blankToNull(breed);
        area = blankToNull(area);
        sort = blankToNull(sort);
    }

    /** No parameters sent: active announcements, newest first. */
    public static AnnouncementFilter none() {
        return new AnnouncementFilter(null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Rules that need more than one parameter, or that Spring's type conversion cannot check.
     * An empty list means the filter is valid.
     */
    public List<ErrorResponse.FieldError> validate() {
        List<ErrorResponse.FieldError> errors = new ArrayList<>();

        checkLength(errors, "color", color, 100);
        checkLength(errors, "breed", breed, 100);
        checkLength(errors, "area", area, 150);

        if ((lat == null) != (lng == null)) {
            errors.add(new ErrorResponse.FieldError(lat != null ? "lat" : "lng",
                    "Latitude and longitude must be provided together"));
        }
        if (lat != null && !(lat >= -90 && lat <= 90)) {
            errors.add(new ErrorResponse.FieldError("lat", "Latitude must be between -90 and 90"));
        }
        if (lng != null && !(lng >= -180 && lng <= 180)) {
            errors.add(new ErrorResponse.FieldError("lng", "Longitude must be between -180 and 180"));
        }

        if (radiusKm != null && !hasLocation()) {
            errors.add(new ErrorResponse.FieldError("radiusKm", "radiusKm requires lat and lng"));
        } else if (radiusKm != null && !(radiusKm >= MIN_RADIUS_KM && radiusKm <= MAX_RADIUS_KM)) {
            errors.add(new ErrorResponse.FieldError("radiusKm", "radiusKm must be between 1 and 100"));
        }

        Sort parsedSort = parseSort();
        if (sort != null && parsedSort == null) {
            errors.add(new ErrorResponse.FieldError("sort", "Invalid value '" + sort + "', allowed values: "
                    + Arrays.stream(Sort.values()).map(Sort::value).collect(Collectors.joining(", "))));
        } else if (parsedSort == Sort.CLOSEST && !hasLocation()) {
            errors.add(new ErrorResponse.FieldError("sort", "sort=closest requires lat and lng"));
        }

        return errors;
    }

    public boolean hasLocation() {
        return lat != null && lng != null;
    }

    public AnnouncementStatus statusOrDefault() {
        return status != null ? status : AnnouncementStatus.ACTIVE;
    }

    public double radiusKmOrDefault() {
        return radiusKm != null ? radiusKm : DEFAULT_RADIUS_KM;
    }

    /** Call only on a valid filter. */
    public Sort sortOrDefault() {
        return sort != null ? parseSort() : Sort.NEWEST;
    }

    private Sort parseSort() {
        return Arrays.stream(Sort.values())
                .filter(value -> value.value().equals(sort))
                .findFirst()
                .orElse(null);
    }

    private static void checkLength(List<ErrorResponse.FieldError> errors, String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(new ErrorResponse.FieldError(field, field + " must be at most " + max + " characters"));
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
