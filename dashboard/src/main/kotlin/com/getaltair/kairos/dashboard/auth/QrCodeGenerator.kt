package com.getaltair.kairos.dashboard.auth

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage

private const val QR_SIZE = 400
private const val QUIET_ZONE = 2

/**
 * Builds the deep-link URI that the mobile app scans to link the dashboard.
 */
fun buildQrDataString(host: String, port: Int, sessionToken: String): String =
    "kairos://link-dashboard?host=$host&port=$port&session=$sessionToken"

/**
 * Generates a QR code as a [BufferedImage] with inverted colours (white
 * modules on a dark background) to match the kiosk dark theme.
 *
 * @param data the payload to encode
 * @param size pixel dimensions of the output image (square)
 * @return a [BufferedImage] of the QR code
 */
fun generateQrImage(data: String, size: Int = QR_SIZE): BufferedImage {
    val hints = mapOf(
        EncodeHintType.MARGIN to QUIET_ZONE,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    )
    val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)

    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    // Dark background colour matching the dashboard theme Background (#0C0E10)
    val darkColor = 0xFF0C0E10.toInt()
    // Light module colour matching Primary teal (#8EF4E9)
    val lightColor = 0xFF8EF4E9.toInt()

    for (x in 0 until size) {
        for (y in 0 until size) {
            // ZXing matrix: true = black module; we invert for dark theme
            image.setRGB(x, y, if (matrix.get(x, y)) lightColor else darkColor)
        }
    }
    return image
}

/**
 * Converts a [BufferedImage] to a Compose Desktop [ImageBitmap] via the
 * Skia bridge provided by JetBrains Compose.
 */
fun BufferedImage.toComposeImage(): ImageBitmap = toComposeImageBitmap()

/**
 * Convenience: generates a QR code and returns it as a Compose [ImageBitmap].
 */
fun generateQrBitmap(data: String, size: Int = QR_SIZE): ImageBitmap = generateQrImage(data, size).toComposeImage()
