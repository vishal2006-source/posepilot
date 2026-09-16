package com.posepilot.app.photoanalysis

import com.posepilot.app.models.CheckStatus
import com.posepilot.app.models.PhotoAnalysis
import com.posepilot.app.models.PhotoMeasurements
import com.posepilot.app.models.QualityCheck
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns measurements into a simple report. Thresholds are heuristics tuned for a 480px-wide grayscale
 * downscale; they flag likely problems rather than guarantee them.
 */
object PhotoQualityEvaluator {
    const val BLUR_FAIL = 40.0
    const val BLUR_WARN = 90.0

    fun evaluate(m: PhotoMeasurements): PhotoAnalysis {
        val checks = mutableListOf<QualityCheck>()

        checks += when (val s = m.poseScore) {
            null -> QualityCheck("Pose match", CheckStatus.UNAVAILABLE, "No target pose to compare")
            else -> {
                val pct = (s * 100).roundToInt()
                QualityCheck("Pose match", when { s >= 0.85 -> CheckStatus.PASS; s >= 0.65 -> CheckStatus.WARN; else -> CheckStatus.FAIL }, "$pct% match")
            }
        }

        checks += when {
            m.faceCount == null -> QualityCheck("Face", CheckStatus.UNAVAILABLE, "Face check unavailable")
            m.faceCount == 0 -> QualityCheck("Face", CheckStatus.WARN, "Face not clearly visible")
            m.faceYawDeg != null && abs(m.faceYawDeg) > 45 -> QualityCheck("Face", CheckStatus.PASS, "Face turned away (fine for looking-away poses)")
            else -> QualityCheck("Face", CheckStatus.PASS, "Face visible")
        }

        val eyes = listOfNotNull(m.leftEyeOpen, m.rightEyeOpen)
        checks += when {
            eyes.isEmpty() -> QualityCheck("Eyes", CheckStatus.UNAVAILABLE, "Couldn't check eyes")
            eyes.min() < 0.3 -> QualityCheck("Eyes", CheckStatus.WARN, "Eyes may be closed")
            else -> QualityCheck("Eyes", CheckStatus.PASS, "Eyes open")
        }

        checks += when {
            !m.poseDetected -> QualityCheck("Framing", CheckStatus.FAIL, "No person detected in the photo")
            m.framingIssues.isNullOrEmpty() -> QualityCheck("Framing", CheckStatus.PASS, "Body well framed")
            else -> QualityCheck("Framing", CheckStatus.WARN, m.framingIssues.first())
        }

        checks += when (val cx = m.personCenterX) {
            null -> QualityCheck("Centering", CheckStatus.UNAVAILABLE, "No person to center")
            else -> if (abs(cx - 0.5) <= 0.15) QualityCheck("Centering", CheckStatus.PASS, "Subject centered")
            else QualityCheck("Centering", CheckStatus.WARN, "Subject is off-center")
        }

        checks += when {
            m.laplacianVariance < BLUR_FAIL -> QualityCheck("Sharpness", CheckStatus.FAIL, "Motion blur detected")
            m.laplacianVariance < BLUR_WARN -> QualityCheck("Sharpness", CheckStatus.WARN, "Slightly soft — hold steady")
            else -> QualityCheck("Sharpness", CheckStatus.PASS, "Sharp")
        }

        val e = m.exposure
        checks += when {
            e.mean < 55 || e.darkFraction > 0.45 -> QualityCheck("Lighting", CheckStatus.WARN, "Too dark — try better lighting")
            e.mean > 205 || e.brightFraction > 0.35 -> QualityCheck("Lighting", CheckStatus.WARN, "Overexposed — avoid direct bright light")
            else -> QualityCheck("Lighting", CheckStatus.PASS, "Good exposure")
        }

        val worst = checks.firstOrNull { it.status == CheckStatus.FAIL } ?: checks.firstOrNull { it.status == CheckStatus.WARN }
        val summary = if (worst == null) "Great shot" else "Try again — ${worst.detail.replaceFirstChar { it.lowercase() }}."
        return PhotoAnalysis(checks, m.poseScore, summary)
    }
}
