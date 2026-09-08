package com.visionassist.app.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Wraps ML Kit's on-device Text Recognition.
 * Takes a Bitmap already in memory — never reads from or writes to disk.
 */
class OCRManager {

    companion object {
        private const val TAG = "OCRManager"
    }

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Runs OCR on the given bitmap.
     * onResult is called with the recognized text, or an empty string if nothing was found.
     * onError is called if recognition fails outright.
     */
    fun recognizeText(
        bitmap: Bitmap,
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, "OCR succeeded. Text length: ${visionText.text.length}")
                onResult(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed: ${e.message}", e)
                onError(e)
            }
    }
}