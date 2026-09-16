package com.posepilot.app

import com.posepilot.app.utilities.AngleMath
import com.posepilot.app.utilities.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AngleMathTest {
    @Test fun rightAngleElbow() {
        val shoulder = Vec2(0.0, 0.0); val elbow = Vec2(0.0, 10.0); val wrist = Vec2(10.0, 10.0)
        assertEquals(90.0, AngleMath.jointAngle(shoulder, elbow, wrist), 1e-9)
    }

    @Test fun straightArmIs180() {
        assertEquals(180.0, AngleMath.jointAngle(Vec2(0.0, 0.0), Vec2(0.0, 5.0), Vec2(0.0, 10.0)), 1e-9)
    }

    @Test fun angleIsScaleInvariant() {
        val a = AngleMath.jointAngle(Vec2(1.0, 2.0), Vec2(3.0, 5.0), Vec2(7.0, 4.0))
        val b = AngleMath.jointAngle(Vec2(10.0, 20.0), Vec2(30.0, 50.0), Vec2(70.0, 40.0))
        assertEquals(a, b, 1e-9)
    }

    @Test fun degenerateSegmentIsNaN() {
        assertTrue(AngleMath.jointAngle(Vec2(1.0, 1.0), Vec2(1.0, 1.0), Vec2(2.0, 2.0)).isNaN())
    }
}
