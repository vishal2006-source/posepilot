package com.posepilot.app.models

enum class GuidancePhase { NO_PERSON, GUIDING, HOLD, COUNTDOWN, CAPTURED }

enum class InstructionTone { NEUTRAL, WARNING, SUCCESS }

data class GuidanceInstruction(
    val text: String,
    val tone: InstructionTone,
    val phase: GuidancePhase,
    val arrow: CorrectionArrow? = null,
    /** Next correction, shown small under the main one. */
    val secondaryText: String? = null,
    val countdown: Int? = null,
    val isManual: Boolean = false,
)

/** Output of one guidance tick. */
data class GuidanceUpdate(
    val instruction: GuidanceInstruction,
    val displayScore: Double,
    /** Text to speak now, if any. */
    val speech: String? = null,
    /** True exactly once when the auto-capture countdown finishes. */
    val captureNow: Boolean = false,
)
