
package com.ninelives.safepaws.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {

    @Test
    void generatesQrCodeWithCorrectAnnouncementUrl() throws Exception {
        QrCodeService service = new QrCodeService("http://localhost:4200");

        byte[] qrBytes = service.generateQrCode(13L);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrBytes));

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(300);

        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(
                        new BufferedImageLuminanceSource(image)
                )
        );

        String decodedUrl = new QRCodeReader().decode(bitmap).getText();

        assertThat(decodedUrl)
                .isEqualTo("http://localhost:4200/announcements/13");
    }

    @Test
    void usesConfiguredFrontendUrl() throws Exception {
        QrCodeService service = new QrCodeService("https://safepaws.example.com/");

        byte[] qrBytes = service.generateQrCode(25L);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrBytes));

        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(
                        new BufferedImageLuminanceSource(image)
                )
        );

        String decodedUrl = new QRCodeReader().decode(bitmap).getText();

        assertThat(decodedUrl)
                .isEqualTo("https://safepaws.example.com/announcements/25");
    }
}
