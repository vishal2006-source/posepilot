package com.posepilot.app.models

/**
 * The 33 body landmarks produced by ML Kit Pose Detection (same order as MediaPipe BlazePose).
 * [mlKitIndex] matches com.google.mlkit.vision.pose.PoseLandmark constants.
 * LEFT/RIGHT always mean the subject's anatomical left/right, never the screen side.
 */
enum class LandmarkType(val mlKitIndex: Int) {
    NOSE(0),
    LEFT_EYE_INNER(1), LEFT_EYE(2), LEFT_EYE_OUTER(3),
    RIGHT_EYE_INNER(4), RIGHT_EYE(5), RIGHT_EYE_OUTER(6),
    LEFT_EAR(7), RIGHT_EAR(8),
    LEFT_MOUTH(9), RIGHT_MOUTH(10),
    LEFT_SHOULDER(11), RIGHT_SHOULDER(12),
    LEFT_ELBOW(13), RIGHT_ELBOW(14),
    LEFT_WRIST(15), RIGHT_WRIST(16),
    LEFT_PINKY(17), RIGHT_PINKY(18),
    LEFT_INDEX(19), RIGHT_INDEX(20),
    LEFT_THUMB(21), RIGHT_THUMB(22),
    LEFT_HIP(23), RIGHT_HIP(24),
    LEFT_KNEE(25), RIGHT_KNEE(26),
    LEFT_ANKLE(27), RIGHT_ANKLE(28),
    LEFT_HEEL(29), RIGHT_HEEL(30),
    LEFT_FOOT_INDEX(31), RIGHT_FOOT_INDEX(32);

    /** The same landmark on the other side of the body (NOSE maps to itself). Used to mirror poses. */
    val opposite: LandmarkType
        get() = when {
            name.startsWith("LEFT_") -> valueOf("RIGHT_" + name.removePrefix("LEFT_"))
            name.startsWith("RIGHT_") -> valueOf("LEFT_" + name.removePrefix("RIGHT_"))
            else -> this
        }

    companion object {
        private val byIndex = entries.associateBy { it.mlKitIndex }
        fun fromMlKitIndex(index: Int): LandmarkType? = byIndex[index]

        /** Bones drawn for the skeleton overlay. Hand detail points are skipped to keep the overlay clean. */
        val SKELETON_CONNECTIONS: List<Pair<LandmarkType, LandmarkType>> = listOf(
            LEFT_SHOULDER to RIGHT_SHOULDER,
            LEFT_SHOULDER to LEFT_ELBOW, LEFT_ELBOW to LEFT_WRIST,
            RIGHT_SHOULDER to RIGHT_ELBOW, RIGHT_ELBOW to RIGHT_WRIST,
            LEFT_SHOULDER to LEFT_HIP, RIGHT_SHOULDER to RIGHT_HIP,
            LEFT_HIP to RIGHT_HIP,
            LEFT_HIP to LEFT_KNEE, LEFT_KNEE to LEFT_ANKLE,
            RIGHT_HIP to RIGHT_KNEE, RIGHT_KNEE to RIGHT_ANKLE,
            LEFT_EAR to LEFT_EYE, LEFT_EYE to NOSE, NOSE to RIGHT_EYE, RIGHT_EYE to RIGHT_EAR,
        )

        /** Points drawn as joints on the overlay. */
        val OVERLAY_JOINTS: Set<LandmarkType> = SKELETON_CONNECTIONS.flatMap { listOf(it.first, it.second) }.toSet()
    }
}
