package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.AnnouncementStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementFilterTest {

    private static AnnouncementFilter location(Double lat, Double lng, Double radiusKm, String sort) {
        return new AnnouncementFilter(null, null, null, null, null, null, lat, lng, radiusKm, sort);
    }

    private static AnnouncementFilter text(String color, String breed, String area) {
        return new AnnouncementFilter(null, null, null, color, breed, area, null, null, null, null);
    }

    private static AnnouncementFilter sort(String sort) {
        return location(null, null, null, sort);
    }

    private static List<String> messages(AnnouncementFilter filter) {
        return filter.validate().stream().map(error -> error.field() + ": " + error.message()).toList();
    }

    @Test
    void noParametersAreValidAndUseTheDefaults() {
        AnnouncementFilter filter = AnnouncementFilter.none();

        assertThat(filter.validate()).isEmpty();
        assertThat(filter.statusOrDefault()).isEqualTo(AnnouncementStatus.ACTIVE);
        assertThat(filter.sortOrDefault()).isEqualTo(AnnouncementFilter.Sort.NEWEST);
        assertThat(filter.radiusKmOrDefault()).isEqualTo(10);
        assertThat(filter.hasLocation()).isFalse();
    }

    @Test
    void sentStatusAndRadiusReplaceTheDefaults() {
        AnnouncementFilter filter = new AnnouncementFilter(null, null, AnnouncementStatus.RESOLVED,
                null, null, null, 46.77, 23.59, 2.5, null);

        assertThat(filter.statusOrDefault()).isEqualTo(AnnouncementStatus.RESOLVED);
        assertThat(filter.radiusKmOrDefault()).isEqualTo(2.5);
    }

    @Test
    void blankTextCountsAsNotSentAndTextIsTrimmed() {
        AnnouncementFilter filter = new AnnouncementFilter(null, null, null, "  ", "", " grey ", null, null, null, " ");

        assertThat(filter.color()).isNull();
        assertThat(filter.breed()).isNull();
        assertThat(filter.area()).isEqualTo("grey");
        assertThat(filter.sort()).isNull();
    }

    @Test
    void everySortValueIsAccepted() {
        assertThat(sort("newest").sortOrDefault()).isEqualTo(AnnouncementFilter.Sort.NEWEST);
        assertThat(sort("oldest").sortOrDefault()).isEqualTo(AnnouncementFilter.Sort.OLDEST);
        assertThat(location(46.77, 23.59, null, "closest").sortOrDefault()).isEqualTo(AnnouncementFilter.Sort.CLOSEST);
        assertThat(location(46.77, 23.59, null, "closest").validate()).isEmpty();
    }

    @Test
    void unknownSortListsTheAllowedValues() {
        assertThat(messages(sort("distance")))
                .containsExactly("sort: Invalid value 'distance', allowed values: newest, oldest, closest");
        // values are lowercase, like in the contract
        assertThat(messages(sort("NEWEST"))).hasSize(1);
    }

    @Test
    void closestNeedsALocation() {
        assertThat(messages(sort("closest"))).containsExactly("sort: sort=closest requires lat and lng");
    }

    @Test
    void latWithoutLngIsRejectedOnLat() {
        assertThat(messages(location(46.77, null, null, null)))
                .containsExactly("lat: Latitude and longitude must be provided together");
    }

    @Test
    void lngWithoutLatIsRejectedOnLng() {
        assertThat(messages(location(null, 23.59, null, null)))
                .containsExactly("lng: Latitude and longitude must be provided together");
    }

    @Test
    void coordinatesOutsideTheEarthAreRejected() {
        assertThat(messages(location(120.0, 200.0, null, null))).containsExactly(
                "lat: Latitude must be between -90 and 90",
                "lng: Longitude must be between -180 and 180");
        assertThat(messages(location(Double.NaN, 23.59, null, null)))
                .containsExactly("lat: Latitude must be between -90 and 90");
    }

    @Test
    void coordinatesOnTheEdgeAreAccepted() {
        assertThat(location(-90.0, 180.0, null, null).validate()).isEmpty();
        assertThat(location(90.0, -180.0, null, null).validate()).isEmpty();
    }

    @Test
    void radiusNeedsALocation() {
        assertThat(messages(location(null, null, 5.0, null))).containsExactly("radiusKm: radiusKm requires lat and lng");
    }

    @Test
    void radiusMustBeBetween1And100() {
        assertThat(messages(location(46.77, 23.59, 0.5, null)))
                .containsExactly("radiusKm: radiusKm must be between 1 and 100");
        assertThat(messages(location(46.77, 23.59, 500.0, null)))
                .containsExactly("radiusKm: radiusKm must be between 1 and 100");
        assertThat(location(46.77, 23.59, 1.0, null).validate()).isEmpty();
        assertThat(location(46.77, 23.59, 100.0, null).validate()).isEmpty();
    }

    @Test
    void textLongerThanTheColumnIsRejected() {
        assertThat(messages(text("a".repeat(101), "b".repeat(101), "c".repeat(151)))).containsExactly(
                "color: color must be at most 100 characters",
                "breed: breed must be at most 100 characters",
                "area: area must be at most 150 characters");
        assertThat(text("a".repeat(100), "b".repeat(100), "c".repeat(150)).validate()).isEmpty();
    }
}
