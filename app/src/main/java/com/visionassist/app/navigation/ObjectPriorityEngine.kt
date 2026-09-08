package com.visionassist.app.navigation

import com.visionassist.app.vision.DistanceCategory

data class PrioritizedDetection(
    val label: String,
    val confidence: Float,
    val direction: Direction,
    val distance: DistanceCategory
)

/**
 * Scores multiple detections in a single frame and picks the single
 * most important one to potentially alert the user about.
 *
 * Priority favors: closer objects, higher confidence, and objects
 * directly ahead (center) over ones to the side.
 */
object ObjectPriorityEngine {

    // Lower ordinal = closer = more urgent.
    private fun distanceUrgency(distance: DistanceCategory): Int {
        return when (distance) {
            DistanceCategory.VERY_CLOSE -> 3
            DistanceCategory.ABOUT_ONE_METER -> 2
            DistanceCategory.ABOUT_TWO_METERS -> 1
            DistanceCategory.SEVERAL_METERS -> 0
        }
    }

    private fun directionBonus(direction: Direction): Float {
        return if (direction == Direction.CENTER) 1.5f else 0f
    }

    private fun score(detection: PrioritizedDetection): Float {
        return (distanceUrgency(detection.distance) * 10f) +
                (detection.confidence * 3f) +
                directionBonus(detection.direction)
    }

    /**
     * Returns the single highest-priority detection, or null if the list is empty.
     */
    fun pickTopPriority(detections: List<PrioritizedDetection>): PrioritizedDetection? {
        return detections.maxByOrNull { score(it) }
    }
}