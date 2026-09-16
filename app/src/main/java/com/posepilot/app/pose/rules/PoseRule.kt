package com.posepilot.app.pose.rules

import com.posepilot.app.pose.analysis.PoseFeature

/**
 * One comparable constraint of a target pose, e.g. "right elbow = 90° ± 15°".
 * [weight] scales this rule's contribution inside its body region.
 */
data class PoseRule(
    val feature: PoseFeature,
    val targetValue: Double,
    val tolerance: Double = feature.defaultTolerance,
    val priority: Int = feature.priority,
    val weight: Double = 1.0,
    /** Optional custom instruction overriding the feature's default wording. */
    val increaseText: String? = null,
    val decreaseText: String? = null,
) {
    init {
        require(tolerance > 0) { "tolerance must be positive" }
        require(weight >= 0) { "weight must be non-negative" }
    }
}

enum class RuleStatus { PERFECT, CLOSE, OFF, UNKNOWN }

/** Direction the user needs to change the measured value. */
enum class CorrectionDirection { INCREASE, DECREASE, NONE }

data class RuleResult(
    val rule: PoseRule,
    val currentValue: Double?,
    /** current - target (signed), null when unknown. */
    val error: Double?,
    val status: RuleStatus,
    /** 0..1 */
    val score: Double,
    val direction: CorrectionDirection,
) {
    /** Error measured in tolerances; 1.0 = exactly at the edge of tolerance. */
    val normalizedError: Double get() = error?.let { kotlin.math.abs(it) / rule.tolerance } ?: 0.0
}
