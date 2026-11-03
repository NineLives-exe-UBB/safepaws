
package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.PhotoResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.service.PhotoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhotoController.class)
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoService photoService;

    private MockMultipartFile jpgFile() {
        return new MockMultipartFile(
                "files", "cat.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile(
                "files", "dog.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );
    }

    private PhotoResponse response(Long id, boolean isMain) {
        return new PhotoResponse(
                id,
                13L,
                "image/jpeg",
                3L,
                "cat.jpg",
                isMain,
                "/photos/" + id,
                LocalDateTime.of(2026, 11, 3, 19, 0)
        );
    }

    @Test
    void uploadValidPhotoReturns201() throws Exception {
        when(photoService.upload(eq(13L), any(MultipartFile[].class)))
                .thenReturn(List.of(response(1L, true)));

        mockMvc.perform(multipart("/announcements/13/photos")
                        .file(jpgFile()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("/photos/1"))
                .andExpect(jsonPath("$[0].isMain").value(true));

        verify(photoService).upload(eq(13L), any(MultipartFile[].class));
    }

    @Test
    void uploadMultiplePhotosReturns201() throws Exception {
        when(photoService.upload(eq(13L), any(MultipartFile[].class)))
                .thenReturn(List.of(
                        response(1L, true),
                        response(2L, false)
                ));

        mockMvc.perform(multipart("/announcements/13/photos")
                        .file(jpgFile())
                        .file(pngFile()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isMain").value(true))
                .andExpect(jsonPath("$[1].isMain").value(false));

        verify(photoService).upload(
                eq(13L),
                argThat(files -> files.length == 2)
        );
    }

    @Test
    void uploadToNonexistentAnnouncementReturns404() throws Exception {
        when(photoService.upload(eq(999L), any(MultipartFile[].class)))
                .thenThrow(new AnnouncementNotFoundException(999L));

        mockMvc.perform(multipart("/announcements/999/photos")
                        .file(jpgFile()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void uploadInvalidFileReturns400() throws Exception {
        when(photoService.upload(eq(13L), any(MultipartFile[].class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only JPG and PNG images are allowed"
                ));

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.txt", "text/plain",
                "Hello".getBytes()
        );

        mockMvc.perform(multipart("/announcements/13/photos")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Only JPG and PNG images are allowed"));
    }

    @Test
    void uploadWithoutFilesReturns400() throws Exception {
        mockMvc.perform(multipart("/announcements/13/photos"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(photoService);
    }

    @Test
    void uploadWithJsonReturns415() throws Exception {
        mockMvc.perform(post("/announcements/13/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(photoService);
    }
}
