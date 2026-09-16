package com.posepilot.app.targetpose

import com.posepilot.app.models.FramingType
import com.posepilot.app.models.LandmarkType.*
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.pose.landmarks.PoseNormalizer

sealed interface ReferenceResult {
    data class Success(val template: PoseTemplate, val warnings: List<String>) : ReferenceResult
    data class Failure(val message: String) : ReferenceResult
}

/**
 * Converts a person detected in a still reference photo into a [PoseTemplate]:
 * detect -> normalize by torso length -> measure features -> rules with slightly relaxed tolerances.
 */
object ReferencePoseFactory {

    fun fromFrame(
        frame: PoseFrame?,
        id: String,
        name: String = "Reference pose",
        facesDetected: Int = -1,
    ): ReferenceResult {
        if (frame == null || frame.isEmpty) {
            return ReferenceResult.Failure("No person found in this photo. Choose a photo where one person is clearly visible.")
        }
        val pose = PoseNormalizer.normalize(frame)
            ?: return ReferenceResult.Failure("Couldn't see the person's shoulders and hips clearly. Try a photo showing at least the upper body.")

        val warnings = mutableListOf<String>()
        if (facesDetected > 1) {
            warnings += "More than one person found. Using the most prominent person — multi-person reference matching is an advanced feature."
        }
        val legsVisible = listOf(LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE).all { frame.isVisible(it) }
        val framing = if (legsVisible) FramingType.FULL_BODY else FramingType.UPPER_BODY
        if (!legsVisible) warnings += "Legs aren't fully visible, so only the upper body will be coached."

        val features = if (framing == FramingType.FULL_BODY) TemplateFactory.ALL_FEATURES else TemplateFactory.UPPER_BODY_FEATURES
        // Real photos are noisier than authored poses, so tolerances are relaxed by 20%.
        val template = TemplateFactory.create(
            id = id, name = name, category = PoseCategory.REFERENCE,
            description = "Pose extracted from your reference photo.",
            framing = framing, pose = pose, features = features,
            source = TemplateSource.REFERENCE_PHOTO, toleranceScale = 1.2,
        )
        if (template.rules.size < 4) {
            return ReferenceResult.Failure("Too little of the body is visible to build a pose. Try a clearer photo.")
        }
        return ReferenceResult.Success(template, warnings)
    }
}
