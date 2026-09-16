package com.posepilot.app

import com.posepilot.app.guidance.CorrectionEngine
import com.posepilot.app.guidance.GuidanceConfig
import com.posepilot.app.guidance.GuidanceEngine
import com.posepilot.app.guidance.SpeechScheduler
import com.posepilot.app.models.GuidancePhase
import com.posepilot.app.models.PoseComparison
import com.posepilot.app.targetpose.PoseLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidanceEngineTest {
    private val template = PoseLibrary.byId("hands-in-pockets")!!
    private val perfect = CorrectionEngine.analyze(TestPoses.toFrame(template.targetPose), template)
    private val neutralFrame = CorrectionEngine.analyze(TestPoses.toFrame(PoseLibrary.byId("one-hand-raised")!!.targetPose), template)

    @Test fun holdCountdownCapture() {
        val engine = GuidanceEngine(GuidanceConfig(autoCapture = true, holdMs = 1000, countdownSeconds = 3))
        var t = 0L
        var captures = 0
        val phases = mutableListOf<GuidancePhase>()
        val spoken = mutableListOf<String>()
        while (t <= 6000) {
            val u = engine.update(perfect, t)
            if (u.captureNow) captures++
            u.speech?.let { spoken += it }
            phases += engine.currentPhase
            t += 33
        }
        assertEquals(1, captures)
        assertTrue(GuidancePhase.HOLD in phases)
        assertTrue(GuidancePhase.COUNTDOWN in phases)
        assertEquals(GuidancePhase.CAPTURED, engine.currentPhase)
        assertEquals(listOf("Perfect. Hold.", "Three", "Two", "One", "Capture"), spoken)
    }

    @Test fun noAutoCaptureStaysReady() {
        val engine = GuidanceEngine(GuidanceConfig(autoCapture = false))
        var t = 0L
        while (t < 5000) { assertFalse(engine.update(perfect, t).captureNow); t += 50 }
        assertEquals(GuidancePhase.HOLD, engine.currentPhase)
    }

    @Test fun breakingPoseCancelsCountdown() {
        val engine = GuidanceEngine(GuidanceConfig(holdMs = 200))
        var t = 0L
        while (t < 800) { engine.update(perfect, t); t += 50 }
        assertEquals(GuidancePhase.COUNTDOWN, engine.currentPhase)
        engine.update(neutralFrame, t)
        assertEquals(GuidancePhase.GUIDING, engine.currentPhase)
    }

    @Test fun showsOneInstructionAndSpeaksOnce() {
        val engine = GuidanceEngine()
        var spokenCount = 0
        var t = 0L
        var text = ""
        while (t < 2000) {
            val u = engine.update(neutralFrame, t)
            text = u.instruction.text
            if (u.speech != null) spokenCount++
            t += 33
        }
        assertEquals(neutralFrame.corrections.first().instruction, text)
        assertEquals(1, spokenCount)
    }

    @Test fun noPersonAfterDelay() {
        val engine = GuidanceEngine()
        engine.update(neutralFrame, 0)
        engine.update(PoseComparison.EMPTY, 300)
        assertEquals(GuidancePhase.GUIDING, engine.currentPhase)
        val u = engine.update(PoseComparison.EMPTY, 1000)
        assertEquals(GuidancePhase.NO_PERSON, engine.currentPhase)
        assertTrue(u.instruction.text.startsWith("No person"))
    }

    @Test fun manualInstructionOverridesThenExpires() {
        val engine = GuidanceEngine()
        engine.pushManual("Smile", 0)
        val u = engine.update(neutralFrame, 10)
        assertEquals("Smile", u.instruction.text)
        assertEquals("Smile", u.speech)
        assertTrue(engine.update(neutralFrame, 3000).instruction.text != "Smile")
    }

    @Test fun speechSchedulerThrottles() {
        val s = SpeechScheduler(minIntervalMs = 2000, repeatCooldownMs = 5000)
        assertTrue(s.shouldSpeak("Raise your right arm", 0))
        assertFalse(s.shouldSpeak("Raise your right arm", 1000))
        assertFalse(s.shouldSpeak("Step to your left", 1500))
        assertTrue(s.shouldSpeak("Step to your left", 2100))
        assertFalse(s.shouldSpeak("Step to your left", 4500))
        assertTrue(s.shouldSpeak("Three", 4600, force = true))
    }
}
