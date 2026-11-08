package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.AnnouncementFilter;
import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.service.AnnouncementService;
import com.ninelives.safepaws.service.PosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final PosterService posterService;

    public AnnouncementController(AnnouncementService announcementService, PosterService posterService) {
        this.announcementService = announcementService;
        this.posterService = posterService;
    }

    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse created = announcementService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List announcements",
            description = "Every parameter is optional and they combine with AND. "
                    + "Without any: only ACTIVE announcements, newest eventDate first.")
    @GetMapping
    public List<AnnouncementSummaryResponse> findAll(
            @Parameter(description = "LOST or FOUND, exact match")
            @RequestParam(required = false) AnnouncementType type,
            @Parameter(description = "CAT, DOG or OTHER, exact match")
            @RequestParam(required = false) Species species,
            @Parameter(description = "ACTIVE or RESOLVED, exact match. Default: ACTIVE")
            @RequestParam(required = false) AnnouncementStatus status,
            @Parameter(description = "Part of the color; ignores case, diacritics and spaces at the ends. "
                    + "Max 100 characters. Example: grey")
            @RequestParam(required = false) String color,
            @Parameter(description = "Part of the breed; ignores case, diacritics and spaces at the ends. "
                    + "Max 100 characters. Example: terrier")
            @RequestParam(required = false) String breed,
            @Parameter(description = "Part of the area; ignores case, diacritics and spaces at the ends. "
                    + "Max 150 characters. Example: manastur")
            @RequestParam(required = false) String area,
            @Parameter(description = "User latitude, -90 to 90. Send together with lng; adds distanceKm "
                    + "and leaves out announcements without a map position. Example: 46.77")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "User longitude, -180 to 180. Send together with lat. Example: 23.59")
            @RequestParam(required = false) Double lng,
            @Parameter(description = "Search radius in km, 1 to 100. Needs lat and lng. Default: 10")
            @RequestParam(required = false) Double radiusKm,
            @Parameter(description = "newest or oldest (by eventDate), or closest (needs lat and lng). "
                    + "Default: newest. Equal results are ordered by id",
                    schema = @Schema(allowableValues = {"newest", "oldest", "closest"}))
            @RequestParam(required = false) String sort) {
        return announcementService.findAll(
                new AnnouncementFilter(type, species, status, color, breed, area, lat, lng, radiusKm, sort));
    }

    @GetMapping("/{id}")
    public AnnouncementResponse findById(@PathVariable Long id) {
        return announcementService.findById(id);
    }

    @GetMapping("/{id}/poster")
    public ResponseEntity<byte[]> getAnnouncementPoster(@PathVariable Long id) {
        byte[] pdfBytes = posterService.generatePoster(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("announcement-" + id + "-poster.pdf")
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}
