package com.posepilot.app

import com.posepilot.app.pose.analysis.FeatureExtractor
import com.posepilot.app.pose.analysis.PoseFeature
import com.posepilot.app.pose.landmarks.PoseNormalizer
import com.posepilot.app.targetpose.PoseBuilder
import com.posepilot.app.targetpose.PoseBuilder.Arm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureExtractorTest {
    private fun features(block: PoseBuilder.() -> Unit) =
        FeatureExtractor.extract(PoseNormalizer.normalize(TestPoses.toFrame(PoseBuilder.pose(block)))!!)

    @Test fun authoredArmAnglesAreMeasuredBack() {
        val f = features { rightArm = Arm(raise = 90.0, elbow = 90.0, bend = PoseBuilder.Bend.UP); leftArm = Arm(raise = 30.0, elbow = 150.0) }
        assertEquals(90.0, f[PoseFeature.RIGHT_ARM_RAISE]!!, 0.5)
        assertEquals(90.0, f[PoseFeature.RIGHT_ELBOW_ANGLE]!!, 0.5)
        assertEquals(30.0, f[PoseFeature.LEFT_ARM_RAISE]!!, 0.5)
        assertEquals(150.0, f[PoseFeature.LEFT_ELBOW_ANGLE]!!, 0.5)
    }

    @Test fun kneeAngleMeasured() {
        val f = features { leftLeg = PoseBuilder.Leg(knee = 150.0) }
        assertEquals(150.0, f[PoseFeature.LEFT_KNEE_ANGLE]!!, 0.5)
    }

    @Test fun bodyTurnSign() {
        assertEquals(30.0, features { bodyTurn = 30.0 }[PoseFeature.BODY_TURN]!!, 0.5)
        assertEquals(-30.0, features { bodyTurn = -30.0 }[PoseFeature.BODY_TURN]!!, 0.5)
    }

    @Test fun headTurnSignAndNeutral() {
        assertEquals(0.0, features { }[PoseFeature.HEAD_TURN]!!, 0.5)
        assertTrue(features { headTurn = 30.0 }[PoseFeature.HEAD_TURN]!! > 20.0)
        assertTrue(features { headTurn = -30.0 }[PoseFeature.HEAD_TURN]!! < -20.0)
    }

    @Test fun leanAndTiltSigns() {
        assertEquals(10.0, features { torsoLean = 10.0 }[PoseFeature.TORSO_LEAN]!!, 0.5)
        assertTrue(features { shoulderTilt = 8.0 }[PoseFeature.SHOULDER_TILT]!! > 5.0)
        assertTrue(features { headTilt = 10.0 }[PoseFeature.HEAD_TILT]!! > 5.0)
    }

    @Test fun missingLandmarksOmitFeature() {
        val frame = TestPoses.toFrame(PoseBuilder.pose { }, hidden = setOf(com.posepilot.app.models.LandmarkType.LEFT_ANKLE))
        val f = FeatureExtractor.extract(PoseNormalizer.normalize(frame)!!)
        assertFalse(f.containsKey(PoseFeature.STANCE_WIDTH))
        assertFalse(f.containsKey(PoseFeature.LEFT_KNEE_ANGLE))
        assertTrue(f.containsKey(PoseFeature.RIGHT_KNEE_ANGLE))
    }
}
