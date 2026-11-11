package com.ninelives.safepaws.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
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
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Photo;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.PhotoMetadata;
import com.ninelives.safepaws.repository.PhotoRepository;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosterService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String FONT_RESOURCE_PATH = "/fonts/DejaVuSans-Bold.ttf";
    private static final int MAX_MARKS_LENGTH = 140;
    private static final int MAX_FIELD_LENGTH = 70;


    private final AnnouncementRepository announcementRepository;
    private final PhotoRepository photoRepository;
    private final QrCodeService qrCodeService;

    public PosterService(
            AnnouncementRepository announcementRepository,
            PhotoRepository photoRepository,
            QrCodeService qrCodeService) {

        this.announcementRepository = announcementRepository;
        this.photoRepository = photoRepository;
        this.qrCodeService = qrCodeService;
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



        byte[] qrCodeBytes = qrCodeService.generateQrCode(announcementId);


        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 32, 32, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = loadUnicodeBaseFont();
            boolean isResolved = announcement.getStatus() == AnnouncementStatus.RESOLVED;

            addBannerTitle(document, announcement.getType(), isResolved, baseFont);
            if (isResolved) {
                addResolvedBanner(document, baseFont);
            }
            addPhotoOrPlaceholder(document, mainPhotoBytes, isResolved, baseFont);
            addDetailsTable(document, announcement, baseFont);
            addContactAndQrFooter(document, announcement, qrCodeBytes, baseFont);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF poster for announcement " + announcementId, e);
        }
    }

    private BaseFont loadUnicodeBaseFont() {
        try (InputStream is = getClass().getResourceAsStream(FONT_RESOURCE_PATH)) {
            if (is != null) {
                byte[] fontBytes = is.readAllBytes();
                return BaseFont.createFont(
                        "DejaVuSans-Bold.ttf",
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED,
                        true,
                        fontBytes,
                        null
                );
            }
        } catch (Exception ignored) {
            // Fall back to standard Helvetica if font resource is unavailable
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load fallback PDF font", e);
        }
    }

    private Font createFont(BaseFont baseFont, float size, Color color) {
        if (baseFont != null) {
            return new Font(baseFont, size, Font.NORMAL, color);
        }
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, size, color);
    }

    private void addBannerTitle(Document document, AnnouncementType type, boolean isResolved, BaseFont baseFont) {
        boolean isLost = type == AnnouncementType.LOST;
        String titleText = isLost ? "LOST" : "FOUND";
        Color bannerColor = isLost ? new Color(211, 47, 47) : new Color(46, 125, 50);

        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(isResolved ? 6f : 14f);

        Font titleFont = createFont(baseFont, 58f, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(titleText, titleFont));
        cell.setBackgroundColor(bannerColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(16f);
        cell.setBorder(Rectangle.NO_BORDER);

        banner.addCell(cell);
        document.add(banner);
    }

    private void addResolvedBanner(Document document, BaseFont baseFont) {
        PdfPTable resolvedTable = new PdfPTable(1);
        resolvedTable.setWidthPercentage(100);
        resolvedTable.setSpacingAfter(12f);

        Font resolvedFont = createFont(baseFont, 26f, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase("RESOLVED", resolvedFont));
        cell.setBackgroundColor(new Color(55, 71, 79));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(10f);
        cell.setBorder(Rectangle.NO_BORDER);

        resolvedTable.addCell(cell);
        document.add(resolvedTable);
    }

    private void addPhotoOrPlaceholder(Document document, byte[] photoBytes, boolean isResolved, BaseFont baseFont) {
        float maxHeight = isResolved ? 175f : 210f;
        if (photoBytes != null && photoBytes.length > 0) {
            try {
                Image image = Image.getInstance(photoBytes);
                image.scaleToFit(420f, maxHeight);
                image.setAlignment(Element.ALIGN_CENTER);
                image.setSpacingAfter(14f);
                document.add(image);
                return;
            } catch (Exception ignored) {
                // Fall through to neat placeholder if image bytes are invalid
            }
        }

        PdfPTable placeholder = new PdfPTable(1);
        placeholder.setWidthPercentage(80);
        placeholder.setSpacingAfter(14f);

        Font placeholderFont = createFont(baseFont, 20f, new Color(110, 110, 110));
        PdfPCell cell = new PdfPCell(new Phrase("No photo available", placeholderFont));
        cell.setFixedHeight(isResolved ? 150f : 180f);
        cell.setBackgroundColor(new Color(242, 242, 242));
        cell.setBorderColor(new Color(189, 189, 189));
        cell.setBorderWidth(1.5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        placeholder.addCell(cell);
        document.add(placeholder);
    }

    private void addDetailsTable(Document document, Announcement announcement, BaseFont baseFont) {
        PdfPTable table = new PdfPTable(new float[]{1.1f, 2.4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(16f);

        addDetailRowIfPresent(table, "NAME:", truncateText(announcement.getPetName(), MAX_FIELD_LENGTH), baseFont);
        addDetailRowIfPresent(table, "SPECIES:", formatSpecies(announcement.getSpecies()), baseFont);
        addDetailRowIfPresent(table, "BREED:", truncateText(announcement.getBreed(), MAX_FIELD_LENGTH), baseFont);
        addDetailRowIfPresent(table, "COLOR:", truncateText(announcement.getColor(), MAX_FIELD_LENGTH), baseFont);
        addDetailRowIfPresent(table, "MARKS:", truncateText(announcement.getDistinctiveMarks(), MAX_MARKS_LENGTH), baseFont);

        String dateText = announcement.getEventDate() != null
                ? announcement.getEventDate().format(DATE_FORMATTER)
                : null;
        addDetailRowIfPresent(table, "DATE:", dateText, baseFont);
        addDetailRowIfPresent(table, "AREA:", truncateText(announcement.getArea(), MAX_FIELD_LENGTH), baseFont);

        document.add(table);
    }

    private void addDetailRowIfPresent(PdfPTable table, String label, String value, BaseFont baseFont) {
        if (value == null || value.isBlank()) {
            return;
        }

        Font labelFont = createFont(baseFont, 16f, Color.DARK_GRAY);
        Font valueFont = createFont(baseFont, 18f, Color.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(Color.LIGHT_GRAY);
        labelCell.setPaddingTop(6f);
        labelCell.setPaddingBottom(7f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value.trim(), valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(Color.LIGHT_GRAY);
        valueCell.setPaddingTop(6f);
        valueCell.setPaddingBottom(7f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addContactAndQrFooter(Document document, Announcement announcement, byte[] qrCodeBytes, BaseFont baseFont) {
        PdfPTable footerTable = new PdfPTable(new float[]{2.5f, 1.1f});
        footerTable.setWidthPercentage(100);

        Font headerFont = createFont(baseFont, 16f, Color.WHITE);
        Font contactFont = createFont(baseFont, 22f, Color.YELLOW);

        String name = announcement.getContactName() != null ? announcement.getContactName().trim() : "";
        String phone = announcement.getContactPhone() != null ? announcement.getContactPhone().trim() : "";
        String contactDetails;
        if (!name.isEmpty() && !phone.isEmpty()) {
            contactDetails = name + " - " + phone;
        } else if (!phone.isEmpty()) {
            contactDetails = phone;
        } else {
            contactDetails = name;
        }

        Paragraph contactParagraph = new Paragraph();
        contactParagraph.setLeading(26f);
        contactParagraph.add(new Chunk("IF SEEN, PLEASE CONTACT:\n", headerFont));
        if (!contactDetails.isEmpty()) {
            contactParagraph.add(new Chunk(contactDetails, contactFont));
        }

        PdfPCell contactCell = new PdfPCell(contactParagraph);
        contactCell.setBackgroundColor(new Color(33, 33, 33));
        contactCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        contactCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        contactCell.setPadding(14f);
        contactCell.setBorder(Rectangle.NO_BORDER);

        PdfPCell qrContainerCell = createQrCodeCell(qrCodeBytes, baseFont);

        footerTable.addCell(contactCell);
        footerTable.addCell(qrContainerCell);
        document.add(footerTable);
    }

    private PdfPCell createQrCodeCell(byte[] qrCodeBytes, BaseFont baseFont) {
        PdfPTable qrInnerTable = new PdfPTable(1);
        qrInnerTable.setWidthPercentage(100);

        PdfPCell boxCell = new PdfPCell();
        boxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        boxCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        boxCell.setBorderColor(new Color(180, 180, 180));
        boxCell.setBorderWidth(1f);
        boxCell.setBackgroundColor(Color.WHITE);
        boxCell.setFixedHeight(84f);

        boolean qrAdded = false;
        if (qrCodeBytes != null && qrCodeBytes.length > 0) {
            try {
                Image qrImage = Image.getInstance(qrCodeBytes);
                qrImage.scaleToFit(76f, 76f);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                boxCell.addElement(qrImage);
                qrAdded = true;
            } catch (Exception ignored) {
                // Fall back to reserved QR box
            }
        }

        if (!qrAdded) {
            Font qrPlaceholderFont = createFont(baseFont, 11f, new Color(130, 130, 130));
            boxCell.setPhrase(new Phrase("QR CODE", qrPlaceholderFont));
        }

        Font scanCaptionFont = createFont(baseFont, 11f, Color.WHITE);
        PdfPCell captionCell = new PdfPCell(new Phrase("Scan for updates", scanCaptionFont));
        captionCell.setBackgroundColor(new Color(33, 33, 33));
        captionCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        captionCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        captionCell.setPaddingTop(5f);
        captionCell.setPaddingBottom(2f);
        captionCell.setBorder(Rectangle.NO_BORDER);

        qrInnerTable.addCell(boxCell);
        qrInnerTable.addCell(captionCell);

        PdfPCell wrapperCell = new PdfPCell(qrInnerTable);
        wrapperCell.setBackgroundColor(new Color(33, 33, 33));
        wrapperCell.setPadding(10f);
        wrapperCell.setBorder(Rectangle.LEFT);
        wrapperCell.setBorderColor(new Color(80, 80, 80));
        wrapperCell.setBorderWidth(1f);
        wrapperCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return wrapperCell;
    }

    private String formatSpecies(Species species) {
        if (species == null) {
            return null;
        }
        String raw = species.name();
        return raw.charAt(0) + raw.substring(1).toLowerCase(Locale.ROOT);
    }

    private String truncateText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 3).trim() + "...";
    }
}