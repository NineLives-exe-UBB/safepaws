package com.ninelives.safepaws.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws a one-page A4 poster in the style of the web app: the type and status as large badges,
 * a friendly question, a large photo, the details in two columns and the contact with the QR code
 * at the bottom of the page.
 */
@Service
public class PosterService {

    /** The same format as the web app, e.g. "30 Oct 2026, 18:00". */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final String BOLD_FONT_PATH = "/fonts/DejaVuSans-Bold.ttf";
    private static final String REGULAR_FONT_PATH = "/fonts/DejaVuSans.ttf";

    private static final int MAX_NAME_LENGTH = 30;
    private static final int MAX_FIELD_LENGTH = 70;
    private static final int MAX_MARKS_LENGTH = 140;

    // Layout, in points (A4 is 595 x 842)
    private static final float MARGIN = 40f;
    private static final float BADGE_HEIGHT = 60f;
    private static final float BADGE_FONT_SIZE = 38f;
    private static final float BADGE_GAP = 12f;
    private static final float HEADLINE_SIZE = 30f;
    private static final float SUBLINE_SIZE = 14f;
    private static final float TITLE_GAP = 16f;
    private static final float LINE_SPACING = 1.2f;
    private static final float GAP = 18f;
    private static final float PHOTO_RADIUS = 14f;
    private static final float MAX_PHOTO_HEIGHT = 300f;
    private static final float MIN_PHOTO_HEIGHT = 60f;
    private static final float MAX_PLACEHOLDER_HEIGHT = 170f;
    private static final float DETAIL_ROW_GAP = 14f;
    private static final float CARD_HEIGHT = 150f;
    private static final float CARD_RADIUS = 16f;
    private static final float CARD_PADDING = 24f;
    private static final float QR_SIZE = 100f;
    private static final float MAX_PHONE_SIZE = 34f;

    // The colors of the web app (frontend/src/styles.css)
    private static final Color PRIMARY = new Color(47, 93, 98);
    private static final Color PRIMARY_LIGHT = new Color(238, 245, 244);
    private static final Color LOST = new Color(192, 57, 43);
    private static final Color FOUND = new Color(46, 139, 87);
    private static final Color TEXT = new Color(34, 34, 34);
    private static final Color TEXT_MUTED = new Color(85, 85, 85);
    private static final Color TEXT_SUBTLE = new Color(107, 119, 119);
    private static final Color SURFACE_MUTED = new Color(228, 233, 233);

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

    /** One label and its value in the details section. */
    private record Detail(String label, String value) {
    }

    @Transactional(readOnly = true)
    public byte[] generatePoster(Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException(announcementId));

