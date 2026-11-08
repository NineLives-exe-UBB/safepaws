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

    /**
     * Every parameter is optional. Without any: active announcements, newest event first.
     */
    @GetMapping
    public List<AnnouncementSummaryResponse> findAll(
            @RequestParam(required = false) AnnouncementType type,
            @RequestParam(required = false) Species species,
            @RequestParam(required = false) AnnouncementStatus status,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
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
