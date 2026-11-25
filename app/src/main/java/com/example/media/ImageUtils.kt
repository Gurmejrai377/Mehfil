package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
    suspend fun encodeUriToCompactBase64(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val maxDim = 280
                val scale = maxDim.toFloat() / max(original.width, original.height).coerceAtLeast(1)
                val targetW = if (scale < 1f) (original.width * scale).toInt().coerceAtLeast(1) else original.width
                val targetH = if (scale < 1f) (original.height * scale).toInt().coerceAtLeast(1) else original.height
                val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
                val out = ByteArrayOutputStream()
                var quality = 70
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                while (out.size() > 55_000 && quality > 25) {
                    out.reset()
                    quality -= 15
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                if (scaled !== original) scaled.recycle()
                original.recycle()
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            }
        }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
