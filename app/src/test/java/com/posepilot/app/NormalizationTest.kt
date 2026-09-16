package com.posepilot.app

import com.posepilot.app.models.LandmarkType
import com.posepilot.app.pose.landmarks.PoseNormalizer
import com.posepilot.app.targetpose.PoseBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizationTest {
    private val pose = PoseBuilder.pose { rightArm = PoseBuilder.Arm(raise = 90.0, elbow = 90.0) }

    @Test fun shortAndTallPeopleNormalizeIdentically() {
        val near = PoseNormalizer.normalize(TestPoses.toFrame(pose, torsoPx = 400.0, hipX = 300.0, hipY = 700.0))!!
        val far = PoseNormalizer.normalize(TestPoses.toFrame(pose, torsoPx = 120.0, hipX = 500.0, hipY = 500.0))!!
        for (t in listOf(LandmarkType.RIGHT_WRIST, LandmarkType.NOSE, LandmarkType.LEFT_ANKLE)) {
            assertEquals(near[t]!!.x, far[t]!!.x, 1e-9)
            assertEquals(near[t]!!.y, far[t]!!.y, 1e-9)
        }
    }

    @Test fun torsoLengthIsUnit() {
        val n = PoseNormalizer.normalize(TestPoses.toFrame(pose, torsoPx = 333.0))!!
        assertEquals(333.0, n.scalePx, 1e-6)
    }

    @Test fun hiddenShouldersGiveNull() {
        val frame = TestPoses.toFrame(pose, hidden = setOf(LandmarkType.LEFT_SHOULDER))
        assertNull(PoseNormalizer.normalize(frame))
    }

    @Test fun mirroredSwapsSides() {
        val n = PoseNormalizer.normalize(TestPoses.toFrame(pose))!!
        val m = n.mirrored()
        assertNotNull(m[LandmarkType.LEFT_WRIST])
        assertEquals(-n[LandmarkType.RIGHT_WRIST]!!.x, m[LandmarkType.LEFT_WRIST]!!.x, 1e-9)
        assertEquals(n[LandmarkType.RIGHT_WRIST]!!.y, m[LandmarkType.LEFT_WRIST]!!.y, 1e-9)
    }
}
