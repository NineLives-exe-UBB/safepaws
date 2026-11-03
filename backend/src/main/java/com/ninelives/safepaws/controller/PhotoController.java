
package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.PhotoResponse;
import com.ninelives.safepaws.model.Photo;
import com.ninelives.safepaws.service.PhotoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping(value = "/announcements/{id}/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PhotoResponse>> upload(
            @PathVariable Long id,
            @RequestPart("files") MultipartFile[] files) {

        List<PhotoResponse> photos = photoService.upload(id, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(photos);
    }

    @GetMapping("/photos/{id}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {

        Photo photo = photoService.getPhoto(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(photo.getData());
    }
}
