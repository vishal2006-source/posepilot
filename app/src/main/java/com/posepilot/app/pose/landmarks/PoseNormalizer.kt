package com.posepilot.app.pose.landmarks

import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.LandmarkType.LEFT_HIP
import com.posepilot.app.models.LandmarkType.LEFT_SHOULDER
import com.posepilot.app.models.LandmarkType.RIGHT_HIP
import com.posepilot.app.models.LandmarkType.RIGHT_SHOULDER
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.utilities.Vec2
import com.posepilot.app.utilities.Vec3

/**
 * A body-size-independent pose.
 *
 * Origin = mid-point of the hips, unit length = torso length (hip mid -> shoulder mid).
 * Axes keep the image convention (x right, y down), so a tall person far away and a short person
 * close to the camera produce the same numbers for the same pose.
 */
data class NormalizedPose(
    val points: Map<LandmarkType, Vec3>,
    val visible: Set<LandmarkType>,
    /** Hip mid-point in source image pixels. */
    val originPx: Vec2,
    /** Torso length in source image pixels. */
    val scalePx: Double,
) {
    operator fun get(type: LandmarkType): Vec3? = points[type]
    fun isVisible(type: LandmarkType) = type in visible
    fun allVisible(types: Collection<LandmarkType>) = types.all { it in visible }

    /** Project a normalized point back to image pixels using this pose's origin and scale. */
    fun toImage(p: Vec3): Vec2 = Vec2(originPx.x + p.x * scalePx, originPx.y + p.y * scalePx)

    /**
     * Mirror left/right: flips x and swaps LEFT_/RIGHT_ labels, so a pose shot as a mirrored selfie
     * can be matched correctly.
     */
    fun mirrored(): NormalizedPose = copy(
        points = points.entries.associate { (t, p) -> t.opposite to Vec3(-p.x, p.y, p.z) },
        visible = visible.map { it.opposite }.toSet(),
    )
}

object PoseNormalizer {
    /** Torso landmarks that must be visible for normalization to be meaningful. */
    val REQUIRED = listOf(LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HIP, RIGHT_HIP)

    /** Returns null when shoulders/hips are not reliable enough to define a body scale. */
    fun normalize(frame: PoseFrame, visibilityThreshold: Double = PoseFrame.VISIBILITY_THRESHOLD): NormalizedPose? {
        val ls = frame[LEFT_SHOULDER] ?: return null
        val rs = frame[RIGHT_SHOULDER] ?: return null
        val lh = frame[LEFT_HIP] ?: return null
        val rh = frame[RIGHT_HIP] ?: return null
        // Hips are often partly hidden in upper-body shots; ML Kit still estimates them, so accept a lower bar.
        if (ls.visibility < visibilityThreshold || rs.visibility < visibilityThreshold) return null
        if (lh.visibility < 0.2 || rh.visibility < 0.2) return null

        val shoulderMid = Vec2((ls.x + rs.x) / 2, (ls.y + rs.y) / 2)
        val hipMid = Vec2((lh.x + rh.x) / 2, (lh.y + rh.y) / 2)
        val torso = shoulderMid.distanceTo(hipMid)
        if (torso < 1.0) return null

        val points = frame.landmarks.mapValues { (_, l) ->
            Vec3((l.x - hipMid.x) / torso, (l.y - hipMid.y) / torso, l.z / torso)
        }
        val visible = frame.landmarks.filterValues { it.visibility >= visibilityThreshold }.keys
        return NormalizedPose(points, visible, hipMid, torso)
    }
}
