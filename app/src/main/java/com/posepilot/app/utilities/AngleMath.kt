package com.posepilot.app.utilities

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object AngleMath {
    fun toDegrees(rad: Double) = rad * 180.0 / PI
    fun toRadians(deg: Double) = deg * PI / 180.0

    /**
     * Interior angle at [vertex] formed by [a]-[vertex]-[b], in degrees (0..180).
     * Example: shoulder-elbow-wrist gives the elbow angle; 180 = straight arm.
     */
    fun jointAngle(a: Vec2, vertex: Vec2, b: Vec2): Double {
        val v1 = a - vertex
        val v2 = b - vertex
        val denom = v1.length() * v2.length()
        if (denom < 1e-9) return Double.NaN
        val c = (v1.dot(v2) / denom).coerceIn(-1.0, 1.0)
        return toDegrees(acos(c))
    }

    /** Signed angle of vector [v] from the +x axis, degrees in (-180, 180]. y is down. */
    fun heading(v: Vec2): Double = toDegrees(atan2(v.y, v.x))

    /** Rotate [v] by [deg] degrees with the standard rotation matrix. */
    fun rotate(v: Vec2, deg: Double): Vec2 {
        val r = toRadians(deg)
        return Vec2(v.x * cos(r) - v.y * sin(r), v.x * sin(r) + v.y * cos(r))
    }
}
