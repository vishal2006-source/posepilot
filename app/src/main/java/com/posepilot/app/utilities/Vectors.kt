package com.posepilot.app.utilities

import kotlin.math.sqrt

/** Minimal immutable 2D vector used by the pose engine. Image convention: x right, y down. */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Double) = Vec2(x * s, y * s)
    operator fun div(s: Double) = Vec2(x / s, y / s)
    fun length(): Double = sqrt(x * x + y * y)
    fun distanceTo(o: Vec2): Double = (this - o).length()
    fun dot(o: Vec2): Double = x * o.x + y * o.y
    fun normalized(): Vec2 { val l = length(); return if (l < 1e-9) Vec2(0.0, 0.0) else this / l }

    companion object { val ZERO = Vec2(0.0, 0.0) }
}

/** 3D point. z follows ML Kit: smaller z = closer to the camera, roughly the same scale as x. */
data class Vec3(val x: Double, val y: Double, val z: Double = 0.0) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Double) = Vec3(x / s, y / s, z / s)
    fun xy(): Vec2 = Vec2(x, y)

    companion object {
        fun midpoint(a: Vec3, b: Vec3) = Vec3((a.x + b.x) / 2, (a.y + b.y) / 2, (a.z + b.z) / 2)
    }
}
