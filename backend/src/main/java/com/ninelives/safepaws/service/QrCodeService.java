
package com.ninelives.safepaws.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeService {

    private static final int QR_SIZE = 300;
    private final String frontendBaseUrl;

    public QrCodeService(
            @Value("${app.frontend.base-url:http://localhost:4200}")
            String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    public byte[] generateQrCode(Long announcementId) {
        String url = frontendBaseUrl + "/announcements/" + announcementId;

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            QRCodeWriter writer = new QRCodeWriter();

            BitMatrix matrix = writer.encode(
                    url,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE
            );

            MatrixToImageWriter.writeToStream(matrix, "PNG", output);

            return output.toByteArray();

        } catch (WriterException | IOException e) {
            throw new IllegalStateException(
                    "Failed to generate QR code for announcement " + announcementId, e
            );
        }
    }
}
