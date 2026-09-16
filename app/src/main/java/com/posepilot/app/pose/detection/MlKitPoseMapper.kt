package com.posepilot.app.pose.detection

import com.google.mlkit.vision.pose.Pose
import com.posepilot.app.models.Landmark
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.PoseFrame

/** Converts ML Kit's Pose into our framework-independent PoseFrame. */
object MlKitPoseMapper {
    fun toFrame(pose: Pose, uprightWidth: Int, uprightHeight: Int, timestampMs: Long): PoseFrame? {
        val all = pose.allPoseLandmarks
        if (all.isEmpty()) return null
        val map = HashMap<LandmarkType, Landmark>(all.size * 2)
        for (lm in all) {
            val type = LandmarkType.fromMlKitIndex(lm.landmarkType) ?: continue
            val p = lm.position3D
            map[type] = Landmark(type, p.x.toDouble(), p.y.toDouble(), p.z.toDouble(), lm.inFrameLikelihood.toDouble())
        }
        return PoseFrame(map, uprightWidth, uprightHeight, timestampMs)
    }
}
