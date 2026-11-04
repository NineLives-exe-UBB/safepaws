package com.ninelives.safepaws.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Photo;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.PhotoMetadata;
import com.ninelives.safepaws.repository.PhotoRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PosterService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String FONT_PATH = "fonts/DejaVuSans-Bold.ttf";

    private final AnnouncementRepository announcementRepository;
    private final PhotoRepository photoRepository;
    private final BaseFont baseFont;

    public PosterService(AnnouncementRepository announcementRepository, PhotoRepository photoRepository) {
        this.announcementRepository = announcementRepository;
        this.photoRepository = photoRepository;
        this.baseFont = loadFont();
    }

    private static BaseFont loadFont() {
        try (InputStream in = new ClassPathResource(FONT_PATH).getInputStream()) {
            byte[] fontBytes = in.readAllBytes();
            return BaseFont.createFont("DejaVuSans-Bold.ttf", BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, fontBytes, null);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load poster font " + FONT_PATH, e);
        }
    }

    private Font font(float size, Color color) {
        return new Font(baseFont, size, Font.NORMAL, color);
    }

    @Transactional(readOnly = true)
    public byte[] generatePoster(Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException(announcementId));

        List<PhotoMetadata> metadataList = photoRepository.findMetadataByAnnouncementIds(List.of(announcementId));

        byte[] mainPhotoBytes = null;
        if (!metadataList.isEmpty()) {
            PhotoMetadata targetPhoto = metadataList.stream()
                    .filter(PhotoMetadata::isMain)
                    .findFirst()
                    .orElse(metadataList.getFirst());

            mainPhotoBytes = photoRepository.findById(targetPhoto.id())
                    .map(Photo::getData)
                    .orElse(null);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            addBannerTitle(document, announcement.getType());
            addMainPhoto(document, mainPhotoBytes);
            addDetailsTable(document, announcement);
            addContactBanner(document, announcement);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF poster for announcement " + announcementId, e);
        }
    }

    private void addBannerTitle(Document document, AnnouncementType type) {
        boolean isLost = type == AnnouncementType.LOST;
        String titleText = isLost ? "LOST" : "FOUND";
        Color bannerColor = isLost ? new Color(211, 47, 47) : new Color(46, 125, 50);

        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(18f);

        Font titleFont = font(64, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(titleText, titleFont));
        cell.setBackgroundColor(bannerColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(14f);
        cell.setPaddingBottom(20f);
        cell.setBorder(Rectangle.NO_BORDER);

        banner.addCell(cell);
        document.add(banner);
    }

    private void addMainPhoto(Document document, byte[] photoBytes) {
        if (photoBytes == null || photoBytes.length == 0) {
            return;
        }
        try {
            Image image = Image.getInstance(photoBytes);
            image.scaleToFit(420f, 260f);
            image.setAlignment(Element.ALIGN_CENTER);
            image.setSpacingAfter(18f);
            document.add(image);
        } catch (Exception ignored) {
            // Skip rendering if photo bytes are invalid
        }
    }

    private void addDetailsTable(Document document, Announcement announcement) {
        PdfPTable table = new PdfPTable(new float[]{1.2f, 2.3f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(22f);

        if (announcement.getPetName() != null && !announcement.getPetName().isBlank()) {
            addDetailRow(table, "NAME:", announcement.getPetName());
        }
        String species = announcement.getSpecies() != null ? announcement.getSpecies().name() : "N/A";
        addDetailRow(table, "SPECIES:", species);
        addDetailRow(table, "BREED:", valueOrDefault(announcement.getBreed(), "Unknown / Mixed"));
        addDetailRow(table, "COLOR:", valueOrDefault(announcement.getColor(), "N/A"));
        addDetailRow(table, "MARKS:", valueOrDefault(announcement.getDistinctiveMarks(), "None specified"));
        String dateText = announcement.getEventDate() != null
                ? announcement.getEventDate().format(DATE_FORMATTER)
                : "N/A";
        addDetailRow(table, "DATE:", dateText);
        addDetailRow(table, "AREA:", valueOrDefault(announcement.getArea(), "N/A"));

        document.add(table);
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        Font labelFont = font(18, Color.DARK_GRAY);
        Font valueFont = font(20, Color.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        labelCell.setPaddingTop(8f);
        labelCell.setPaddingBottom(8f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        valueCell.setPaddingTop(8f);
        valueCell.setPaddingBottom(8f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addContactBanner(Document document, Announcement announcement) {
        PdfPTable contactTable = new PdfPTable(1);
        contactTable.setWidthPercentage(100);

        Font headerFont = font(18, Color.WHITE);
        Font contactFont = font(26, Color.YELLOW);

        String name = valueOrDefault(announcement.getContactName(), "");
        String phone = valueOrDefault(announcement.getContactPhone(), "N/A");
        String contactDetails = name.isEmpty() ? phone : name + " - " + phone;

        Paragraph p = new Paragraph();
        p.add(new Chunk("IF SEEN, PLEASE CONTACT:\n", headerFont));
        p.add(new Chunk(contactDetails, contactFont));

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(new Color(33, 33, 33));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(14f);
        cell.setPaddingBottom(16f);
        cell.setBorder(Rectangle.NO_BORDER);

        contactTable.addCell(cell);
        document.add(contactTable);
    }

    private String valueOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}