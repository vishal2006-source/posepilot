package com.posepilot.app.models

/** Body regions used for the per-region pose score. Weights sum to 1.0. */
enum class BodyRegion(val label: String, val weight: Double) {
    HEAD("Head", 0.15),
    TORSO("Torso", 0.20),
    LEFT_ARM("Left arm", 0.20),
    RIGHT_ARM("Right arm", 0.20),
    LEGS("Legs", 0.25),
}

/** Whether a pose needs the whole body in frame or only head-to-hips. */
enum class FramingType { FULL_BODY, UPPER_BODY }
