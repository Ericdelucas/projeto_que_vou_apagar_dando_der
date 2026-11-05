package com.example.app.users
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

object PhotoEncoder {
    fun encode(context: android.content.Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            require(options.outWidth > 0 && options.outHeight > 0)
            options.inJustDecodeBounds = false
            options.inSampleSize = 1
            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: error("Foto inválida")
        }
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output))
                require(output.size() <= 8 * 1024 * 1024)
                output.toByteArray()
            }
        } finally { bitmap.recycle() }
    }
}
