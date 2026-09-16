package com.posepilot.app.targetpose

import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.LandmarkType.*
import com.posepilot.app.pose.landmarks.NormalizedPose
import com.posepilot.app.utilities.AngleMath
import com.posepilot.app.utilities.Vec2
import com.posepilot.app.utilities.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Forward-kinematics helper for authoring library poses from human-readable joint angles,
 * using average adult proportions expressed in torso lengths.
 *
 * All angles in degrees, subject facing the camera. The subject's left side is +x.
 */
class PoseBuilder {
    /** Which way a bent forearm / shin swings. */
    enum class Bend { TOWARD_MIDLINE, OUTWARD, UP, DOWN }

    data class Arm(val raise: Double = 8.0, val elbow: Double = 170.0, val bend: Bend = Bend.TOWARD_MIDLINE)
    data class Leg(val spread: Double = 4.0, val knee: Double = 178.0, val bend: Bend = Bend.TOWARD_MIDLINE)

    var bodyTurn = 0.0
    var torsoLean = 0.0
    var shoulderTilt = 0.0
    var headTurn = 0.0
    var headTilt = 0.0
    var chinUp = 0.0
    var leftArm = Arm()
    var rightArm = Arm()
    var leftLeg = Leg()
    var rightLeg = Leg()

    fun build(): NormalizedPose {
        val pts = HashMap<LandmarkType, Vec3>()
        val hipMid = Vec2(0.0, 0.0)
        // Torso: unit length, leaning by torsoLean from vertical.
        val up = AngleMath.rotate(Vec2(0.0, -1.0), torsoLean)
        val shoulderMid = hipMid + up

        val yaw = AngleMath.toRadians(bodyTurn)
        val halfShoulder = SHOULDER_WIDTH / 2
        val halfHip = HIP_WIDTH / 2
        val tiltDy = halfShoulder * sin(AngleMath.toRadians(shoulderTilt))
        pts[LEFT_SHOULDER] = Vec3(shoulderMid.x + halfShoulder * cos(yaw), shoulderMid.y + tiltDy, halfShoulder * sin(yaw))
        pts[RIGHT_SHOULDER] = Vec3(shoulderMid.x - halfShoulder * cos(yaw), shoulderMid.y - tiltDy, -halfShoulder * sin(yaw))
        pts[LEFT_HIP] = Vec3(halfHip * cos(yaw), 0.0, halfHip * sin(yaw))
        pts[RIGHT_HIP] = Vec3(-halfHip * cos(yaw), 0.0, -halfHip * sin(yaw))

        // Arm raise is defined relative to the same-side shoulder->hip line (exactly what FeatureExtractor measures).
        val leftDown = (pts.getValue(LEFT_HIP).xy() - pts.getValue(LEFT_SHOULDER).xy()).normalized()
        val rightDown = (pts.getValue(RIGHT_HIP).xy() - pts.getValue(RIGHT_SHOULDER).xy()).normalized()
        addArm(pts, LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, leftArm, side = +1, down = leftDown, midX = shoulderMid.x)
        addArm(pts, RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, rightArm, side = -1, down = rightDown, midX = shoulderMid.x)
        addLeg(pts, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE, LEFT_FOOT_INDEX, leftLeg, side = +1)
        addLeg(pts, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE, RIGHT_FOOT_INDEX, rightLeg, side = -1)
        addHead(pts, shoulderMid, up)

        return NormalizedPose(pts, pts.keys.toSet(), Vec2.ZERO, 1.0)
    }

    private fun addArm(
        pts: MutableMap<LandmarkType, Vec3>, s: LandmarkType, e: LandmarkType, w: LandmarkType,
        arm: Arm, side: Int, down: Vec2, midX: Double,
    ) {
        val shoulder = pts.getValue(s).xy()
        // Rotate "down along the torso" outward toward the arm's own side by `raise` degrees.
        val upper = rotateToward(down, arm.raise, side)
        val elbow = shoulder + upper * UPPER_ARM
        val fore = bendSegment(upper, 180.0 - arm.elbow, arm.bend, elbow, FOREARM, midX)
        val wrist = elbow + fore * FOREARM
        pts[e] = Vec3(elbow.x, elbow.y)
        pts[w] = Vec3(wrist.x, wrist.y)
    }