        byte[] mainPhotoBytes = findMainPhoto(announcementId);
        byte[] qrCodeBytes = qrCodeService.generateQrCode(announcementId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            Poster poster = new Poster(
                    writer.getDirectContent(),
                    loadFont(BOLD_FONT_PATH, BaseFont.HELVETICA_BOLD),
                    loadFont(REGULAR_FONT_PATH, BaseFont.HELVETICA));
            poster.draw(announcement, mainPhotoBytes, qrCodeBytes);

            writer.setPageEmpty(false);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF poster for announcement " + announcementId, e);
        }
    }

    private byte[] findMainPhoto(Long announcementId) {
        List<PhotoMetadata> metadataList = photoRepository.findMetadataByAnnouncementIds(List.of(announcementId));
        if (metadataList.isEmpty()) {
            return null;
        }

        PhotoMetadata mainPhoto = metadataList.stream()
                .filter(PhotoMetadata::isMain)
                .findFirst()
                .orElse(metadataList.getFirst());

        return photoRepository.findById(mainPhoto.id())
                .map(Photo::getData)
                .orElse(null);
    }

    /** Loads a DejaVu font (it has the Romanian letters), or a standard PDF font if it is missing. */
    private BaseFont loadFont(String resourcePath, String fallbackFont) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                String name = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, is.readAllBytes(), null);
            }
        } catch (Exception ignored) {
            // Fall back to the standard font below
        }
        try {
            return BaseFont.createFont(fallbackFont, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load fallback PDF font", e);
        }
    }

    /** Draws one poster from top to bottom. */
    private final class Poster {

        private final PdfContentByte canvas;
        private final BaseFont bold;
        private final BaseFont regular;
        private final float left = MARGIN;
        private final float right = PageSize.A4.getWidth() - MARGIN;
        private final float width = right - left;
        private final float centerX = PageSize.A4.getWidth() / 2;

        Poster(PdfContentByte canvas, BaseFont bold, BaseFont regular) {
            this.canvas = canvas;
            this.bold = bold;
            this.regular = regular;
        }

        void draw(Announcement announcement, byte[] photoBytes, byte[] qrCodeBytes) {
            boolean isLost = announcement.getType() == AnnouncementType.LOST;
            boolean isResolved = announcement.getStatus() == AnnouncementStatus.RESOLVED;
            String name = truncateText(announcement.getPetName(), MAX_NAME_LENGTH);
            String area = truncateText(announcement.getArea(), MAX_FIELD_LENGTH);

            String headline = headline(isLost, isResolved, name, announcement.getSpecies());
            String subline = (isLost ? "Lost " : "Found ") + speciesWord(announcement.getSpecies(), isLost)
                    + (area != null && !area.isEmpty() ? "  ·  " + area : "");
            List<Detail> details = details(announcement, isLost);

            List<String> headlineLines = wrap(headline, bold, HEADLINE_SIZE, width);
            List<String> sublineLines = wrap(subline, regular, SUBLINE_SIZE, width);

            // Everything except the photo has a known height, so the photo gets the space that is left.
            float top = PageSize.A4.getHeight() - MARGIN;
            float cardBottom = MARGIN + 20f;
            float cardTop = cardBottom + CARD_HEIGHT;
            float titleHeight = BADGE_HEIGHT
                    + TITLE_GAP + linesHeight(headlineLines, bold, HEADLINE_SIZE)
                    + TITLE_GAP + linesHeight(sublineLines, regular, SUBLINE_SIZE)
                    + descent(regular, SUBLINE_SIZE);
            float detailsHeight = drawDetails(details, 0, true);
            float photoSpace = top - titleHeight - GAP - GAP - detailsHeight - GAP - cardTop;
            float photoHeight = Math.max(MIN_PHOTO_HEIGHT, Math.min(MAX_PHOTO_HEIGHT, photoSpace));

            // The badge, the headline and the location line are the same distance (TITLE_GAP) apart.
            float y = top;
            drawBadges(isLost, isResolved, y);
            y -= BADGE_HEIGHT + TITLE_GAP;
            y = drawCenteredLines(headlineLines, bold, HEADLINE_SIZE, TEXT, y);
            y -= TITLE_GAP;
            y = drawCenteredLines(sublineLines, regular, SUBLINE_SIZE, TEXT_MUTED, y);
            y -= descent(regular, SUBLINE_SIZE) + GAP;
            drawPhotoOrPlaceholder(photoBytes, isResolved, y, photoHeight);
            y -= photoHeight + GAP;
            drawDetails(details, y, false);

            drawContactCard(announcement, isLost, name, cardBottom, qrCodeBytes);
        }

        /**
         * Draws centered lines whose capital letters start exactly at {@code visualTop}, and returns
         * the baseline of the last line, so the next text can keep the same visual distance.
         */
        private float drawCenteredLines(
                List<String> lines, BaseFont baseFont, float size, Color color, float visualTop) {
            float baseline = visualTop - capHeight(baseFont, size);
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    baseline -= size * LINE_SPACING;
                }
                ColumnText.showTextAligned(
                        canvas, Element.ALIGN_CENTER, new Phrase(lines.get(i), font(baseFont, size, color)),
                        centerX, baseline, 0);
            }
            return baseline;
        }

        /** From the top of the capital letters of the first line to the baseline of the last one. */
        private float linesHeight(List<String> lines, BaseFont baseFont, float size) {
            return capHeight(baseFont, size) + (lines.size() - 1) * size * LINE_SPACING;
        }

        /** Splits a text into lines that fit the given width, breaking between words. */
        private List<String> wrap(String text, BaseFont baseFont, float size, float maxWidth) {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && baseFont.getWidthPoint(candidate, size) > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
            return lines;
        }

        private float capHeight(BaseFont baseFont, float size) {
            return baseFont.getFontDescriptor(BaseFont.CAPHEIGHT, size);
        }

        /** How far letters like "g" or "p" go below the baseline. */
        private float descent(BaseFont baseFont, float size) {
            return -baseFont.getFontDescriptor(BaseFont.DESCENT, size);
        }

        /** "LOST" or "FOUND" in the color of the type, and "RESOLVED" in grey next to it. */
        private void drawBadges(boolean isLost, boolean isResolved, float top) {
            List<String> labels = new ArrayList<>(List.of(isLost ? "LOST" : "FOUND"));
            List<Color> colors = new ArrayList<>(List.of(isLost ? LOST : FOUND));
            if (isResolved) {
                labels.add("RESOLVED");
                colors.add(TEXT_SUBTLE);
            }

            float totalWidth = BADGE_GAP * (labels.size() - 1);
            for (String label : labels) {
                totalWidth += badgeWidth(label);
            }

            float x = centerX - totalWidth / 2;
            float bottom = top - BADGE_HEIGHT;
            for (int i = 0; i < labels.size(); i++) {
                float badgeWidth = badgeWidth(labels.get(i));
                fillRoundRect(x, bottom, badgeWidth, BADGE_HEIGHT, BADGE_HEIGHT / 2, colors.get(i));
                ColumnText.showTextAligned(
                        canvas, Element.ALIGN_CENTER,
                        new Phrase(labels.get(i), font(bold, BADGE_FONT_SIZE, Color.WHITE)),
                        x + badgeWidth / 2, bottom + (BADGE_HEIGHT - capHeight(bold, BADGE_FONT_SIZE)) / 2, 0);
                x += badgeWidth + BADGE_GAP;
            }
        }

        private float badgeWidth(String label) {
            return bold.getWidthPoint(label, BADGE_FONT_SIZE) + 48f;
        }

        private void drawPhotoOrPlaceholder(byte[] photoBytes, boolean isResolved, float top, float maxHeight) {
            Image image = loadPhoto(photoBytes, isResolved);
            if (image != null) {
                float scale = Math.min(width / image.getWidth(), maxHeight / image.getHeight());
                float photoWidth = image.getWidth() * scale;
                float photoHeight = image.getHeight() * scale;
                float x = centerX - photoWidth / 2;
                float y = top - photoHeight - (maxHeight - photoHeight) / 2;

                canvas.saveState();
                canvas.roundRectangle(x, y, photoWidth, photoHeight, PHOTO_RADIUS);
                canvas.clip();
                canvas.newPath();
                canvas.addImage(image, photoWidth, 0, 0, photoHeight, x, y);
                canvas.restoreState();
                return;
            }

            float boxHeight = Math.min(maxHeight, MAX_PLACEHOLDER_HEIGHT);
            float boxWidth = width * 0.75f;
            float y = top - maxHeight / 2 - boxHeight / 2;
            fillRoundRect(centerX - boxWidth / 2, y, boxWidth, boxHeight, PHOTO_RADIUS, SURFACE_MUTED);
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_CENTER,
                    new Phrase("No photo available", font(regular, 16f, TEXT_SUBTLE)),
                    centerX, y + boxHeight / 2 - 6f, 0);
        }

        /** A resolved announcement gets a grey photo, like its card in the app. */
        private Image loadPhoto(byte[] photoBytes, boolean grey) {
            if (photoBytes == null || photoBytes.length == 0) {
                return null;
            }
            try {
                if (grey) {
                    BufferedImage original = ImageIO.read(new ByteArrayInputStream(photoBytes));
                    if (original != null) {
                        BufferedImage greyImage = new BufferedImage(
                                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                        Graphics2D graphics = greyImage.createGraphics();
                        graphics.drawImage(original, 0, 0, null);
                        graphics.dispose();
                        return Image.getInstance(greyImage, null);
                    }
                }
                return Image.getInstance(photoBytes);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Draws the details two per row (the distinctive marks get a whole row) and returns their
         * height. With {@code measureOnly} nothing is drawn.
         */
        private float drawDetails(List<Detail> details, float top, boolean measureOnly) {
            float columnWidth = (width - 24f) / 2;
            float y = top;
            int i = 0;
            while (i < details.size()) {
                Detail first = details.get(i);
                boolean fullWidth = first.label().equals("DISTINCTIVE MARKS");
                if (fullWidth) {
                    y -= drawDetail(first, left, right, y, measureOnly) + DETAIL_ROW_GAP;
                    i++;
                    continue;
                }

                float rowHeight = drawDetail(first, left, left + columnWidth, y, measureOnly);
                if (i + 1 < details.size() && !details.get(i + 1).label().equals("DISTINCTIVE MARKS")) {
                    Detail second = details.get(i + 1);
                    rowHeight = Math.max(rowHeight, drawDetail(second, right - columnWidth, right, y, measureOnly));
                    i++;
                }
                y -= rowHeight + DETAIL_ROW_GAP;
                i++;
            }
            return top - y;
        }

        private float drawDetail(Detail detail, float x1, float x2, float top, boolean measureOnly) {
            Font labelFont = font(regular, 9.5f, TEXT_SUBTLE);
            Font valueFont = font(bold, 14f, TEXT);
            if (measureOnly) {
                return measure(detail.label(), labelFont, 12f, x2 - x1) + 2f
                        + measure(detail.value(), valueFont, 18f, x2 - x1);
            }
            float height = drawText(detail.label(), labelFont, 12f, Element.ALIGN_LEFT, x1, x2, top) + 2f;
            return height + drawText(detail.value(), valueFont, 18f, Element.ALIGN_LEFT, x1, x2, top - height);
        }

        /** The light card at the bottom: who to call on the left, the QR code on the right. */
        private void drawContactCard(
                Announcement announcement, boolean isLost, String petName, float bottom, byte[] qrCodeBytes) {

            fillRoundRect(left, bottom, width, CARD_HEIGHT, CARD_RADIUS, PRIMARY_LIGHT);

            float qrBoxSize = QR_SIZE + 20f;
            float qrBoxX = right - CARD_PADDING - qrBoxSize;
            float qrBoxY = bottom + (CARD_HEIGHT - qrBoxSize - 16f) / 2;
            fillRoundRect(qrBoxX, qrBoxY, qrBoxSize, qrBoxSize + 16f, 10f, Color.WHITE);
            drawQrCode(qrCodeBytes, qrBoxX + 10f, qrBoxY + 26f);
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_CENTER, new Phrase("Scan for updates", font(bold, 9f, PRIMARY)),
                    qrBoxX + qrBoxSize / 2, qrBoxY + 10f, 0);

            float textLeft = left + CARD_PADDING;
            float textRight = qrBoxX - 16f;
            String species = speciesWord(announcement.getSpecies(), isLost);
            String callToAction = isLost
                    ? "If you see " + (petName != null && !petName.isEmpty() ? petName : "this " + species)
                    + ", please call"
                    : "Recognize this " + species + "? Please call";
            String contactName = truncateText(announcement.getContactName(), MAX_NAME_LENGTH);
            String phone = announcement.getContactPhone() != null ? announcement.getContactPhone().trim() : "";

            float y = bottom + CARD_HEIGHT - 26f;
            Font callToActionFont = font(regular, 13f, TEXT_MUTED);
            y -= drawText(callToAction, callToActionFont, 16f, Element.ALIGN_LEFT, textLeft, textRight, y);
            if (contactName != null && !contactName.isEmpty()) {
                y -= 4f;
                y -= drawText(contactName, font(bold, 18f, TEXT), 22f, Element.ALIGN_LEFT, textLeft, textRight, y);
            }
            if (!phone.isEmpty()) {
                // A long number gets a smaller font, so it always stays on one line.
                float phoneSize = Math.min(MAX_PHONE_SIZE, (textRight - textLeft) / bold.getWidthPoint(phone, 1f));
                ColumnText.showTextAligned(
                        canvas, Element.ALIGN_LEFT, new Phrase(phone, font(bold, phoneSize, PRIMARY)),
                        textLeft, y - phoneSize - 4f, 0);
            }
        }

        private void drawQrCode(byte[] qrCodeBytes, float x, float y) {
            if (qrCodeBytes != null && qrCodeBytes.length > 0) {
                try {
                    canvas.addImage(Image.getInstance(qrCodeBytes), QR_SIZE, 0, 0, QR_SIZE, x, y);
                    return;
                } catch (Exception ignored) {
                    // Fall back to a text placeholder
                }
            }
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_CENTER, new Phrase("QR CODE", font(regular, 11f, TEXT_SUBTLE)),
                    x + QR_SIZE / 2, y + QR_SIZE / 2, 0);
        }

        /** Draws text that wraps inside [x1, x2] starting at {@code top} and returns its height. */
        private float drawText(String text, Font font, float leading, int alignment, float x1, float x2, float top) {
            ColumnText column = new ColumnText(canvas);
            column.setSimpleColumn(new Phrase(text, font), x1, 0, x2, top, leading, alignment);
            column.go();
            return top - column.getYLine();
        }

        /** The height {@link #drawText} would use, without drawing anything. */
        private float measure(String text, Font font, float leading, float columnWidth) {
            ColumnText column = new ColumnText(canvas);
            float top = 10_000f;
            column.setSimpleColumn(new Phrase(text, font), 0, 0, columnWidth, top, leading, Element.ALIGN_LEFT);
            column.go(true);
            return top - column.getYLine();
        }

        private void fillRoundRect(float x, float y, float rectWidth, float rectHeight, float radius, Color color) {
            canvas.saveState();
            canvas.setColorFill(color);
            canvas.roundRectangle(x, y, rectWidth, rectHeight, radius);
            canvas.fill();
            canvas.restoreState();
        }
    }

    private static String headline(boolean isLost, boolean isResolved, String name, Species species) {
        boolean hasName = name != null && !name.isEmpty();
        String animal = speciesWord(species, isLost);
        if (isResolved) {
            if (!isLost) {
                return "Back with its family!";
            }
            return (hasName ? name : "This " + animal) + " is back home!";
        }
        if (isLost) {
            return "Have you seen " + (hasName ? name : "this " + animal) + "?";
        }
        return "Is this your " + animal + "?";
    }

    private static List<Detail> details(Announcement announcement, boolean isLost) {
        String date = announcement.getEventDate() != null
                ? announcement.getEventDate().format(DATE_FORMATTER)
                : null;

        List<Detail> details = new ArrayList<>();
        addIfPresent(details, "BREED", truncateText(announcement.getBreed(), MAX_FIELD_LENGTH));
        addIfPresent(details, "COLOR", truncateText(announcement.getColor(), MAX_FIELD_LENGTH));
        addIfPresent(details, isLost ? "LOST ON" : "FOUND ON", date);
        addIfPresent(details, "AREA", truncateText(announcement.getArea(), MAX_FIELD_LENGTH));
        addIfPresent(details, "DISTINCTIVE MARKS", truncateText(announcement.getDistinctiveMarks(), MAX_MARKS_LENGTH));
        return details;
    }

    private static void addIfPresent(List<Detail> details, String label, String value) {
        if (value != null && !value.isBlank()) {
            details.add(new Detail(label, value));
        }
    }

    /** "cat" or "dog"; for other animals "pet" when lost and "animal" when found, as in the app. */
    private static String speciesWord(Species species, boolean isLost) {
        if (species == Species.CAT) {
            return "cat";
        }
        if (species == Species.DOG) {
            return "dog";
        }
        return isLost ? "pet" : "animal";
    }

    private static Font font(BaseFont baseFont, float size, Color color) {
        return new Font(baseFont, size, Font.NORMAL, color);
    }

    private static String truncateText(String value, int maxLength) {
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