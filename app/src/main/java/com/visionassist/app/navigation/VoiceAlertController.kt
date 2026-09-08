package com.visionassist.app.navigation

import com.visionassist.app.vision.DistanceCategory

/**
 * Decides whether a prioritized detection should actually be spoken.
 * Speaks once per new situation (new object, new direction, or the
 * object getting closer) and stays silent otherwise — no automatic
 * repeat-on-timer, since that gets overwhelming for the listener.
 */
class VoiceAlertController {

    private var lastSpokenLabel: String? = null
    private var lastSpokenDirection: Direction? = null
    private var lastSpokenDistance: DistanceCategory? = null

    private fun distanceOrdinal(distance: DistanceCategory): Int {
        return when (distance) {
            DistanceCategory.VERY_CLOSE -> 0
            DistanceCategory.ABOUT_ONE_METER -> 1
            DistanceCategory.ABOUT_TWO_METERS -> 2
            DistanceCategory.SEVERAL_METERS -> 3
        }
    }

    /**
     * Returns a spoken sentence if this detection represents a genuinely
     * new situation worth mentioning, or null if it should stay silent.
     */
    fun shouldSpeak(detection: PrioritizedDetection): String? {
        val isSameAsLast = detection.label == lastSpokenLabel &&
                detection.direction == lastSpokenDirection

        val hasGottenCloser = isSameAsLast &&
                lastSpokenDistance != null &&
                distanceOrdinal(detection.distance) < distanceOrdinal(lastSpokenDistance!!)

        val shouldAnnounce = !isSameAsLast || hasGottenCloser

        if (!shouldAnnounce) {
            return null
        }

        lastSpokenLabel = detection.label
        lastSpokenDirection = detection.direction
        lastSpokenDistance = detection.distance

        return buildSentence(detection)
    }

    /**
     * Call this when nothing is currently detected, so that if the same
     * object appears again later, it gets announced fresh instead of
     * being remembered as "already spoken."
     */
    fun reset() {
        lastSpokenLabel = null
        lastSpokenDirection = null
        lastSpokenDistance = null
    }

    private fun buildSentence(detection: PrioritizedDetection): String {
        val directionPhrase = when (detection.direction) {
            Direction.LEFT -> "on your left"
            Direction.RIGHT -> "on your right"
            Direction.CENTER -> "ahead"
        }
        return "${detection.label.replaceFirstChar { it.uppercase() }} $directionPhrase, ${detection.distance.spokenLabel}."
    }
}