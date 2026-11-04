package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.service.AnnouncementService;
import com.ninelives.safepaws.service.PosterService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping
    public List<AnnouncementSummaryResponse> findAll() {
        return announcementService.findAll();
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
