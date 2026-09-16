package com.posepilot.app.guidance

import com.posepilot.app.models.GuidanceInstruction
import com.posepilot.app.models.GuidancePhase
import com.posepilot.app.models.GuidanceUpdate
import com.posepilot.app.models.InstructionTone
import com.posepilot.app.models.PoseComparison
import com.posepilot.app.models.PoseCorrection

data class GuidanceConfig(
    val autoCapture: Boolean = true,
    val holdMs: Long = 1000,
    val countdownSeconds: Int = 3,
    val readyThreshold: Double = 0.85,
    /** How long a new top correction must persist before replacing the one on screen. */
    val switchDelayMs: Long = 400,
    val noPersonDelayMs: Long = 700,
    val manualDisplayMs: Long = 2500,
)

/**
 * Stateful coach. Feed it one [PoseComparison] per analysed frame with a monotonic timestamp.
 *
 * Responsibilities: one instruction at a time, anti-flicker switching, "pose ready" hysteresis,
 * hold -> countdown -> capture, manual photographer instructions, and speech throttling.
 * Pure Kotlin with an injected clock so it is fully unit-testable.
 */
class GuidanceEngine(
    var config: GuidanceConfig = GuidanceConfig(),
    val speech: SpeechScheduler = SpeechScheduler(),
) {
    private var phase = GuidancePhase.NO_PERSON
    private var displayed: PoseCorrection? = null
    private var candidateId: String? = null
    private var candidateSince = 0L
    private var lastPersonSeen = Long.MIN_VALUE / 2
    private var holdStart = 0L
    private var countdownStart = 0L
    private var lastCountdownSpoken = -1
    private var smoothedScore = 0.0
    private var manualText: String? = null
    private var manualUntil = 0L
    private var manualPendingSpeech = false

    val currentPhase: GuidancePhase get() = phase

    fun pushManual(text: String, nowMs: Long) {
        manualText = text
        manualUntil = nowMs + config.manualDisplayMs
        manualPendingSpeech = true
    }

    /** Call after a capture has been handled (review closed) to resume coaching. */
    fun reset() {
        phase = GuidancePhase.NO_PERSON
        displayed = null; candidateId = null
        lastCountdownSpoken = -1
        smoothedScore = 0.0
        speech.reset()
    }

    fun update(c: PoseComparison, nowMs: Long): GuidanceUpdate {
        smoothedScore = if (c.personDetected) smoothedScore * 0.7 + c.score * 0.3 else smoothedScore * 0.8

        if (phase == GuidancePhase.CAPTURED) {
            return GuidanceUpdate(GuidanceInstruction("Photo captured", InstructionTone.SUCCESS, phase), smoothedScore)
        }

        // Manual photographer instruction overrides AI text briefly (AI keeps analysing underneath).
        val manual = manualText
        if (manual != null && nowMs < manualUntil) {
            val speak = if (manualPendingSpeech) { manualPendingSpeech = false; speech.shouldSpeak(manual, nowMs, force = true); manual } else null
            cancelCountdownIfNeeded()
            return GuidanceUpdate(
                GuidanceInstruction(manual, InstructionTone.NEUTRAL, GuidancePhase.GUIDING, isManual = true),
                smoothedScore, speech = speak,
            )
        } else if (manual != null) {
            manualText = null
        }

        if (c.personDetected) lastPersonSeen = nowMs
        if (!c.personDetected) {
            if (nowMs - lastPersonSeen < config.noPersonDelayMs && displayed != null) {
                // Brief detection dropout: keep showing the last instruction to avoid flicker.
                return GuidanceUpdate(guidingInstruction(displayed!!, null), smoothedScore)
            }
            phase = GuidancePhase.NO_PERSON
            displayed = null
            val text = "No person detected. Step into the frame."
            return GuidanceUpdate(
                GuidanceInstruction(text, InstructionTone.WARNING, phase), smoothedScore,
                speech = if (speech.shouldSpeak(text, nowMs)) text else null,
            )
        }

        val inReadyState = phase == GuidancePhase.HOLD || phase == GuidancePhase.COUNTDOWN
        val hasFrameIssue = c.corrections.any { it.id.startsWith("frame-") || it.id == "torso-hidden" }
        val coverageOk = (c.match?.coverage ?: 0.0) >= 0.7
        val ready = if (inReadyState) {
            // Hysteresis: once ready, tolerate a small wobble so the countdown isn't cancelled by noise.
            // but any correction more than 2x its tolerance means the person really moved.
            !hasFrameIssue && coverageOk && c.score >= config.readyThreshold - 0.07 &&
                c.corrections.none { it.severity >= 2.0 }
        } else {
            c.corrections.isEmpty() && coverageOk && c.score >= config.readyThreshold
        }

        return if (ready) readyUpdate(nowMs) else guidingUpdate(c, nowMs)
    }

    private fun readyUpdate(nowMs: Long): GuidanceUpdate {
        displayed = null
        when (phase) {
            GuidancePhase.HOLD -> {
                if (config.autoCapture && nowMs - holdStart >= config.holdMs) {
                    phase = GuidancePhase.COUNTDOWN
                    countdownStart = nowMs
                    lastCountdownSpoken = -1
                } else {
                    return GuidanceUpdate(
                        GuidanceInstruction(
                            if (config.autoCapture) "Perfect — hold still" else "Pose ready ✓",
                            InstructionTone.SUCCESS, phase,
                        ),
                        smoothedScore,
                    )
                }
            }
            GuidancePhase.COUNTDOWN -> Unit
            else -> {
                phase = GuidancePhase.HOLD
                holdStart = nowMs
                val text = if (config.autoCapture) "Perfect. Hold." else "Perfect."
                return GuidanceUpdate(
                    GuidanceInstruction(if (config.autoCapture) "Perfect — hold still" else "Pose ready ✓", InstructionTone.SUCCESS, phase),
                    smoothedScore,
                    speech = if (speech.shouldSpeak(text, nowMs, force = true)) text else null,
                )
            }
        }

        // COUNTDOWN
        val elapsed = nowMs - countdownStart
        val total = config.countdownSeconds * 1000L
        if (elapsed >= total) {
            phase = GuidancePhase.CAPTURED
            speech.shouldSpeak("Capture", nowMs, force = true)
            return GuidanceUpdate(
                GuidanceInstruction("Capturing…", InstructionTone.SUCCESS, phase), smoothedScore,
                speech = "Capture", captureNow = true,
            )
        }
        val remaining = (config.countdownSeconds - (elapsed / 1000L)).toInt().coerceAtLeast(1)
        var spoken: String? = null
        if (remaining != lastCountdownSpoken) {
            lastCountdownSpoken = remaining
            spoken = NUMBER_WORDS.getOrElse(remaining) { remaining.toString() }
            speech.shouldSpeak(spoken, nowMs, force = true)
        }
        return GuidanceUpdate(
            GuidanceInstruction("Pose locked ✓", InstructionTone.SUCCESS, phase, countdown = remaining),
            smoothedScore, speech = spoken,
        )
    }

    private fun guidingUpdate(c: PoseComparison, nowMs: Long): GuidanceUpdate {
        phase = GuidancePhase.GUIDING
        lastCountdownSpoken = -1

        val candidate = c.corrections.firstOrNull()
        if (candidate == null) {
            // Nothing out of tolerance but score/coverage not high enough yet.
            displayed = null
            val text = if ((c.match?.coverage ?: 0.0) < 0.7) "Make sure your whole pose is visible" else "Almost there — hold the pose"
            return GuidanceUpdate(GuidanceInstruction(text, InstructionTone.NEUTRAL, phase), smoothedScore)
        }

        val current = displayed
        val currentStillValid = current != null && c.corrections.any { it.id == current.id }
        displayed = when {
            current == null || !currentStillValid -> { candidateId = null; candidate }
            candidate.id == current.id -> { candidateId = null; candidate }
            else -> {
                if (candidateId != candidate.id) { candidateId = candidate.id; candidateSince = nowMs }
                if (nowMs - candidateSince >= config.switchDelayMs) { candidateId = null; candidate }
                else c.corrections.first { it.id == current.id } // refresh arrow positions of the kept instruction
            }
        }
        val shown = displayed!!
        val secondary = c.corrections.firstOrNull { it.id != shown.id }?.instruction
        return GuidanceUpdate(
            guidingInstruction(shown, secondary),
            smoothedScore,
            speech = if (speech.shouldSpeak(shown.instruction, nowMs)) shown.instruction else null,
        )
    }

    private fun guidingInstruction(p: PoseCorrection, secondary: String?) = GuidanceInstruction(
        text = p.instruction,
        tone = if (p.id.startsWith("frame-")) InstructionTone.WARNING else InstructionTone.NEUTRAL,
        phase = GuidancePhase.GUIDING,
        arrow = p.arrow,
        secondaryText = secondary,
    )

    private fun cancelCountdownIfNeeded() {
        if (phase == GuidancePhase.HOLD || phase == GuidancePhase.COUNTDOWN) {
            phase = GuidancePhase.GUIDING
            lastCountdownSpoken = -1
        }
    }

    companion object {
        private val NUMBER_WORDS = mapOf(1 to "One", 2 to "Two", 3 to "Three", 4 to "Four", 5 to "Five")
    }
}
