package com.yunfie.illustia.data

import android.graphics.Bitmap
import android.graphics.Color
import com.yunfie.illustia.rust.analyzeRgba
import kotlin.math.roundToInt

internal object NativeImageAnalysis {
    private const val HEADER_SAMPLE_SIZE = 40
    private const val COLOR_SAMPLE_SIZE = 32

    fun shouldUseDarkHeaderIcons(bitmap: Bitmap): Boolean {
        val sample = sampledPixels(bitmap, HEADER_SAMPLE_SIZE)
        val insetX = (sample.width / 6).coerceAtLeast(1)
        val insetY = (sample.height / 6).coerceAtLeast(1)
        val rgba = pixelsToRgba(sample.pixels) { index ->
            val x = index % sample.width
            val y = index / sample.width
            y < insetY || y >= sample.height - insetY ||
                x < insetX || x >= sample.width - insetX
        }
        val analysis = analyzeRgba(rgba)
        if (analysis.sampleCount == 0u) {
            return analyzeRgba(pixelsToRgba(sample.pixels)).averageLuminance >= 0.58
        }
        return analysis.averageLuminance >= 0.58
    }

    fun dominantColor(bitmap: Bitmap): Int {
        val sample = sampledPixels(bitmap, COLOR_SAMPLE_SIZE)
        return analyzeRgba(pixelsToRgba(sample.pixels)).dominantArgb
    }

    private fun sampledPixels(bitmap: Bitmap, maxDimension: Int): PixelSample {
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            return PixelSample(0, 0, IntArray(0))
        }
        val scale = minOf(
            1f,
            maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat(),
        )
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val sampled = if (width == bitmap.width && height == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        return try {
            val pixels = IntArray(width * height)
            sampled.getPixels(pixels, 0, width, 0, 0, width, height)
            PixelSample(width, height, pixels)
        } finally {
            if (sampled !== bitmap) sampled.recycle()
        }
    }

    private fun pixelsToRgba(
        pixels: IntArray,
        include: (Int) -> Boolean = { true },
    ): ByteArray {
        val included = pixels.indices.count(include)
        val rgba = ByteArray(included * 4)
        var offset = 0
        pixels.forEachIndexed { index, color ->
            if (!include(index)) return@forEachIndexed
            rgba[offset++] = Color.red(color).toByte()
            rgba[offset++] = Color.green(color).toByte()
            rgba[offset++] = Color.blue(color).toByte()
            rgba[offset++] = Color.alpha(color).toByte()
        }
        return rgba
    }

    private data class PixelSample(
        val width: Int,
        val height: Int,
        val pixels: IntArray,
    )
}
