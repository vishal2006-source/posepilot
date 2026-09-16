package com.posepilot.app

import com.posepilot.app.guidance.CorrectionEngine
import com.posepilot.app.models.FramingType
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.targetpose.PoseBuilder
import com.posepilot.app.targetpose.PoseBuilder.Arm
import com.posepilot.app.targetpose.ReferencePoseFactory
import com.posepilot.app.targetpose.ReferenceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferencePoseTest {
    private val refPose = PoseBuilder.pose {
        bodyTurn = 20.0; headTurn = -10.0
        leftArm = Arm(raise = 45.0, elbow = 95.0)
        rightLeg = PoseBuilder.Leg(10.0, 160.0)
    }

    @Test fun extractsFullBodyTemplateAndLiveCopyMatches() {
        val result = ReferencePoseFactory.fromFrame(TestPoses.toFrame(refPose, torsoPx = 200.0), "ref-1")
        assertTrue(result is ReferenceResult.Success)
        val template = (result as ReferenceResult.Success).template
        assertEquals(FramingType.FULL_BODY, template.framing)
        // Someone recreating it at a different distance should match.
        val live = CorrectionEngine.analyze(TestPoses.toFrame(refPose, torsoPx = 280.0), template)
        assertTrue(live.score > 0.98)
        assertTrue(live.corrections.isEmpty())
    }

    @Test fun upperBodyOnlyReference() {
        val hidden = setOf(LandmarkType.LEFT_KNEE, LandmarkType.RIGHT_KNEE, LandmarkType.LEFT_ANKLE, LandmarkType.RIGHT_ANKLE)
        val result = ReferencePoseFactory.fromFrame(TestPoses.toFrame(refPose, hidden = hidden), "ref-2") as ReferenceResult.Success
        assertEquals(FramingType.UPPER_BODY, result.template.framing)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test fun noPersonFails() {
        assertTrue(ReferencePoseFactory.fromFrame(null, "x") is ReferenceResult.Failure)
    }

    @Test fun multipleFacesWarns() {
        val r = ReferencePoseFactory.fromFrame(TestPoses.toFrame(refPose), "x", facesDetected = 3) as ReferenceResult.Success
        assertTrue(r.warnings.any { it.contains("More than one person") })
    }

    @Test fun mirroredTemplateMatchesMirroredPerson() {
        val template = (ReferencePoseFactory.fromFrame(TestPoses.toFrame(refPose), "ref") as ReferenceResult.Success).template
        val flipped = template.mirrored()
        val mirroredLive = TestPoses.toFrame(refPose.mirrored())
        assertTrue(CorrectionEngine.analyze(mirroredLive, flipped).score > 0.98)
        assertTrue(CorrectionEngine.analyze(mirroredLive, template).score < 0.9)
    }
}
