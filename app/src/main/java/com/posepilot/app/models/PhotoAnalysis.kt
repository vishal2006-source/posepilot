package com.posepilot.app.models

enum class CheckStatus { PASS, WARN, FAIL, UNAVAILABLE }

data class QualityCheck(val name: String, val status: CheckStatus, val detail: String)

data class PhotoAnalysis(
    val checks: List<QualityCheck>,
    val poseScore: Double?,
    val summary: String,
) {
    val hasProblems: Boolean get() = checks.any { it.status == CheckStatus.WARN || it.status == CheckStatus.FAIL }
}

/** Raw measurements gathered from the captured photo by the Android layer. */
data class PhotoMeasurements(
    val laplacianVariance: Double,
    val exposure: com.posepilot.app.photoanalysis.ImageMetrics.Exposure,
    val faceCount: Int?,
    val leftEyeOpen: Double?,
    val rightEyeOpen: Double?,
    val faceYawDeg: Double?,
    val framingIssues: List<String>?,
    val personCenterX: Double?,
    val poseScore: Double?,
    val poseDetected: Boolean,
)
