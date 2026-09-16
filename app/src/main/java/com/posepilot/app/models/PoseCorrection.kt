package com.posepilot.app.models

import com.posepilot.app.pose.analysis.FrameStatus
import com.posepilot.app.pose.matching.PoseMatch
import com.posepilot.app.utilities.Vec2

enum class ArrowKind {
    /** Straight arrow from a body point toward a target point (e.g. wrist -> ghost wrist). */
    LIMB,
    /** Large horizontal/vertical arrow beside the body for stepping. */
    STEP,
    /** Curved arrow around the head or shoulders for rotation. */
    ROTATE,
}

/**
 * Arrow description in un-mirrored image pixel coordinates.
 * For ROTATE, [to] - [from] encodes the rotation direction (+x = toward subject's left).
 */
data class CorrectionArrow(val kind: ArrowKind, val from: Vec2, val to: Vec2)

/** One actionable correction, already ranked. */
data class PoseCorrection(
    val id: String,
    val instruction: String,
    val priority: Int,
    val severity: Double,
    val arrow: CorrectionArrow?,
    val highlight: LandmarkType? = null,
)

/** Everything the UI needs for one analysed frame. */
data class PoseComparison(
    val personDetected: Boolean,
    val frame: PoseFrame?,
    val frameStatus: FrameStatus?,
    val match: PoseMatch?,
    val corrections: List<PoseCorrection>,
    /** Ghost skeleton projected onto the live person, image pixels. */
    val ghost: Map<LandmarkType, Vec2>,
) {
    val score: Double get() = match?.overall ?: 0.0

    companion object {
        val EMPTY = PoseComparison(false, null, null, null, emptyList(), emptyMap())
    }
}
