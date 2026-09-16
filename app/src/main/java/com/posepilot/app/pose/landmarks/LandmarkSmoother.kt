package com.posepilot.app.pose.landmarks

import com.posepilot.app.models.Landmark
import com.posepilot.app.models.PoseFrame

/**
 * Exponential moving average over landmark positions to remove per-frame jitter
 * without adding noticeable lag. Resets if the person disappears for longer than [resetAfterMs].
 */
class LandmarkSmoother(
    private val alpha: Double = 0.55,
    private val resetAfterMs: Long = 500,
) {
    private var previous: PoseFrame? = null

    fun smooth(frame: PoseFrame): PoseFrame {
        val prev = previous
        if (prev == null || frame.timestampMs - prev.timestampMs > resetAfterMs ||
            prev.imageWidth != frame.imageWidth || prev.imageHeight != frame.imageHeight
        ) {
            previous = frame
            return frame
        }
        val smoothed = frame.landmarks.mapValues { (type, cur) ->
            val old = prev.landmarks[type] ?: return@mapValues cur
            Landmark(
                type = type,
                x = alpha * cur.x + (1 - alpha) * old.x,
                y = alpha * cur.y + (1 - alpha) * old.y,
                z = alpha * cur.z + (1 - alpha) * old.z,
                visibility = cur.visibility,
            )
        }
        val result = frame.copy(landmarks = smoothed)
        previous = result
        return result
    }

    fun reset() { previous = null }
}
