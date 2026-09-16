package com.posepilot.app.photoanalysis

/**
 * Pure image statistics on an 8-bit grayscale buffer (row-major, width x height).
 */
object ImageMetrics {

    /**
     * Variance of the 4-neighbour Laplacian. Sharp images have strong edges -> high variance;
     * motion/focus blur smooths edges -> low variance. Thresholds depend on resolution, so callers
     * should downscale to a fixed width first (we use 480px).
     */
    fun laplacianVariance(gray: IntArray, width: Int, height: Int): Double {
        require(gray.size >= width * height)
        if (width < 3 || height < 3) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val lap = (gray[i - 1] + gray[i + 1] + gray[i - width] + gray[i + width] - 4 * gray[i]).toDouble()
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    data class Exposure(val mean: Double, val darkFraction: Double, val brightFraction: Double)

    fun exposure(gray: IntArray, width: Int, height: Int): Exposure {
        val total = width * height
        var sum = 0L; var dark = 0; var bright = 0
        for (i in 0 until total) {
            val v = gray[i]
            sum += v
            if (v < 25) dark++
            if (v > 235) bright++
        }
        return Exposure(sum.toDouble() / total, dark.toDouble() / total, bright.toDouble() / total)
    }

    /** ITU-R BT.601 luma from packed ARGB. */
    fun argbToGray(argb: IntArray): IntArray = IntArray(argb.size) { i ->
        val c = argb[i]
        val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
        (r * 299 + g * 587 + b * 114) / 1000
    }
}
