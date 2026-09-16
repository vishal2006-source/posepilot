package com.posepilot.app.pose.analysis

import com.posepilot.app.models.FramingType
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.LandmarkType.*
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.targetpose.FrameTarget
import com.posepilot.app.utilities.Vec2

enum class FrameIssueType { FEET_CUT_OFF, HEAD_CUT_OFF, TOO_CLOSE, TOO_FAR, MOVE_TO_SUBJECT_LEFT, MOVE_TO_SUBJECT_RIGHT }

/**
 * [direction] is an image-space unit vector showing where the person should move (for the arrow overlay).
 */
data class FrameIssue(
    val type: FrameIssueType,
    val instruction: String,
    val priority: Int,
    val direction: Vec2,
)

data class FrameStatus(
    /** Body centre, fraction of image width (un-mirrored image). */
    val centerX: Double,
    /** Nose-to-ankle (full body) or nose-to-hip (upper body) height as a fraction of image height. */
    val bodyFraction: Double,
    val issues: List<FrameIssue>,
)

/**
 * Checks where the person is in the frame. Works in the un-mirrored upright image.
 *
 * For a person facing the camera their own left side is on the image's +x side, so if they are too far toward
 * image-left they must step to THEIR left. Instructions are phrased for the subject; arrows are drawn in
 * image space and mirrored by the overlay when the preview is mirrored.
 */
object FrameAnalyzer {
    private const val EDGE_MARGIN = 0.02

    fun analyze(frame: PoseFrame, framing: FramingType, target: FrameTarget): FrameStatus {
        val w = frame.imageWidth.toDouble()
        val h = frame.imageHeight.toDouble()
        val issues = mutableListOf<FrameIssue>()

        val torsoPoints = listOf(LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HIP, RIGHT_HIP).mapNotNull { frame[it] }
        val centerX = if (torsoPoints.isNotEmpty()) torsoPoints.sumOf { it.x } / torsoPoints.size / w else 0.5

        val nose = frame[NOSE]
        val headOut = nose == null || nose.visibility < 0.5 || nose.y < h * EDGE_MARGIN
        val lowestY = when (framing) {
            FramingType.FULL_BODY -> maxOfVisible(frame, listOf(LEFT_ANKLE, RIGHT_ANKLE))
            FramingType.UPPER_BODY -> maxOfVisible(frame, listOf(LEFT_HIP, RIGHT_HIP))
        }
        val topY = nose?.y ?: 0.0
        val bodyFraction = if (lowestY != null) (lowestY - topY) / h else 1.0

        if (headOut) {
            issues += FrameIssue(FrameIssueType.HEAD_CUT_OFF, "Step back — your head is out of frame", 1, Vec2(0.0, -1.0))
        }
        if (framing == FramingType.FULL_BODY) {
            val feetVisible = listOf(LEFT_ANKLE, RIGHT_ANKLE).all { t ->
                val a = frame[t]; a != null && a.visibility >= 0.5 && a.y <= h * (1 - EDGE_MARGIN)
            }
            if (!feetVisible) {
                issues += FrameIssue(FrameIssueType.FEET_CUT_OFF, "Step back so your full body is visible", 2, Vec2(0.0, 1.0))
            }
        }
        if (issues.none { it.type == FrameIssueType.FEET_CUT_OFF || it.type == FrameIssueType.HEAD_CUT_OFF }) {
            if (bodyFraction > target.maxBodyFraction) {
                issues += FrameIssue(FrameIssueType.TOO_CLOSE, "Move back a little", 3, Vec2(0.0, 1.0))
            } else if (bodyFraction < target.minBodyFraction) {
                issues += FrameIssue(FrameIssueType.TOO_FAR, "Move closer to the camera", 3, Vec2(0.0, -1.0))
            }
        }
        if (centerX < target.centerX - target.centerTolerance) {
            issues += FrameIssue(FrameIssueType.MOVE_TO_SUBJECT_LEFT, "Step to your left", 4, Vec2(1.0, 0.0))
        } else if (centerX > target.centerX + target.centerTolerance) {
            issues += FrameIssue(FrameIssueType.MOVE_TO_SUBJECT_RIGHT, "Step to your right", 4, Vec2(-1.0, 0.0))
        }
        return FrameStatus(centerX, bodyFraction, issues.sortedBy { it.priority })
    }

    private fun maxOfVisible(frame: PoseFrame, types: List<LandmarkType>): Double? =
        types.mapNotNull { frame[it] }.filter { it.visibility >= 0.5 }.maxOfOrNull { it.y }
}
