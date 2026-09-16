package com.posepilot.app.models

import com.posepilot.app.utilities.Vec2

/**
 * One detected landmark in pixel coordinates of the upright (rotation-corrected, un-mirrored) image.
 * [visibility] is ML Kit's inFrameLikelihood (0..1).
 */
data class Landmark(
    val type: LandmarkType,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val visibility: Double = 1.0,
) {
    fun xy() = Vec2(x, y)
}

/** A single detected person in one frame. */
data class PoseFrame(
    val landmarks: Map<LandmarkType, Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long = 0L,
) {
    operator fun get(type: LandmarkType): Landmark? = landmarks[type]

    fun isVisible(type: LandmarkType, threshold: Double = VISIBILITY_THRESHOLD): Boolean {
        val l = landmarks[type] ?: return false
        return l.visibility >= threshold
    }

    val isEmpty: Boolean get() = landmarks.isEmpty()

    companion object {
        const val VISIBILITY_THRESHOLD = 0.5
    }
}
