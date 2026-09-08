package com.visionassist.app.vision

import android.graphics.RectF

enum class DistanceCategory(val spokenLabel: String) {
    VERY_CLOSE("very close"),
    ABOUT_ONE_METER("about one meter away"),
    ABOUT_TWO_METERS("about two meters away"),
    SEVERAL_METERS("several meters away")
}

/**
 * Approximates distance to a detected object using the ratio of its
 * bounding box height to the camera frame's height.
 *
 * IMPORTANT LIMITATION: this is a rough heuristic, not a measured
 * distance. It assumes objects of a given size occupy a predictable
 * portion of the frame at a given distance — true for a person or car
 * in typical framing, less reliable for objects with widely varying
 * real-world sizes. No depth sensor or ML depth model is used here.
 */
object DistanceEstimator {

    // Tuned thresholds: fraction of frame height the box occupies.
    private const val VERY_CLOSE_THRESHOLD = 0.6f
    private const val ONE_METER_THRESHOLD = 0.35f
    private const val TWO_METER_THRESHOLD = 0.15f

    fun estimateDistance(boundingBox: RectF, frameHeight: Int): DistanceCategory {
        val boxHeight = boundingBox.height()
        val ratio = boxHeight / frameHeight

        return when {
            ratio > VERY_CLOSE_THRESHOLD -> DistanceCategory.VERY_CLOSE
            ratio > ONE_METER_THRESHOLD -> DistanceCategory.ABOUT_ONE_METER
            ratio > TWO_METER_THRESHOLD -> DistanceCategory.ABOUT_TWO_METERS
            else -> DistanceCategory.SEVERAL_METERS
        }
    }
}