    private fun addLeg(
        pts: MutableMap<LandmarkType, Vec3>, h: LandmarkType, k: LandmarkType, a: LandmarkType, foot: LandmarkType,
        leg: Leg, side: Int,
    ) {
        val hip = pts.getValue(h).xy()
        val thigh = rotateToward(Vec2(0.0, 1.0), leg.spread, side)
        val knee = hip + thigh * THIGH
        val shinDir = bendSegment(thigh, 180.0 - leg.knee, leg.bend, knee, SHIN, 0.0)
        val ankle = knee + shinDir * SHIN
        pts[k] = Vec3(knee.x, knee.y)
        pts[a] = Vec3(ankle.x, ankle.y)
        pts[foot] = Vec3(ankle.x + side * 0.08, ankle.y + 0.08)
    }

    private fun addHead(pts: MutableMap<LandmarkType, Vec3>, shoulderMid: Vec2, up: Vec2) {
        val center = shoulderMid + up * NECK
        val yaw = AngleMath.toRadians(headTurn)
        fun place(dx: Double, dy: Double): Vec3 {
            val p = AngleMath.rotate(Vec2(dx, dy), headTilt)
            return Vec3(center.x + p.x, center.y + p.y)
        }
        // Ears sit on the sides of the head; the nose sits in front and swings with yaw.
        pts[LEFT_EAR] = place(HEAD_RADIUS * cos(yaw), 0.0)
        pts[RIGHT_EAR] = place(-HEAD_RADIUS * cos(yaw), 0.0)
        pts[NOSE] = place(HEAD_RADIUS * sin(yaw), 0.02 - chinUp)
        pts[LEFT_EYE] = place(0.045 * cos(yaw) + 0.08 * sin(yaw), -0.04)
        pts[RIGHT_EYE] = place(-0.045 * cos(yaw) + 0.08 * sin(yaw), -0.04)
    }

    /** Rotate [v] by [deg] toward the given body side (+1 = subject's left = +x, -1 = right). */
    private fun rotateToward(v: Vec2, deg: Double, side: Int): Vec2 {
        // A negative angle means "toward the other side" (e.g. a leg crossing over).
        val effectiveSide = if (deg >= 0) side else -side
        val a = AngleMath.rotate(v, abs(deg))
        val b = AngleMath.rotate(v, -abs(deg))
        return if ((a.x - b.x) * effectiveSide >= 0) a else b
    }

    /** Rotate the parent segment direction by [deg], choosing the swing direction that satisfies [bend]. */
    private fun bendSegment(parent: Vec2, deg: Double, bend: Bend, joint: Vec2, length: Double, midX: Double): Vec2 {
        val a = AngleMath.rotate(parent, deg)
        val b = AngleMath.rotate(parent, -deg)
        val endA = joint + a * length
        val endB = joint + b * length
        val pickA = when (bend) {
            Bend.TOWARD_MIDLINE -> abs(endA.x - midX) <= abs(endB.x - midX)
            Bend.OUTWARD -> abs(endA.x - midX) > abs(endB.x - midX)
            Bend.UP -> endA.y <= endB.y
            Bend.DOWN -> endA.y > endB.y
        }
        return if (pickA) a else b
    }

    companion object {
        const val SHOULDER_WIDTH = 0.72
        const val HIP_WIDTH = 0.46
        const val UPPER_ARM = 0.58
        const val FOREARM = 0.50
        const val THIGH = 0.95
        const val SHIN = 0.90
        const val NECK = 0.38
        const val HEAD_RADIUS = 0.13

        fun pose(block: PoseBuilder.() -> Unit): NormalizedPose = PoseBuilder().apply(block).build()
    }
}
