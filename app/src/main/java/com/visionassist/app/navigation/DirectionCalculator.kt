package com.visionassist.app.navigation

import android.graphics.RectF

enum class Direction {
    LEFT, CENTER, RIGHT
}

/**
 * Determines whether a detected object is to the left, center, or right
 * of the frame, based on the horizontal position of its bounding box.
 *
 * This is pure logic with no Android/camera/ML dependencies, so it can
 * be unit tested independently and reused across features (object
 * detection, OCR regions, product scanning) later.
 */
object DirectionCalculator {

    // The center zone occupies the middle third of the frame by default.
    private const val LEFT_ZONE_END_RATIO = 1f / 3f
    private const val RIGHT_ZONE_START_RATIO = 2f / 3f

    fun calculateDirection(boundingBox: RectF, frameWidth: Int): Direction {
        val boxCenterX = (boundingBox.left + boundingBox.right) / 2f
        val positionRatio = boxCenterX / frameWidth

        return when {
            positionRatio < LEFT_ZONE_END_RATIO -> Direction.LEFT
            positionRatio > RIGHT_ZONE_START_RATIO -> Direction.RIGHT
            else -> Direction.CENTER
        }
    }
}