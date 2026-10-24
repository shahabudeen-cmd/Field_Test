package com.commissioning.cctv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object Photos {
    fun dir(ctx: Context): File = File(ctx.filesDir, "photos").also { it.mkdirs() }
    fun shotsDir(ctx: Context): File = File(ctx.cacheDir, "shots").also { it.mkdirs() }

    /** Decode, fix rotation, shrink to [maxSide] and write a JPEG to [dest]. Returns false if unreadable. */
    fun process(open: () -> InputStream?, dest: File, maxSide: Int = 1600): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = open()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return false
            val orientation = try {
                open()?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val m = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            }
            val s = min(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
            m.postScale(s, s)
            val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            FileOutputStream(dest).use { out.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Small decoded copy for on-screen display. */
    fun decode(file: File, target: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
