package com.posepilot.app.pose.analysis

import com.posepilot.app.models.BodyRegion
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.LandmarkType.*

/**
 * Every measurable aspect of a pose. Values come from [FeatureExtractor].
 *
 * Sign conventions (subject facing the camera, un-mirrored image):
 *  - the subject's LEFT side appears on the image's +x side.
 *  - "increase" means the measured value must go up to reach the target.
 *
 * [anchor] is the landmark an on-screen arrow should start from. [defaultTolerance] is in the feature's unit.
 */
enum class PoseFeature(
    val label: String,
    val region: BodyRegion,
    val unit: MeasureUnit,
    val required: List<LandmarkType>,
    val anchor: LandmarkType,
    val defaultTolerance: Double,
    val priority: Int,
    val increaseInstruction: String,
    val decreaseInstruction: String,
    /** Instruction to use instead when the target is roughly neutral (e.g. "Look at the camera"). */
    val neutralInstruction: String? = null,
    val neutralBand: Double = 0.0,
) {
    BODY_TURN(
        "Body turn", BodyRegion.TORSO, MeasureUnit.DEGREES,
        listOf(LEFT_SHOULDER, RIGHT_SHOULDER), LEFT_SHOULDER, 22.0, 10,
        "Turn your body to your left", "Turn your body to your right",
        neutralInstruction = "Face the camera", neutralBand = 12.0,
    ),
    TORSO_LEAN(
        "Torso lean", BodyRegion.TORSO, MeasureUnit.DEGREES,
        listOf(LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HIP, RIGHT_HIP), NOSE, 8.0, 11,
        "Lean slightly to your left", "Lean slightly to your right",
        neutralInstruction = "Stand up straight", neutralBand = 4.0,
    ),
    LEFT_ARM_RAISE(
        "Left arm height", BodyRegion.LEFT_ARM, MeasureUnit.DEGREES,
        listOf(LEFT_HIP, LEFT_SHOULDER, LEFT_ELBOW), LEFT_WRIST, 18.0, 20,
        "Raise your left arm", "Lower your left arm",
    ),
    RIGHT_ARM_RAISE(
        "Right arm height", BodyRegion.RIGHT_ARM, MeasureUnit.DEGREES,
        listOf(RIGHT_HIP, RIGHT_SHOULDER, RIGHT_ELBOW), RIGHT_WRIST, 18.0, 20,
        "Raise your right arm", "Lower your right arm",
    ),
    LEFT_ELBOW_ANGLE(
        "Left elbow", BodyRegion.LEFT_ARM, MeasureUnit.DEGREES,
        listOf(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST), LEFT_WRIST, 22.0, 21,
        "Straighten your left arm", "Bend your left elbow",
    ),
    RIGHT_ELBOW_ANGLE(
        "Right elbow", BodyRegion.RIGHT_ARM, MeasureUnit.DEGREES,
        listOf(RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST), RIGHT_WRIST, 22.0, 21,
        "Straighten your right arm", "Bend your right elbow",
    ),
    LEFT_HAND_REACH(
        "Left hand position", BodyRegion.LEFT_ARM, MeasureUnit.TORSO_LENGTHS,
        listOf(LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_WRIST), LEFT_WRIST, 0.22, 22,
        "Move your left hand outward", "Bring your left hand across your body",
    ),
    RIGHT_HAND_REACH(
        "Right hand position", BodyRegion.RIGHT_ARM, MeasureUnit.TORSO_LENGTHS,
        listOf(LEFT_SHOULDER, RIGHT_SHOULDER, RIGHT_WRIST), RIGHT_WRIST, 0.22, 22,
        "Move your right hand outward", "Bring your right hand across your body",
    ),
    STANCE_WIDTH(
        "Feet distance", BodyRegion.LEGS, MeasureUnit.TORSO_LENGTHS,
        listOf(LEFT_ANKLE, RIGHT_ANKLE), LEFT_ANKLE, 0.22, 30,
        "Move your feet further apart", "Bring your feet closer together",
    ),
    LEFT_KNEE_ANGLE(
        "Left knee", BodyRegion.LEGS, MeasureUnit.DEGREES,
        listOf(LEFT_HIP, LEFT_KNEE, LEFT_ANKLE), LEFT_KNEE, 20.0, 31,
        "Straighten your left leg", "Bend your left knee slightly",
    ),
    RIGHT_KNEE_ANGLE(
        "Right knee", BodyRegion.LEGS, MeasureUnit.DEGREES,
        listOf(RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE), RIGHT_KNEE, 20.0, 31,
        "Straighten your right leg", "Bend your right knee slightly",
    ),
    SHOULDER_TILT(
        "Shoulder level", BodyRegion.TORSO, MeasureUnit.DEGREES,
        listOf(LEFT_SHOULDER, RIGHT_SHOULDER), LEFT_SHOULDER, 7.0, 35,
        "Drop your left shoulder slightly", "Drop your right shoulder slightly",
        neutralInstruction = "Straighten your shoulders", neutralBand = 3.0,
    ),
    HEAD_TURN(
        "Face direction", BodyRegion.HEAD, MeasureUnit.DEGREES,
        listOf(NOSE, LEFT_EAR, RIGHT_EAR), NOSE, 15.0, 40,
        "Turn your face to your left", "Turn your face to your right",
        neutralInstruction = "Look at the camera", neutralBand = 10.0,
    ),
    HEAD_TILT(
        "Head tilt", BodyRegion.HEAD, MeasureUnit.DEGREES,
        listOf(LEFT_EYE, RIGHT_EYE), NOSE, 10.0, 41,
        "Tilt your head toward your left shoulder", "Tilt your head toward your right shoulder",
        neutralInstruction = "Keep your head straight", neutralBand = 5.0,
    ),
    CHIN_HEIGHT(
        "Chin", BodyRegion.HEAD, MeasureUnit.TORSO_LENGTHS,
        listOf(NOSE, LEFT_EAR, RIGHT_EAR), NOSE, 0.05, 42,
        "Tilt your chin up slightly", "Tilt your chin down slightly",
    );

    enum class MeasureUnit { DEGREES, TORSO_LENGTHS }
}
