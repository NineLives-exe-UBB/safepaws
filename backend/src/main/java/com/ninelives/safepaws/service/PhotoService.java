package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.PhotoResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.Photo;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.PhotoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PhotoService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final PhotoRepository photoRepository;
    private final AnnouncementRepository announcementRepository;

    public PhotoService(PhotoRepository photoRepository,
                        AnnouncementRepository announcementRepository) {
        this.photoRepository = photoRepository;
        this.announcementRepository = announcementRepository;
    }

    @Transactional
    public List<PhotoResponse> upload(Long announcementId, MultipartFile[] files) {

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException(announcementId));

        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }

        boolean hasMain = photoRepository.existsByAnnouncement_IdAndIsMainTrue(announcementId);
        List<PhotoResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
            }

            if (file.getSize() > MAX_SIZE) {
                throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Maximum file size is 5 MB");
            }

            String type = file.getContentType();

            if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG and PNG images are allowed");
            }

            String fileName = file.getOriginalFilename();

            if (fileName != null && fileName.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is too long");
            }

            byte[] data;

            try {
                data = file.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read uploaded file", e);
            }

            if (!isValidImage(data, type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
            }

            Photo photo = new Photo(announcement, data, type, fileName, !hasMain);
            photo = photoRepository.saveAndFlush(photo);

            responses.add(new PhotoResponse(
                    photo.getId(),
                    announcementId,
                    photo.getContentType(),
                    photo.getFileSize(),
                    photo.getOriginalFileName(),
                    photo.isMain(),
                    PhotoResponse.urlFor(photo.getId()),
                    photo.getUploadedAt()
            ));

            hasMain = true;
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public Photo getPhoto(Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Photo not found with id: " + id
                ));
    }

    private boolean isValidImage(byte[] data, String type) {
        if ("image/jpeg".equals(type)) {
            return data.length >= 3
                    && (data[0] & 0xFF) == 0xFF
                    && (data[1] & 0xFF) == 0xD8
                    && (data[2] & 0xFF) == 0xFF;
        }

        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        if (data.length < png.length) {
            return false;
        }

        for (int i = 0; i < png.length; i++) {
            if (data[i] != png[i]) {
                return false;
            }
        }

        return true;
    }
}
