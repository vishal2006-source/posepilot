package com.posepilot.app.targetpose

import com.posepilot.app.models.FramingType
import com.posepilot.app.pose.analysis.FeatureExtractor
import com.posepilot.app.pose.analysis.PoseFeature
import com.posepilot.app.pose.landmarks.NormalizedPose
import com.posepilot.app.pose.rules.PoseRule

enum class PoseCategory(val label: String) {
    CASUAL("Casual"),
    SOCIAL("Instagram"),
    FORMAL("Formal"),
    PORTRAIT("Portrait"),
    TRAVEL("Travel"),
    REFERENCE("My references"),
}

enum class TemplateSource { LIBRARY, REFERENCE_PHOTO }

/** Where the person should be in the frame. Fractions of image width/height. */
data class FrameTarget(
    val centerX: Double = 0.5,
    val centerTolerance: Double = 0.12,
    /** For FULL_BODY: nose-to-ankle height. For UPPER_BODY: nose-to-hip height. */
    val minBodyFraction: Double,
    val maxBodyFraction: Double,
)

/**
 * A target pose. [targetLandmarks] are in normalized body space (see NormalizedPose) and are used
 * both to derive [rules] and to draw the ghost skeleton.
 */
data class PoseTemplate(
    val id: String,
    val name: String,
    val category: PoseCategory,
    val description: String,
    val framing: FramingType,
    val targetPose: NormalizedPose,
    val rules: List<PoseRule>,
    val frameTarget: FrameTarget,
    val tips: List<String> = emptyList(),
    val source: TemplateSource = TemplateSource.LIBRARY,
) {
    val targetAngles: Map<PoseFeature, Double> get() = rules.associate { it.feature to it.targetValue }

    /** Same pose with left and right swapped (useful for mirrored reference photos). */
    fun mirrored(): PoseTemplate {
        val mirroredPose = targetPose.mirrored()
        val features = rules.map { it.feature }.map { mirrorFeature(it) }.toSet()
        return TemplateFactory.create(
            id = "$id-mirrored", name = "$name (flipped)", category = category, description = description,
            framing = framing, pose = mirroredPose, features = features, tips = tips, source = source,
            toleranceScale = 1.0,
        )
    }

    private fun mirrorFeature(f: PoseFeature): PoseFeature = when (f) {
        PoseFeature.LEFT_ARM_RAISE -> PoseFeature.RIGHT_ARM_RAISE
        PoseFeature.RIGHT_ARM_RAISE -> PoseFeature.LEFT_ARM_RAISE
        PoseFeature.LEFT_ELBOW_ANGLE -> PoseFeature.RIGHT_ELBOW_ANGLE
        PoseFeature.RIGHT_ELBOW_ANGLE -> PoseFeature.LEFT_ELBOW_ANGLE
        PoseFeature.LEFT_HAND_REACH -> PoseFeature.RIGHT_HAND_REACH
        PoseFeature.RIGHT_HAND_REACH -> PoseFeature.LEFT_HAND_REACH
        PoseFeature.LEFT_KNEE_ANGLE -> PoseFeature.RIGHT_KNEE_ANGLE
        PoseFeature.RIGHT_KNEE_ANGLE -> PoseFeature.LEFT_KNEE_ANGLE
        else -> f
    }
}

object TemplateFactory {
    val UPPER_BODY_FEATURES: Set<PoseFeature> = setOf(
        PoseFeature.BODY_TURN, PoseFeature.TORSO_LEAN, PoseFeature.SHOULDER_TILT,
        PoseFeature.LEFT_ARM_RAISE, PoseFeature.RIGHT_ARM_RAISE,
        PoseFeature.LEFT_ELBOW_ANGLE, PoseFeature.RIGHT_ELBOW_ANGLE,
        PoseFeature.LEFT_HAND_REACH, PoseFeature.RIGHT_HAND_REACH,
        PoseFeature.HEAD_TURN, PoseFeature.HEAD_TILT, PoseFeature.CHIN_HEIGHT,
    )
    val ALL_FEATURES: Set<PoseFeature> = PoseFeature.entries.toSet()

    fun defaultFrameTarget(framing: FramingType) = when (framing) {
        FramingType.FULL_BODY -> FrameTarget(minBodyFraction = 0.55, maxBodyFraction = 0.88)
        FramingType.UPPER_BODY -> FrameTarget(minBodyFraction = 0.40, maxBodyFraction = 0.75)
    }

    /**
     * Builds rules by measuring the target pose itself, so targets are never hand-typed numbers that
     * could disagree with the ghost skeleton.
     */
    fun create(
        id: String,
        name: String,
        category: PoseCategory,
        description: String,
        framing: FramingType,
        pose: NormalizedPose,
        features: Set<PoseFeature>,
        tips: List<String> = emptyList(),
        source: TemplateSource = TemplateSource.LIBRARY,
        toleranceScale: Double = 1.0,
        toleranceOverrides: Map<PoseFeature, Double> = emptyMap(),
        weightOverrides: Map<PoseFeature, Double> = emptyMap(),
        frameTarget: FrameTarget = defaultFrameTarget(framing),
    ): PoseTemplate {
        val measured = FeatureExtractor.extract(pose)
        val rules = features.mapNotNull { f ->
            val target = measured[f] ?: return@mapNotNull null
            PoseRule(
                feature = f,
                targetValue = target,
                tolerance = (toleranceOverrides[f] ?: f.defaultTolerance) * toleranceScale,
                weight = weightOverrides[f] ?: 1.0,
            )
        }.sortedBy { it.priority }
        return PoseTemplate(id, name, category, description, framing, pose, rules, frameTarget, tips, source)
    }
}
