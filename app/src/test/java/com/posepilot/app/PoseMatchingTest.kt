package com.posepilot.app

import com.posepilot.app.models.BodyRegion
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.pose.analysis.FeatureExtractor
import com.posepilot.app.pose.landmarks.PoseNormalizer
import com.posepilot.app.pose.matching.PoseMatcher
import com.posepilot.app.targetpose.PoseBuilder
import com.posepilot.app.targetpose.PoseBuilder.Arm
import com.posepilot.app.targetpose.PoseLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseMatchingTest {
    private fun liveFeatures(frame: com.posepilot.app.models.PoseFrame) =
        FeatureExtractor.extract(PoseNormalizer.normalize(frame)!!)

    @Test fun everyLibraryPoseMatchesItselfAtAnyScale() {
        for (t in PoseLibrary.all) {
            val frame = TestPoses.toFrame(t.targetPose, torsoPx = 150.0, hipX = 200.0)
            val m = PoseMatcher.match(t, liveFeatures(frame))
            assertTrue("${t.id} self-match was ${m.overall}", m.overall > 0.99)
            assertTrue("${t.id} should have rules", t.rules.size >= 5)
        }
    }

    @Test fun libraryHasTenDistinctPoses() {
        assertEquals(10, PoseLibrary.all.size)
        val neutral = PoseLibrary.byId("neutral-standing")!!
        for (t in PoseLibrary.all.filter { it.id != neutral.id }) {
            val m = PoseMatcher.match(t, liveFeatures(TestPoses.toFrame(neutral.targetPose)))
            assertTrue("${t.id} should differ from neutral standing (score ${m.overall})", m.offRules.isNotEmpty())
        }
    }

    @Test fun wrongArmLowersOnlyThatRegion() {
        val target = PoseLibrary.byId("one-hand-raised")!!
        val live = PoseBuilder.pose { rightArm = Arm(raise = 20.0, elbow = 170.0); leftLeg = PoseBuilder.Leg(6.0); rightLeg = PoseBuilder.Leg(6.0) }
        val m = PoseMatcher.match(target, liveFeatures(TestPoses.toFrame(live)))
        val right = m.regionScores.first { it.region == BodyRegion.RIGHT_ARM }
        val left = m.regionScores.first { it.region == BodyRegion.LEFT_ARM }
        assertTrue("right arm region ${right.score}", right.score < 0.5)
        assertTrue(left.score > 0.9)
        assertTrue("overall ${m.overall}", m.overall < 0.8)
    }

    @Test fun missingLegsReduceCoverageAndScore() {
        val t = PoseLibrary.byId("neutral-standing")!!
        val hidden = setOf(LandmarkType.LEFT_KNEE, LandmarkType.RIGHT_KNEE, LandmarkType.LEFT_ANKLE, LandmarkType.RIGHT_ANKLE)
        val m = PoseMatcher.match(t, liveFeatures(TestPoses.toFrame(t.targetPose, hidden = hidden)))
        assertTrue(m.coverage < 1.0)
        assertTrue("half-visible person must not score 100% (was ${m.overall})", m.overall < 0.95)
    }
}
