package com.posepilot.app.models

data class UserSettings(
    val voiceGuidance: Boolean = true,
    val showSkeleton: Boolean = true,
    val showTargetPose: Boolean = true,
    val showMatchPercentage: Boolean = true,
    val autoCapture: Boolean = true,
    val countdownSeconds: Int = 3,
    /** Minimum gap between spoken instructions. */
    val instructionIntervalMs: Long = 2500,
    /** Save front-camera photos mirrored (as seen in the preview). */
    val mirrorFrontCamera: Boolean = true,
    val useFrontCamera: Boolean = false,
)

enum class LensFacing { BACK, FRONT }

data class CameraState(
    val lens: LensFacing = LensFacing.BACK,
    val permissionGranted: Boolean = false,
    val isBound: Boolean = false,
    val error: String? = null,
    /** Size of the upright analysis image the landmarks are expressed in. */
    val analysisWidth: Int = 0,
    val analysisHeight: Int = 0,
)
