package com.posepilot.app

import com.posepilot.app.utilities.OverlayTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayTransformTest {
    @Test fun fillCenterCropsWiderAxis() {
        // 480x640 image into a 1080x2340 view: height-limited, image is scaled by 2340/640 and cropped left/right.
        val t = OverlayTransform(480, 640, 1080f, 2340f, mirror = false)
        val scale = 2340f / 640f
        assertEquals(1080f / 2, t.x(240.0), 0.01f)
        assertEquals(0f, t.y(0.0), 0.01f)
        assertEquals(scale * 100, t.length(100.0), 0.01f)
    }

    @Test fun mirrorFlipsX() {
        val plain = OverlayTransform(480, 640, 1080f, 1440f, mirror = false)
        val mirrored = OverlayTransform(480, 640, 1080f, 1440f, mirror = true)
        assertEquals(1080f - plain.x(100.0), mirrored.x(100.0), 0.01f)
        assertEquals(plain.y(100.0), mirrored.y(100.0), 0.01f)
    }

    @Test fun fitLetterboxes() {
        val t = OverlayTransform(1000, 1000, 500f, 1000f, mirror = false, fit = true)
        assertEquals(250f, t.y(0.0), 0.01f)
        assertEquals(500f, t.x(1000.0), 0.01f)
    }
}
