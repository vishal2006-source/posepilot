package com.posepilot.app

import com.posepilot.app.pose.analysis.PoseFeature
import com.posepilot.app.pose.rules.CorrectionDirection
import com.posepilot.app.pose.rules.PoseRule
import com.posepilot.app.pose.rules.RuleEngine
import com.posepilot.app.pose.rules.RuleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    private val rule = PoseRule(PoseFeature.RIGHT_ELBOW_ANGLE, targetValue = 90.0, tolerance = 15.0)

    @Test fun tooStraightElbowSaysBend() {
        val r = RuleEngine.evaluate(rule, 130.0)
        assertEquals(RuleStatus.OFF, r.status)
        assertEquals(CorrectionDirection.DECREASE, r.direction)
        assertEquals("Bend your right elbow", RuleEngine.instructionFor(r))
    }

    @Test fun tooBentElbowSaysStraighten() {
        val r = RuleEngine.evaluate(rule, 50.0)
        assertEquals(CorrectionDirection.INCREASE, r.direction)
        assertEquals("Straighten your right arm", RuleEngine.instructionFor(r))
    }

    @Test fun closeAndPerfectBands() {
        assertEquals(RuleStatus.CLOSE, RuleEngine.evaluate(rule, 80.0).status)
        assertEquals(RuleStatus.PERFECT, RuleEngine.evaluate(rule, 92.0).status)
        assertNull(RuleEngine.instructionFor(RuleEngine.evaluate(rule, 92.0)))
    }

    @Test fun unknownWhenMissing() {
        assertEquals(RuleStatus.UNKNOWN, RuleEngine.evaluate(rule, null).status)
    }

    @Test fun scoreDecreasesMonotonically() {
        var prev = 2.0
        var e = 0.0
        while (e <= 70.0) {
            val s = RuleEngine.scoreFor(e, 15.0)
            assertTrue("score must not increase at e=$e", s <= prev + 1e-12)
            prev = s
            e += 0.5
        }
        assertEquals(1.0, RuleEngine.scoreFor(0.0, 15.0), 1e-12)
        assertEquals(0.85, RuleEngine.scoreFor(15.0, 15.0), 1e-12)
        assertEquals(0.0, RuleEngine.scoreFor(60.0, 15.0), 1e-12)
    }

    @Test fun neutralTargetUsesNeutralInstruction() {
        val head = PoseRule(PoseFeature.HEAD_TURN, 0.0, 15.0)
        assertEquals("Look at the camera", RuleEngine.instructionFor(RuleEngine.evaluate(head, 40.0)))
        val away = PoseRule(PoseFeature.HEAD_TURN, 40.0, 15.0)
        assertEquals("Turn your face to your left", RuleEngine.instructionFor(RuleEngine.evaluate(away, 0.0)))
    }
}
