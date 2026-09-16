package com.posepilot.app

import com.posepilot.app.models.CheckStatus
import com.posepilot.app.models.PhotoMeasurements
import com.posepilot.app.photoanalysis.ImageMetrics
import com.posepilot.app.photoanalysis.PhotoQualityEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoQualityTest {
    private val w = 64
    private val h = 64

    @Test fun flatImageHasZeroLaplacianVariance() {
        assertEquals(0.0, ImageMetrics.laplacianVariance(IntArray(w * h) { 128 }, w, h), 1e-9)
    }

    @Test fun sharpEdgesBeatBlurredEdges() {
        val sharp = IntArray(w * h) { i -> if ((i % w / 4 + i / w / 4) % 2 == 0) 20 else 230 }
        val blurred = boxBlur(boxBlur(sharp))
        val vs = ImageMetrics.laplacianVariance(sharp, w, h)
        val vb = ImageMetrics.laplacianVariance(blurred, w, h)
        assertTrue("sharp $vs should exceed blurred $vb", vs > vb * 3)
    }

    @Test fun reportFlagsBlurAndDarkness() {
        val m = PhotoMeasurements(
            laplacianVariance = 20.0, exposure = ImageMetrics.Exposure(40.0, 0.6, 0.0),
            faceCount = 1, leftEyeOpen = 0.9, rightEyeOpen = 0.8, faceYawDeg = 0.0,
            framingIssues = emptyList(), personCenterX = 0.5, poseScore = 0.92, poseDetected = true,
        )
        val r = PhotoQualityEvaluator.evaluate(m)
        assertEquals(CheckStatus.FAIL, r.checks.first { it.name == "Sharpness" }.status)
        assertEquals(CheckStatus.WARN, r.checks.first { it.name == "Lighting" }.status)
        assertEquals(CheckStatus.PASS, r.checks.first { it.name == "Pose match" }.status)
        assertTrue(r.summary.startsWith("Try again"))
    }

    @Test fun goodPhotoPasses() {
        val m = PhotoMeasurements(250.0, ImageMetrics.Exposure(120.0, 0.05, 0.02), 1, 0.9, 0.9, 5.0, emptyList(), 0.52, 0.9, true)
        val r = PhotoQualityEvaluator.evaluate(m)
        assertTrue(!r.hasProblems)
        assertEquals("Great shot", r.summary)
    }

    private fun boxBlur(src: IntArray): IntArray = IntArray(src.size) { i ->
        val x = i % w; val y = i / w
        var s = 0; var n = 0
        for (dy in -1..1) for (dx in -1..1) {
            val xx = x + dx; val yy = y + dy
            if (xx in 0 until w && yy in 0 until h) { s += src[yy * w + xx]; n++ }
        }
        s / n
    }
}
