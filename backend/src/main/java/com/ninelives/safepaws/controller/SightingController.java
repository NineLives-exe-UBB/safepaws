package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.SightingRequest;
import com.ninelives.safepaws.dto.SightingResponse;
import com.ninelives.safepaws.service.SightingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SightingController {

    private final SightingService sightingService;

    public SightingController(SightingService sightingService) {
        this.sightingService = sightingService;
    }

    @PostMapping("/announcements/{id}/sightings")
    @ResponseStatus(HttpStatus.CREATED)
    public SightingResponse create(
            @PathVariable Long id,
            @Valid @RequestBody SightingRequest request) {

        return sightingService.create(id, request);
    }

    @GetMapping("/announcements/{id}/sightings")
    public List<SightingResponse> findByAnnouncementId(@PathVariable Long id) {
        return sightingService.findByAnnouncementId(id);
    }

    @DeleteMapping("/sightings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sightingService.delete(id);
    }
}
