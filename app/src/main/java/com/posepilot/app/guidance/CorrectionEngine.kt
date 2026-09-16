package com.posepilot.app.guidance

import com.posepilot.app.models.ArrowKind
import com.posepilot.app.models.CorrectionArrow
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.LandmarkType.*
import com.posepilot.app.models.PoseComparison
import com.posepilot.app.models.PoseCorrection
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.pose.analysis.FeatureExtractor
import com.posepilot.app.pose.analysis.FrameAnalyzer
import com.posepilot.app.pose.analysis.FrameStatus
import com.posepilot.app.pose.analysis.PoseFeature
import com.posepilot.app.pose.landmarks.NormalizedPose
import com.posepilot.app.pose.landmarks.PoseNormalizer
import com.posepilot.app.pose.matching.PoseMatcher
import com.posepilot.app.pose.rules.CorrectionDirection
import com.posepilot.app.pose.rules.RuleEngine
import com.posepilot.app.pose.rules.RuleResult
import com.posepilot.app.targetpose.PoseTemplate
import com.posepilot.app.utilities.Vec2

/**
 * Turns one detected pose + target template into a ranked list of corrections with overlay arrows.
 * Stateless: temporal smoothing and speech timing live in [GuidanceEngine].
 */
object CorrectionEngine {

    fun analyze(frame: PoseFrame?, template: PoseTemplate): PoseComparison {
        if (frame == null || frame.isEmpty) return PoseComparison.EMPTY

        val frameStatus = FrameAnalyzer.analyze(frame, template.framing, template.frameTarget)
        val normalized = PoseNormalizer.normalize(frame)
            ?: return PoseComparison(
                personDetected = true, frame = frame, frameStatus = frameStatus, match = null,
                corrections = listOf(
                    PoseCorrection("torso-hidden", "Face the camera so your shoulders and hips are visible", 0, 1.0, null)
                ),
                ghost = emptyMap(),
            )

        val features = FeatureExtractor.extract(normalized)
        val match = PoseMatcher.match(template, features)
        val ghost = projectGhost(template, normalized)

        val corrections = ArrayList<PoseCorrection>()
        corrections += frameCorrections(frameStatus, normalized)
        for (result in match.offRules) {
            val text = RuleEngine.instructionFor(result) ?: continue
            corrections += PoseCorrection(
                id = "${result.rule.feature.name}-${result.direction.name}",
                instruction = text,
                priority = result.rule.priority,
                severity = result.normalizedError,
                arrow = arrowFor(result, frame, normalized, ghost),
                highlight = result.rule.feature.anchor,
            )
        }
        // Lower priority number first; within the same priority the biggest error first.
        corrections.sortWith(compareBy<PoseCorrection> { it.priority }.thenByDescending { it.severity })
        return PoseComparison(true, frame, frameStatus, match, corrections, ghost)
    }

    /** Places the template's normalized skeleton on the live person's hips at the live torso scale. */
    fun projectGhost(template: PoseTemplate, live: NormalizedPose): Map<LandmarkType, Vec2> =
        template.targetPose.points
            .filterKeys { it in LandmarkType.OVERLAY_JOINTS }
            .mapValues { (_, p) -> live.toImage(p) }

    private fun frameCorrections(status: FrameStatus, pose: NormalizedPose): List<PoseCorrection> =
        status.issues.map { issue ->
            val center = pose.originPx + Vec2(0.0, -0.5 * pose.scalePx)
            val arrow = CorrectionArrow(ArrowKind.STEP, center, center + issue.direction * (0.9 * pose.scalePx))
            PoseCorrection("frame-${issue.type.name}", issue.instruction, issue.priority, 1.0, arrow)
        }

    private fun arrowFor(
        result: RuleResult, frame: PoseFrame, pose: NormalizedPose, ghost: Map<LandmarkType, Vec2>,
    ): CorrectionArrow? {
        val f = result.rule.feature
        val sign = if (result.direction == CorrectionDirection.INCREASE) 1.0 else -1.0
        val torso = pose.scalePx
        fun live(t: LandmarkType): Vec2? = frame[t]?.xy()
        fun mid(a: LandmarkType, b: LandmarkType): Vec2? {
            val pa = live(a) ?: return null; val pb = live(b) ?: return null
            return (pa + pb) / 2.0
        }
        fun limb(t: LandmarkType): CorrectionArrow? {
            val from = live(t) ?: return null
            val to = ghost[t] ?: return null
            if (from.distanceTo(to) < 0.12 * torso) return null
            return CorrectionArrow(ArrowKind.LIMB, from, to)
        }
        return when (f) {
            PoseFeature.LEFT_ARM_RAISE, PoseFeature.LEFT_ELBOW_ANGLE, PoseFeature.LEFT_HAND_REACH -> limb(LEFT_WRIST)
            PoseFeature.RIGHT_ARM_RAISE, PoseFeature.RIGHT_ELBOW_ANGLE, PoseFeature.RIGHT_HAND_REACH -> limb(RIGHT_WRIST)
            PoseFeature.LEFT_KNEE_ANGLE -> limb(LEFT_KNEE)
            PoseFeature.RIGHT_KNEE_ANGLE -> limb(RIGHT_KNEE)
            PoseFeature.STANCE_WIDTH -> {
                val l = limb(LEFT_ANKLE); val r = limb(RIGHT_ANKLE)
                listOfNotNull(l, r).maxByOrNull { it.from.distanceTo(it.to) }
            }
            PoseFeature.BODY_TURN -> mid(LEFT_SHOULDER, RIGHT_SHOULDER)?.let { c ->
                CorrectionArrow(ArrowKind.ROTATE, c, c + Vec2(sign * 0.45 * torso, 0.0))
            }
            PoseFeature.TORSO_LEAN -> mid(LEFT_SHOULDER, RIGHT_SHOULDER)?.let { c ->
                CorrectionArrow(ArrowKind.LIMB, c, c + Vec2(sign * 0.35 * torso, 0.0))
            }
            PoseFeature.SHOULDER_TILT -> {
                val t = if (sign > 0) LEFT_SHOULDER else RIGHT_SHOULDER
                live(t)?.let { CorrectionArrow(ArrowKind.LIMB, it, it + Vec2(0.0, 0.25 * torso)) }
            }
            PoseFeature.HEAD_TURN, PoseFeature.HEAD_TILT -> live(NOSE)?.let { n ->
                val c = n + Vec2(0.0, -0.45 * torso)
                CorrectionArrow(ArrowKind.ROTATE, c, c + Vec2(sign * 0.3 * torso, 0.0))
            }
            PoseFeature.CHIN_HEIGHT -> live(NOSE)?.let { n ->
                CorrectionArrow(ArrowKind.LIMB, n, n + Vec2(0.0, -sign * 0.22 * torso))
            }
        }
    }
}
