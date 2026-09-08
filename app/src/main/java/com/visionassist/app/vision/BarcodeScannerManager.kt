package com.visionassist.app.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Wraps ML Kit's on-device Barcode Scanning.
 * Takes a Bitmap already in memory — never reads from or writes to disk.
 */
class BarcodeScannerManager {

    companion object {
        private const val TAG = "BarcodeScannerManager"
    }

    // Explicitly list the formats mentioned in the project spec.
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Scans the bitmap for a barcode.
     * onResult is called with the raw barcode value (or null if none found).
     * onError is called if scanning fails outright.
     */
    fun scanBarcode(
        bitmap: Bitmap,
        onResult: (Barcode?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                Log.d(TAG, "Barcode scan found ${barcodes.size} result(s).")
                // If multiple barcodes are visible, just use the first one for now.
                onResult(barcodes.firstOrNull())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scan failed: ${e.message}", e)
                onError(e)
            }
    }
}