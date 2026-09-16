package com.posepilot.app

import com.posepilot.app.models.Landmark
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.pose.landmarks.NormalizedPose

/** Helpers that render a normalized pose into a synthetic camera frame. */
object TestPoses {
    const val W = 720
    const val H = 1280

    /** Places the pose with its hips at (hipX, hipY) pixels, torso length [torsoPx]. */
    fun toFrame(
        pose: NormalizedPose,
        torsoPx: Double = 280.0,
        hipX: Double = W / 2.0,
        hipY: Double = 600.0,
        visibility: Double = 0.99,
        hidden: Set<LandmarkType> = emptySet(),
        timestampMs: Long = 0,
    ): PoseFrame {
        val lms = pose.points.mapValues { (t, p) ->
            Landmark(t, hipX + p.x * torsoPx, hipY + p.y * torsoPx, p.z * torsoPx, if (t in hidden) 0.05 else visibility)
        }
        return PoseFrame(lms, W, H, timestampMs)
    }
}
