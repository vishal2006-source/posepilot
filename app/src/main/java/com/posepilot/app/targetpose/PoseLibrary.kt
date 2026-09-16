package com.posepilot.app.targetpose

import com.posepilot.app.models.FramingType
import com.posepilot.app.pose.analysis.PoseFeature
import com.posepilot.app.pose.analysis.PoseFeature.*
import com.posepilot.app.targetpose.PoseBuilder.Arm
import com.posepilot.app.targetpose.PoseBuilder.Bend
import com.posepilot.app.targetpose.PoseBuilder.Leg

/**
 * Built-in offline pose library. Each pose is authored as joint angles and measured by the same
 * FeatureExtractor used on the live camera, so the target values are real geometry.
 */
object PoseLibrary {

    private val ARMS: Set<PoseFeature> = setOf(
        LEFT_ARM_RAISE, RIGHT_ARM_RAISE, LEFT_ELBOW_ANGLE, RIGHT_ELBOW_ANGLE, LEFT_HAND_REACH, RIGHT_HAND_REACH,
    )
    private val HEAD: Set<PoseFeature> = setOf(HEAD_TURN, HEAD_TILT, CHIN_HEIGHT)
    private val TORSO: Set<PoseFeature> = setOf(BODY_TURN, TORSO_LEAN, SHOULDER_TILT)
    private val LEGS: Set<PoseFeature> = setOf(STANCE_WIDTH, LEFT_KNEE_ANGLE, RIGHT_KNEE_ANGLE)

