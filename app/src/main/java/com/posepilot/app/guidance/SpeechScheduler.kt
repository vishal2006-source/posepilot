package com.posepilot.app.guidance

/**
 * Decides when an instruction may be spoken so the coach doesn't talk every frame.
 * - The same sentence is not repeated within [repeatCooldownMs].
 * - Different sentences are spaced by at least [minIntervalMs].
 * - Forced cues (countdown, "Perfect") bypass both checks.
 */
class SpeechScheduler(
    var minIntervalMs: Long = 2500,
    var repeatCooldownMs: Long = 6000,
) {
    private var lastText: String? = null
    private var lastTime: Long = Long.MIN_VALUE / 2

    fun shouldSpeak(text: String, nowMs: Long, force: Boolean = false): Boolean {
        if (force) { record(text, nowMs); return true }
        if (text == lastText && nowMs - lastTime < repeatCooldownMs) return false
        if (nowMs - lastTime < minIntervalMs) return false
        record(text, nowMs)
        return true
    }

    private fun record(text: String, nowMs: Long) { lastText = text; lastTime = nowMs }

    fun reset() { lastText = null; lastTime = Long.MIN_VALUE / 2 }
}
