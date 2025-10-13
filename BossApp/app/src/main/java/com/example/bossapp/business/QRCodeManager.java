package com.example.bossapp.business;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeManager {

    private static final int QR_CODE_SIZE = 512; // Size in pixels

    /**
     * Generates a QR code bitmap from user ID
     * @param userId The user's Firebase UID
     * @return Bitmap of the QR code
     */
    public static Bitmap generateQRCode(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        try {
            // Create QR code content - we'll use a custom format
            // Format: "BOSSAPP:USER:{userId}"
            String qrContent = "BOSSAPP:USER:" + userId;

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE
            );

            // Convert BitMatrix to Bitmap
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts user ID from scanned QR code content
     * @param qrContent The raw content from QR scan
     * @return User ID if valid format, null otherwise
     */
    public static String extractUserIdFromQR(String qrContent) {
        if (qrContent == null || !qrContent.startsWith("BOSSAPP:USER:")) {
            return null;
        }

        return qrContent.replace("BOSSAPP:USER:", "");
    }

    /**
     * Validates if scanned QR code is a valid BossApp user QR
     * @param qrContent The raw content from QR scan
     * @return true if valid BossApp user QR code
     */
    public static boolean isValidUserQR(String qrContent) {
        return qrContent != null &&
                qrContent.startsWith("BOSSAPP:USER:") &&
                qrContent.length() > "BOSSAPP:USER:".length();
    }
}