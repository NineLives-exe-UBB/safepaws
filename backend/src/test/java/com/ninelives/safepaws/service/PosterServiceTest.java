package com.ninelives.safepaws.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Photo;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.PhotoMetadata;
import com.ninelives.safepaws.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosterServiceTest {

    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 11, 1, 9, 15);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Spy
    private QrCodeService qrCodeService =
            new QrCodeService("http://localhost:4200");

    @InjectMocks
    private PosterService posterService;

    private static Announcement storedAnnouncement(Long id, AnnouncementType type, Species species, String petName) {
        Announcement announcement = new Announcement();
        ReflectionTestUtils.setField(announcement, "id", id);
        ReflectionTestUtils.setField(announcement, "createdAt", CREATED_AT);
        announcement.setType(type);
        announcement.setStatus(AnnouncementStatus.ACTIVE);
        announcement.setSpecies(species);
        announcement.setPetName(petName);
        announcement.setBreed("Golden Retriever");
        announcement.setColor("Golden");
        announcement.setDistinctiveMarks("Red nylon collar with bell");
        announcement.setEventDate(EVENT_DATE);
        announcement.setLatitude(46.7712);
        announcement.setLongitude(23.6236);
        announcement.setArea("Mănăștur, Cluj-Napoca");
        announcement.setContactName("Ion Radu");
        announcement.setContactPhone("0740123456");
        return announcement;
    }

    @Test
    void generatePosterIncludesEveryAnnouncementDetailMainPhotoAndQrSection() throws Exception {
        Announcement announcement = storedAnnouncement(1L, AnnouncementType.LOST, Species.DOG, "Oscar");

        BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
        ImageIO.write(img, "png", imgOut);

        Photo photoEntity = mock(Photo.class);
        when(photoEntity.getData()).thenReturn(imgOut.toByteArray());

        PhotoMetadata metadata = new PhotoMetadata(10L, 1L, "image/png", 500L, "oscar.png", true, CREATED_AT);

        when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement));
        when(photoRepository.findMetadataByAnnouncementIds(List.of(1L))).thenReturn(List.of(metadata));
        when(photoRepository.findById(10L)).thenReturn(Optional.of(photoEntity));

        byte[] pdfBytes = posterService.generatePoster(1L);

        assertThat(pdfBytes).isNotEmpty();
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text)
                    .contains("LOST")
                    .contains("Oscar")
                    .contains("Dog")
                    .doesNotContain("DOG")
                    .contains("Golden Retriever")
                    .contains("Golden")
                    .contains("Red nylon collar with bell")
                    .contains("2026-11-01 09:15")
                    .contains("Mănăștur, Cluj-Napoca")
                    .contains("IF SEEN, PLEASE CONTACT:")
                    .contains("Ion Radu")
                    .contains("0740123456")
                    .doesNotContain("Ion Radu - ")
                    .contains("Scan for updates")
                    .doesNotContain("QR CODE")
                    .doesNotContain("No photo available")
                    .doesNotContain("RESOLVED");
        }
    }

    @Test
    void generatePosterWithoutPhotoHidesMissingFieldsAndShowsPhotoPlaceholder() throws Exception {
        Announcement announcement = storedAnnouncement(2L, AnnouncementType.FOUND, Species.CAT, null);
        announcement.setBreed("   ");
        announcement.setDistinctiveMarks(null);
        announcement.setContactName("");
        announcement.setArea("Florești, Cluj");

        when(announcementRepository.findById(2L)).thenReturn(Optional.of(announcement));
        when(photoRepository.findMetadataByAnnouncementIds(List.of(2L))).thenReturn(List.of());

        byte[] pdfBytes = posterService.generatePoster(2L);

        assertThat(pdfBytes).isNotEmpty();
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text)
                    .contains("IS THIS YOUR PET? CONTACT:")
                    .doesNotContain("IF SEEN")
                    .contains("Cat")
                    .doesNotContain("CAT")
                    .contains("No photo available")
                    .contains("Florești, Cluj")
                    .contains("0740123456")
                    .contains("Scan for updates")
                    .doesNotContain("NAME:")
                    .doesNotContain("BREED:")
                    .doesNotContain("MARKS:")
                    .doesNotContain("N/A")
                    .doesNotContain("Unknown / Mixed")
                    .doesNotContain("None specified");
        }
    }

    @Test
    void generatePosterShowsResolvedMarkAndFitsLongTextOnSinglePage() throws Exception {
        Announcement announcement = storedAnnouncement(3L, AnnouncementType.LOST, Species.DOG, "Rex");
        announcement.setStatus(AnnouncementStatus.RESOLVED);
        announcement.setArea("Bartolomeu, Brașov");
        announcement.setDistinctiveMarks("Very long distinctive marks description ".repeat(10));

        when(announcementRepository.findById(3L)).thenReturn(Optional.of(announcement));
        when(photoRepository.findMetadataByAnnouncementIds(List.of(3L))).thenReturn(List.of());

        byte[] pdfBytes = posterService.generatePoster(3L);

        assertThat(pdfBytes).isNotEmpty();
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text)
                    .contains("LOST")
                    .contains("RESOLVED")
                    .contains("Bartolomeu, Brașov")
                    .contains("...");
        }
    }

    @Test
    void generatePosterThrowsWhenTheAnnouncementDoesNotExist() {
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> posterService.generatePoster(999L))
                .isInstanceOf(AnnouncementNotFoundException.class)
                .hasMessage("Announcement not found with id: 999");
    }
}