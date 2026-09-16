package com.posepilot.app.pose.analysis

import com.posepilot.app.models.LandmarkType.*
import com.posepilot.app.pose.landmarks.NormalizedPose
import com.posepilot.app.utilities.AngleMath
import com.posepilot.app.utilities.Vec2
import com.posepilot.app.utilities.Vec3
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2

/**
 * Converts a normalized pose into geometric measurements. Pure geometry, no ML:
 * every number is derived from landmark positions so it can be unit-tested.
 */
object FeatureExtractor {

    /**
     * Computes every feature whose required landmarks are visible.
     * Features with missing landmarks are omitted (they are "unknown", not "wrong").
     */
    fun extract(pose: NormalizedPose): Map<PoseFeature, Double> {
        val out = LinkedHashMap<PoseFeature, Double>()
        for (f in PoseFeature.entries) {
            if (!pose.allVisible(f.required)) continue
            val v = compute(f, pose) ?: continue
            if (!v.isNaN()) out[f] = v
        }
        return out
    }

    fun compute(feature: PoseFeature, p: NormalizedPose): Double? {
        fun pt(t: com.posepilot.app.models.LandmarkType): Vec3? = p[t]
        fun v2(t: com.posepilot.app.models.LandmarkType): Vec2? = p[t]?.xy()

        return when (feature) {
            PoseFeature.BODY_TURN -> {
                // Yaw of the shoulder line in the x-z plane. 0 = facing camera.
                // Positive = the left shoulder is further from the camera, i.e. body turned to the subject's left.
                val l = pt(LEFT_SHOULDER) ?: return null
                val r = pt(RIGHT_SHOULDER) ?: return null
                AngleMath.toDegrees(atan2(l.z - r.z, l.x - r.x))
            }
            PoseFeature.TORSO_LEAN -> {
                // Angle of hip-mid -> shoulder-mid from vertical. Positive = shoulders shifted to the subject's left (+x).
                val sm = mid(v2(LEFT_SHOULDER), v2(RIGHT_SHOULDER)) ?: return null
                val hm = mid(v2(LEFT_HIP), v2(RIGHT_HIP)) ?: return null
                val up = sm - hm
                AngleMath.toDegrees(atan2(up.x, -up.y))
            }
            PoseFeature.LEFT_ARM_RAISE -> angle(v2(LEFT_HIP), v2(LEFT_SHOULDER), v2(LEFT_ELBOW))
            PoseFeature.RIGHT_ARM_RAISE -> angle(v2(RIGHT_HIP), v2(RIGHT_SHOULDER), v2(RIGHT_ELBOW))
            PoseFeature.LEFT_ELBOW_ANGLE -> angle(v2(LEFT_SHOULDER), v2(LEFT_ELBOW), v2(LEFT_WRIST))
            PoseFeature.RIGHT_ELBOW_ANGLE -> angle(v2(RIGHT_SHOULDER), v2(RIGHT_ELBOW), v2(RIGHT_WRIST))
            PoseFeature.LEFT_HAND_REACH -> {
                // Horizontal distance of the wrist from the body midline, positive toward the subject's own left side.
                val sm = mid(v2(LEFT_SHOULDER), v2(RIGHT_SHOULDER)) ?: return null
                val w = v2(LEFT_WRIST) ?: return null
                w.x - sm.x
            }
            PoseFeature.RIGHT_HAND_REACH -> {
                val sm = mid(v2(LEFT_SHOULDER), v2(RIGHT_SHOULDER)) ?: return null
                val w = v2(RIGHT_WRIST) ?: return null
                sm.x - w.x
            }
            PoseFeature.STANCE_WIDTH -> {
                val l = v2(LEFT_ANKLE) ?: return null
                val r = v2(RIGHT_ANKLE) ?: return null
                abs(l.x - r.x)
            }
            PoseFeature.LEFT_KNEE_ANGLE -> angle(v2(LEFT_HIP), v2(LEFT_KNEE), v2(LEFT_ANKLE))
            PoseFeature.RIGHT_KNEE_ANGLE -> angle(v2(RIGHT_HIP), v2(RIGHT_KNEE), v2(RIGHT_ANKLE))
            PoseFeature.SHOULDER_TILT -> {
                // Positive = left shoulder lower than right shoulder.
                val l = v2(LEFT_SHOULDER) ?: return null
                val r = v2(RIGHT_SHOULDER) ?: return null
                AngleMath.toDegrees(atan2(l.y - r.y, abs(l.x - r.x).coerceAtLeast(1e-6)))
            }
            PoseFeature.HEAD_TURN -> {
                // Where the nose sits between the ears horizontally.
                // ratio = (dRight - dLeft) / (dRight + dLeft) ~ tan(yaw) for a round head, so atan gives an approximate yaw.
                // Positive = face turned to the subject's left.
                val n = v2(NOSE) ?: return null
                val le = v2(LEFT_EAR) ?: return null
                val re = v2(RIGHT_EAR) ?: return null
                val dL = abs(n.x - le.x)
                val dR = abs(n.x - re.x)
                if (dL + dR < 1e-6) return null
                AngleMath.toDegrees(atan((dR - dL) / (dR + dL)))
            }
            PoseFeature.HEAD_TILT -> {
                // Roll of the eye line. Positive = left eye lower = head tilted toward the left shoulder.
                val l = v2(LEFT_EYE) ?: return null
                val r = v2(RIGHT_EYE) ?: return null
                AngleMath.toDegrees(atan2(l.y - r.y, abs(l.x - r.x).coerceAtLeast(1e-6)))
            }
            PoseFeature.CHIN_HEIGHT -> {
                // Vertical position of the nose relative to the ear mid-point (torso units). Positive = chin up.
                val n = v2(NOSE) ?: return null
                val em = mid(v2(LEFT_EAR), v2(RIGHT_EAR)) ?: return null
                em.y - n.y
            }
        }
    }

    private fun mid(a: Vec2?, b: Vec2?): Vec2? = if (a == null || b == null) null else (a + b) / 2.0
    private fun angle(a: Vec2?, v: Vec2?, b: Vec2?): Double? =
        if (a == null || v == null || b == null) null else AngleMath.jointAngle(a, v, b)
}
