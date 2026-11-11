package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.PhotoResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.Photo;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final Long ANNOUNCEMENT_ID = 13L;

    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3
    };

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private PhotoService photoService;

    private void announcementExists() {
        when(announcementRepository.findById(ANNOUNCEMENT_ID))
                .thenReturn(Optional.of(new Announcement()));
    }

    private MockMultipartFile image(String name, String type, byte[] data) {
        return new MockMultipartFile("files", name, type, data);
    }

    private void mockSave() {
        AtomicLong nextId = new AtomicLong(1);

        when(photoRepository.saveAndFlush(any(Photo.class)))
                .thenAnswer(invocation -> {
                    Photo photo = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            photo, "id", nextId.getAndIncrement()
                    );
                    return photo;
                });
    }

    @Test
    void uploadValidJpg() {
        announcementExists();
        mockSave();

        MockMultipartFile file = image("cat.jpg", "image/jpeg", JPEG);

        List<PhotoResponse> result = photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).url()).isEqualTo("/photos/1");
        assertThat(result.get(0).isMain()).isTrue();

        verify(photoRepository).saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadValidPng() {
        announcementExists();
        mockSave();

        MockMultipartFile file = image("cat.png", "image/png", PNG);

        List<PhotoResponse> result = photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).contentType()).isEqualTo("image/png");
        assertThat(result.get(0).isMain()).isTrue();
    }

    @Test
    void uploadMultipleImages() {
        announcementExists();
        mockSave();

        MockMultipartFile file1 = image("cat.jpg", "image/jpeg", JPEG);
        MockMultipartFile file2 = image("dog.png", "image/png", PNG);

        List<PhotoResponse> result = photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file1, file2}
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(0).isMain()).isTrue();
        assertThat(result.get(1).isMain()).isFalse();

        verify(photoRepository, times(2))
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadWhenMainPhotoAlreadyExists() {
        announcementExists();
        mockSave();

        when(photoRepository.existsByAnnouncement_IdAndIsMainTrue(ANNOUNCEMENT_ID))
                .thenReturn(true);

        MockMultipartFile file = image("cat.jpg", "image/jpeg", JPEG);

        List<PhotoResponse> result = photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        );

        assertThat(result.get(0).isMain()).isFalse();
    }

    @Test
    void uploadToNonexistentAnnouncement() {
        when(announcementRepository.findById(ANNOUNCEMENT_ID))
                .thenReturn(Optional.empty());

        MockMultipartFile file = image("cat.jpg", "image/jpeg", JPEG);

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(AnnouncementNotFoundException.class)
                .hasMessageContaining("Announcement not found");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadNullFiles() {
        announcementExists();

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No files provided");
    }

    @Test
    void uploadNoFiles() {
        announcementExists();

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No files provided");
    }

    @Test
    void uploadEmptyFile() {
        announcementExists();

        MockMultipartFile file = image(
                "empty.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("File is empty");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadInvalidFileType() {
        announcementExists();

        MockMultipartFile file = image(
                "test.txt", "text/plain", "Hello".getBytes()
        );

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only JPG and PNG images are allowed");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadOversizedFile() {
        announcementExists();

        byte[] largeImage = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile file = image(
                "large.jpg", "image/jpeg", largeImage
        );

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Maximum file size is 5 MB");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadInvalidJpgContent() {
        announcementExists();

        MockMultipartFile file = image(
                "fake.jpg", "image/jpeg", "Not a JPG".getBytes()
        );

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid image file");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadInvalidPngContent() {
        announcementExists();

        MockMultipartFile file = image(
                "fake.png", "image/png", "Not a PNG".getBytes()
        );

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid image file");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadFileNameTooLong() {
        announcementExists();

        String longName = "a".repeat(252) + ".jpg";

        MockMultipartFile file = image(
                longName, "image/jpeg", JPEG
        );

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("File name is too long");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }

    @Test
    void uploadFileReadError() throws IOException {
        announcementExists();

        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenThrow(new IOException("Read failed"));

        assertThatThrownBy(() -> photoService.upload(
                ANNOUNCEMENT_ID, new MultipartFile[]{file}
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Could not read uploaded file");

        verify(photoRepository, never())
                .saveAndFlush(any(Photo.class));
    }


    @Test
    void getExistingPhoto() {
        Photo photo = new Photo(
                new Announcement(), JPEG, "image/jpeg", "cat.jpg", true
        );

        when(photoRepository.findById(1L))
                .thenReturn(Optional.of(photo));

        Photo result = photoService.getPhoto(1L);

        assertThat(result).isSameAs(photo);

        verify(photoRepository).findById(1L);
    }

    @Test
    void getNonexistentPhoto() {
        when(photoRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.getPhoto(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Photo not found with id: 999");

        verify(photoRepository).findById(999L);
    }
}
