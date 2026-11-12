package com.hashcore.pool.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder

object TotpQr {
    /**
     * Standard otpauth URI for Google Authenticator / Authy.
     * @param accountLabel email or wallet short id shown in the authenticator app
     */
    fun otpAuthUri(secret: String, accountLabel: String = "miner", issuer: String = "P2PMine"): String {
        val cleanSecret = secret.replace(" ", "").uppercase()
        val label = URLEncoder.encode("$issuer:$accountLabel", "UTF-8")
        val iss = URLEncoder.encode(issuer, "UTF-8")
        return "otpauth://totp/$label?secret=$cleanSecret&issuer=$iss&algorithm=SHA1&digits=6&period=30"
    }

    fun encodeBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }
}
