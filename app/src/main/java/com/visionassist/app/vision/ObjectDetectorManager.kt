package com.visionassist.app.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * Wraps a TensorFlow Lite object detection model (EfficientDet-Lite0,
 * trained on the COCO dataset). Runs fully on-device.
 *
 * Note: this model recognizes common physical objects (people, vehicles,
 * furniture, animals, bags, etc.) but does NOT recognize structural
 * elements like walls, stairs, doors, or poles — those aren't standard
 * object-detection categories. Distance/depth estimation (a later stage)
 * will help catch generic close obstacles that this model can't label.
 */
class ObjectDetectorManager(context: Context) {

    companion object {
        private const val TAG = "ObjectDetectorManager"
        private const val MODEL_FILENAME = "efficientdet-lite0.tflite"
        private const val MAX_RESULTS = 5
        private const val SCORE_THRESHOLD = 0.5f
    }

    private val detector: ObjectDetector

    init {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().build())
            .setMaxResults(MAX_RESULTS)
            .setScoreThreshold(SCORE_THRESHOLD)
            .build()

        detector = ObjectDetector.createFromFileAndOptions(context, MODEL_FILENAME, options)
    }

    /**
     * Runs detection on a single frame. Returns a list of detections,
     * or an empty list if nothing was found above the confidence threshold.
     */
    fun detect(bitmap: Bitmap): List<Detection> {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        return try {
            detector.detect(tensorImage)
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed: ${e.message}", e)
            emptyList()
        }
    }
}