    val all: List<PoseTemplate> by lazy {
        listOf(
            TemplateFactory.create(
                id = "neutral-standing", name = "Neutral standing", category = PoseCategory.CASUAL,
                description = "Relaxed, straight posture facing the camera. A good warm-up pose.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    leftLeg = Leg(spread = 5.0); rightLeg = Leg(spread = 5.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                tips = listOf("Relax your shoulders", "Weight on both feet"),
            ),
            TemplateFactory.create(
                id = "hands-in-pockets", name = "Hands in pockets", category = PoseCategory.CASUAL,
                description = "Both hands in front pockets, slight turn, easy confidence.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    bodyTurn = 15.0
                    leftArm = Arm(raise = 14.0, elbow = 145.0, bend = Bend.TOWARD_MIDLINE)
                    rightArm = Arm(raise = 14.0, elbow = 145.0, bend = Bend.TOWARD_MIDLINE)
                    leftLeg = Leg(spread = 6.0); rightLeg = Leg(spread = 6.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                // Wrists are often hidden inside pockets, so arm rules are weighted a little lower.
                weightOverrides = mapOf(LEFT_HAND_REACH to 0.5, RIGHT_HAND_REACH to 0.5),
                tips = listOf("Thumbs can stay outside the pocket", "Keep elbows relaxed"),
            ),
            TemplateFactory.create(
                id = "one-hand-raised", name = "One hand raised", category = PoseCategory.SOCIAL,
                description = "Right hand raised high, like a wave or celebration.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    rightArm = Arm(raise = 155.0, elbow = 160.0, bend = Bend.TOWARD_MIDLINE)
                    leftLeg = Leg(spread = 6.0); rightLeg = Leg(spread = 6.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                tips = listOf("Open palm toward the camera", "Big smile"),
            ),
            TemplateFactory.create(
                id = "crossed-arms", name = "Crossed arms", category = PoseCategory.FORMAL,
                description = "Arms folded across the chest. Confident and professional.",
                framing = FramingType.UPPER_BODY,
                pose = PoseBuilder.pose {
                    bodyTurn = 10.0
                    leftArm = Arm(raise = 10.0, elbow = 55.0, bend = Bend.TOWARD_MIDLINE)
                    rightArm = Arm(raise = 10.0, elbow = 55.0, bend = Bend.TOWARD_MIDLINE)
                },
                features = TORSO + ARMS + HEAD,
                toleranceOverrides = mapOf(LEFT_ELBOW_ANGLE to 28.0, RIGHT_ELBOW_ANGLE to 28.0),
                tips = listOf("Don't squeeze too tight", "Chin level"),
            ),
            TemplateFactory.create(
                id = "casual-side", name = "Casual side pose", category = PoseCategory.SOCIAL,
                description = "Body turned to the side, face back toward the camera.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    bodyTurn = 40.0
                    headTurn = -30.0
                    leftArm = Arm(raise = 6.0, elbow = 165.0)
                    rightArm = Arm(raise = 10.0, elbow = 150.0)
                    leftLeg = Leg(spread = 3.0)
                    rightLeg = Leg(spread = 7.0, knee = 165.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                toleranceOverrides = mapOf(BODY_TURN to 25.0),
                tips = listOf("Shift weight to your back leg"),
            ),
            TemplateFactory.create(
                id = "looking-away", name = "Looking away", category = PoseCategory.TRAVEL,
                description = "Face turned away from the camera, as if looking at the view.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    headTurn = 40.0
                    chinUp = 0.03
                    leftLeg = Leg(spread = 5.0); rightLeg = Leg(spread = 5.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                toleranceOverrides = mapOf(HEAD_TURN to 18.0),
                tips = listOf("Pick a point in the distance to look at"),
            ),
            TemplateFactory.create(
                id = "leaning", name = "Leaning pose", category = PoseCategory.CASUAL,
                description = "Leaning on a wall or railing to your right, legs relaxed.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    torsoLean = -10.0
                    shoulderTilt = -6.0
                    headTilt = -6.0
                    rightArm = Arm(raise = 30.0, elbow = 150.0)
                    leftLeg = Leg(spread = -6.0, knee = 165.0)
                    rightLeg = Leg(spread = 8.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                tips = listOf("Use a wall on your right side", "Cross your left foot over"),
            ),
            TemplateFactory.create(
                id = "one-leg-forward", name = "One leg forward", category = PoseCategory.FORMAL,
                description = "Three-quarter stance with the left knee softly bent in front.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    bodyTurn = 25.0
                    headTurn = -15.0
                    leftLeg = Leg(spread = 12.0, knee = 155.0, bend = Bend.OUTWARD)
                    rightLeg = Leg(spread = 4.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                tips = listOf("Weight on the back leg", "Point the front toe at the camera"),
            ),
            TemplateFactory.create(
                id = "hand-on-waist", name = "Hand on waist", category = PoseCategory.SOCIAL,
                description = "Left hand on the waist with the elbow out. Classic fashion pose.",
                framing = FramingType.FULL_BODY,
                pose = PoseBuilder.pose {
                    bodyTurn = 15.0
                    leftArm = Arm(raise = 42.0, elbow = 95.0, bend = Bend.TOWARD_MIDLINE)
                    leftLeg = Leg(spread = 6.0); rightLeg = Leg(spread = 6.0, knee = 168.0)
                },
                features = TORSO + ARMS + LEGS + HEAD,
                tips = listOf("Point the elbow slightly back", "Relax the other arm"),
            ),
            TemplateFactory.create(
                id = "simple-portrait", name = "Simple portrait", category = PoseCategory.PORTRAIT,
                description = "Head-and-shoulders portrait: shoulders angled away, face turned back, chin slightly down.",
                framing = FramingType.UPPER_BODY,
                pose = PoseBuilder.pose {
                    bodyTurn = 35.0
                    headTurn = -22.0
                    headTilt = 6.0
                    chinUp = -0.03
                },
                features = TORSO + HEAD + setOf(LEFT_ARM_RAISE, RIGHT_ARM_RAISE),
                tips = listOf("Shoulders relaxed", "Soft smile with your eyes"),
            ),
        )
    }

    fun byId(id: String): PoseTemplate? = all.firstOrNull { it.id == id }
    fun byCategory(category: PoseCategory): List<PoseTemplate> = all.filter { it.category == category }
}
