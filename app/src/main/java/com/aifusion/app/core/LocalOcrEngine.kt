package com.aifusion.app.core

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import dev.ffmpegkit.tesseract.TesseractOCR

/**
 * Lightweight on-device OCR adapter. The bundled free model is English.
 */
object LocalOcrEngine {
    suspend fun recognize(context: Context, uri: Uri): String? {
        if (!Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }) return null
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        val ocr = TesseractOCR()
        return try {
            ocr.initialize(context, language = "eng")
            ocr.recognize(bitmap).text.trim().takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        } finally {
            ocr.release()
            bitmap.recycle()
        }
    }
}
