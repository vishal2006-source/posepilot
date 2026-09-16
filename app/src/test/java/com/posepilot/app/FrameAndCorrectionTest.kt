package com.posepilot.app

import com.posepilot.app.guidance.CorrectionEngine
import com.posepilot.app.models.FramingType
import com.posepilot.app.pose.analysis.FrameAnalyzer
import com.posepilot.app.pose.analysis.FrameIssueType
import com.posepilot.app.targetpose.PoseBuilder
import com.posepilot.app.targetpose.PoseBuilder.Arm
import com.posepilot.app.targetpose.PoseLibrary
import com.posepilot.app.targetpose.TemplateFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameAndCorrectionTest {
    private val neutral = PoseLibrary.byId("neutral-standing")!!
    private val fullTarget = TemplateFactory.defaultFrameTarget(FramingType.FULL_BODY)

    @Test fun centeredFullBodyHasNoIssues() {
        val s = FrameAnalyzer.analyze(TestPoses.toFrame(neutral.targetPose), FramingType.FULL_BODY, fullTarget)
        assertTrue("unexpected: ${s.issues}", s.issues.isEmpty())
        assertEquals(0.5, s.centerX, 0.01)
    }

    @Test fun personOnImageLeftMustStepToTheirLeft() {
        val s = FrameAnalyzer.analyze(TestPoses.toFrame(neutral.targetPose, hipX = 150.0), FramingType.FULL_BODY, fullTarget)
        assertTrue(s.issues.any { it.type == FrameIssueType.MOVE_TO_SUBJECT_LEFT })
        assertTrue(s.issues.first { it.type == FrameIssueType.MOVE_TO_SUBJECT_LEFT }.direction.x > 0)
    }

    @Test fun feetBelowFrameAreCutOff() {
        val s = FrameAnalyzer.analyze(TestPoses.toFrame(neutral.targetPose, hipY = 1000.0), FramingType.FULL_BODY, fullTarget)
        assertTrue(s.issues.any { it.type == FrameIssueType.FEET_CUT_OFF })
    }

    @Test fun tooFarAndTooClose() {
        val far = FrameAnalyzer.analyze(TestPoses.toFrame(neutral.targetPose, torsoPx = 120.0), FramingType.FULL_BODY, fullTarget)
        assertTrue(far.issues.any { it.type == FrameIssueType.TOO_FAR })
        val close = FrameAnalyzer.analyze(TestPoses.toFrame(neutral.targetPose, torsoPx = 360.0, hipY = 560.0), FramingType.FULL_BODY, fullTarget)
        assertTrue("issues: ${close.issues} fraction ${close.bodyFraction}", close.issues.any { it.type == FrameIssueType.TOO_CLOSE })
    }

    @Test fun framingCorrectionOutranksLimbCorrection() {
        val t = PoseLibrary.byId("one-hand-raised")!!
        val c = CorrectionEngine.analyze(TestPoses.toFrame(neutral.targetPose, hipX = 150.0), t)
        assertEquals("Step to your left", c.corrections.first().instruction)
        assertTrue(c.corrections.any { it.instruction == "Raise your right arm" })
    }

    @Test fun limbArrowPointsTowardGhost() {
        val t = PoseLibrary.byId("one-hand-raised")!!
        val c = CorrectionEngine.analyze(TestPoses.toFrame(neutral.targetPose), t)
        val raise = c.corrections.first { it.instruction == "Raise your right arm" }
        val arrow = raise.arrow!!
        assertTrue("arrow should point up the image", arrow.to.y < arrow.from.y)
        assertTrue(c.ghost.isNotEmpty())
    }

    @Test fun exactPoseHasNoCorrections() {
        val t = PoseLibrary.byId("hand-on-waist")!!
        val c = CorrectionEngine.analyze(TestPoses.toFrame(t.targetPose), t)
        assertTrue("got ${c.corrections.map { it.instruction }}", c.corrections.isEmpty())
        assertTrue(c.score > 0.99)
    }

    @Test fun onlyArmWrongGivesArmInstructionFirst() {
        val t = PoseLibrary.byId("hand-on-waist")!!
        val live = PoseBuilder.pose { bodyTurn = 15.0; leftArm = Arm(raise = 8.0, elbow = 170.0); leftLeg = PoseBuilder.Leg(6.0); rightLeg = PoseBuilder.Leg(6.0, 168.0) }
        val c = CorrectionEngine.analyze(TestPoses.toFrame(live), t)
        assertEquals("Raise your left arm", c.corrections.first().instruction)
    }
}
