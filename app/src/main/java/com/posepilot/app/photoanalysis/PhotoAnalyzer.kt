package com.posepilot.app.photoanalysis

import android.graphics.Bitmap
import com.posepilot.app.guidance.CorrectionEngine
import com.posepilot.app.models.PhotoAnalysis
import com.posepilot.app.models.PhotoMeasurements
import com.posepilot.app.pose.analysis.FrameAnalyzer
import com.posepilot.app.pose.detection.StillImagePoseDetector
import com.posepilot.app.targetpose.PoseTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Gathers measurements from a captured photo (on a background thread) and builds the quality report. */
class PhotoAnalyzer(private val detector: StillImagePoseDetector) {

    suspend fun analyze(photo: Bitmap, template: PoseTemplate?): PhotoAnalysis = withContext(Dispatchers.Default) {
        // Fixed working width so blur thresholds are comparable between phones.
        val small = scaleToWidth(photo, 480)
        val pixels = IntArray(small.width * small.height)
        small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
        val gray = ImageMetrics.argbToGray(pixels)
        val lapVar = ImageMetrics.laplacianVariance(gray, small.width, small.height)
        val exposure = ImageMetrics.exposure(gray, small.width, small.height)
        if (small !== photo) small.recycle()

        val detectBitmap = scaleToMaxSide(photo, 1280)
        val detection = runCatching { detector.detect(detectBitmap) }.getOrNull()
        if (detectBitmap !== photo) detectBitmap.recycle()

        val frame = detection?.frame
        val faces = detection?.faces
        val mainFace = faces?.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

        val frameStatus = if (frame != null && template != null) FrameAnalyzer.analyze(frame, template.framing, template.frameTarget) else null
        val poseScore = if (frame != null && template != null) CorrectionEngine.analyze(frame, template).match?.overall else null

        PhotoQualityEvaluator.evaluate(
            PhotoMeasurements(
                laplacianVariance = lapVar,
                exposure = exposure,
                faceCount = faces?.size,
                leftEyeOpen = mainFace?.leftEyeOpenProbability?.toDouble(),
                rightEyeOpen = mainFace?.rightEyeOpenProbability?.toDouble(),
                faceYawDeg = mainFace?.headEulerAngleY?.toDouble(),
                framingIssues = frameStatus?.issues
                    ?.filter { it.priority <= 3 }
                    ?.map { it.instruction },
                personCenterX = frameStatus?.centerX,
                poseScore = poseScore,
                poseDetected = frame != null,
            )
        )
    }

    private fun scaleToWidth(src: Bitmap, width: Int): Bitmap {
        if (src.width <= width) return src
        val h = (src.height * width.toFloat() / src.width).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, width, h, true)
    }

    private fun scaleToMaxSide(src: Bitmap, max: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= max) return src
        val s = max.toFloat() / longest
        return Bitmap.createScaledBitmap(src, (src.width * s).toInt(), (src.height * s).toInt(), true)
    }
}
