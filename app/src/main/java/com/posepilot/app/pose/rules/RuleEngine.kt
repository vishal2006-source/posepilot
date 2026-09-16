package com.posepilot.app.pose.rules

import com.posepilot.app.pose.analysis.PoseFeature
import kotlin.math.abs
import kotlin.math.max

/**
 * Evaluates [PoseRule]s against measured features.
 *
 * Scoring per rule (e = |current - target|, t = tolerance):
 *   e <= t/2      -> PERFECT, score 1.0 .. 0.95
 *   e <= t        -> CLOSE,   score 0.95 .. 0.85
 *   e >  t        -> OFF,     score falls linearly from 0.85 to 0 at e = 4t
 * so a score always corresponds to a real angle/distance difference.
 */
object RuleEngine {

    fun evaluate(rules: List<PoseRule>, features: Map<PoseFeature, Double>): List<RuleResult> =
        rules.map { evaluate(it, features[it.feature]) }

    fun evaluate(rule: PoseRule, current: Double?): RuleResult {
        if (current == null || current.isNaN()) {
            return RuleResult(rule, null, null, RuleStatus.UNKNOWN, 0.0, CorrectionDirection.NONE)
        }
        val error = current - rule.targetValue
        val e = abs(error)
        val t = rule.tolerance
        val status = when {
            e <= t / 2 -> RuleStatus.PERFECT
            e <= t -> RuleStatus.CLOSE
            else -> RuleStatus.OFF
        }
        val direction = when {
            status != RuleStatus.OFF -> CorrectionDirection.NONE
            error < 0 -> CorrectionDirection.INCREASE
            else -> CorrectionDirection.DECREASE
        }
        return RuleResult(rule, current, error, status, scoreFor(e, t), direction)
    }

    fun scoreFor(absError: Double, tolerance: Double): Double = when {
        absError <= tolerance / 2 -> 1.0 - 0.05 * (absError / (tolerance / 2))
        absError <= tolerance -> 0.95 - 0.10 * ((absError - tolerance / 2) / (tolerance / 2))
        else -> max(0.0, 0.85 - 0.85 * (absError - tolerance) / (3 * tolerance))
    }

    /** Human instruction for an OFF result. */
    fun instructionFor(result: RuleResult): String? {
        if (result.direction == CorrectionDirection.NONE) return null
        val rule = result.rule
        val f = rule.feature
        // If the target is roughly neutral (e.g. face the camera), a single clear instruction reads better.
        if (f.neutralInstruction != null && abs(rule.targetValue) <= f.neutralBand) return f.neutralInstruction
        return if (result.direction == CorrectionDirection.INCREASE) rule.increaseText ?: f.increaseInstruction
        else rule.decreaseText ?: f.decreaseInstruction
    }
}
