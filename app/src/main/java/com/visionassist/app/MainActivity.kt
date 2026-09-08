package com.visionassist.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.visionassist.app.camera.CameraController
import com.visionassist.app.navigation.DirectionCalculator
import com.visionassist.app.navigation.ObjectPriorityEngine
import com.visionassist.app.navigation.PrioritizedDetection
import com.visionassist.app.navigation.VoiceAlertController
import com.visionassist.app.product.ProductManager
import com.visionassist.app.product.ProductRepository
import com.visionassist.app.settings.AppSettings
import com.visionassist.app.vision.BarcodeScannerManager
import com.visionassist.app.vision.DistanceEstimator
import com.visionassist.app.vision.ObjectDetectorManager
import com.visionassist.app.vision.OCRManager
import com.visionassist.app.voice.SpeechInputManager
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private lateinit var cameraController: CameraController
    private lateinit var previewView: PreviewView
    private lateinit var ocrManager: OCRManager
    private lateinit var barcodeScannerManager: BarcodeScannerManager
    private lateinit var productManager: ProductManager
    private lateinit var appSettings: AppSettings
    private lateinit var objectDetectorManager: ObjectDetectorManager
    private lateinit var speechInputManager: SpeechInputManager

    private val voiceAlertController = VoiceAlertController()
    private var lastTapTime = 0L
    private var isPerformingAction = false

    companion object {
        private const val TAG = "VisionAssist"
        private const val RECORD_AUDIO_REQUEST_CODE = 1001
        private const val DOUBLE_TAP_WINDOW_MS = 400L
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Camera permission granted.")
                startCameraWithAnalysis()
            } else {
                Log.w(TAG, "Camera permission denied.")
                speak("Camera permission is required.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        appSettings = AppSettings(this)
        cameraController = CameraController(this, this)
        ocrManager = OCRManager()
        barcodeScannerManager = BarcodeScannerManager()
        productManager = ProductManager(ProductRepository(appSettings))
        objectDetectorManager = ObjectDetectorManager(this)
        speechInputManager = SpeechInputManager(this)

        textToSpeech = TextToSpeech(this, this)

        // Request microphone permission for voice input.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        }

        // Single tap = read text (OCR). Double tap = test voice input.
        // (Both are temporary trigger methods — will be replaced by
        // volume-button control in the next stage.)
        previewView.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < DOUBLE_TAP_WINDOW_MS) {
                isPerformingAction = true
                speak("I'm listening.")
                speechInputManager.startListening(
                    onResult = { text ->
                        speak("You said: $text")
                        isPerformingAction = false
                    },
                    onError = { message ->
                        speak(message)
                        isPerformingAction = false
                    }
                )
            } else {
                speak("Reading text.")
                captureAndReadText()
            }
            lastTapTime = now
        }

        previewView.setOnLongClickListener {
            speak("Scanning barcode.")
            captureAndScanBarcode()
            true
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            showOnlineLookupSettingDialog()
        }
    }

    private fun startCameraWithAnalysis() {
        cameraController.startCamera(previewView) { bitmap ->
            if (isPerformingAction) {
                return@startCamera
            }

            val detections = objectDetectorManager.detect(bitmap)

            if (detections.isEmpty()) {
                voiceAlertController.reset()
                return@startCamera
            }

            val prioritizedDetections = detections.map { detection ->
                val category = detection.categories.firstOrNull()
                val label = category?.label ?: "object"
                val confidence = category?.score ?: 0f
                val box = detection.boundingBox
                val direction = DirectionCalculator.calculateDirection(box, bitmap.width)
                val distance = DistanceEstimator.estimateDistance(box, bitmap.height)

                PrioritizedDetection(label, confidence, direction, distance)
            }

            val topDetection = ObjectPriorityEngine.pickTopPriority(prioritizedDetections)

            topDetection?.let {
                val sentence = voiceAlertController.shouldSpeak(it)
                if (sentence != null) {
                    Log.d(TAG, "Speaking alert: $sentence")
                    speak(sentence)
                }
            }
        }
    }
    private fun showOnlineLookupSettingDialog() {
        val currentlyEnabled = appSettings.isOnlineLookupEnabled

        AlertDialog.Builder(this)
            .setTitle("Online Product Lookup")
            .setMessage(
                "VisionAssist can look up scanned barcodes using Open Food Facts, " +
                        "a free public product database, to give you more detailed product information.\n\n" +
                        "If you enable this, the barcode number of scanned products will be sent " +
                        "over the internet to Open Food Facts' servers. No images, photos, or personal " +
                        "information are ever sent — only the barcode number.\n\n" +
                        "This is currently ${if (currentlyEnabled) "ENABLED" else "DISABLED"}."
            )
            .setPositiveButton(if (currentlyEnabled) "Disable" else "Enable") { _, _ ->
                appSettings.isOnlineLookupEnabled = !currentlyEnabled
                speak(
                    if (!currentlyEnabled) "Online product lookup enabled."
                    else "Online product lookup disabled."
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun captureFrameSuspend(): android.graphics.Bitmap? =
        suspendCancellableCoroutine { continuation ->
            cameraController.captureFrame(
                onSuccess = { bitmap -> continuation.resume(bitmap) },
                onError = { continuation.resume(null) }
            )
        }

    private suspend fun recognizeTextSuspend(bitmap: android.graphics.Bitmap): String? =
        suspendCancellableCoroutine { continuation ->
            ocrManager.recognizeText(
                bitmap = bitmap,
                onResult = { text -> continuation.resume(if (text.isBlank()) null else text) },
                onError = { continuation.resume(null) }
            )
        }

    private suspend fun scanBarcodeSuspend(bitmap: android.graphics.Bitmap): String? =
        suspendCancellableCoroutine { continuation ->
            barcodeScannerManager.scanBarcode(
                bitmap = bitmap,
                onResult = { barcode -> continuation.resume(barcode?.rawValue) },
                onError = { continuation.resume(null) }
            )
        }

    private fun captureAndReadText() {
        isPerformingAction = true
        lifecycleScope.launch {
            val maxAttempts = 12
            val retryDelayMs = 600L
            var result: String? = null

            for (attempt in 1..maxAttempts) {
                val bitmap = captureFrameSuspend()
                if (bitmap != null) {
                    result = recognizeTextSuspend(bitmap)
                }
                if (result != null) break
                delay(retryDelayMs)
            }

            if (result != null) {
                speak(result)
            } else {
                speak("I couldn't read the text.")
            }
            isPerformingAction = false
        }
    }

    private fun captureAndScanBarcode() {
        isPerformingAction = true
        lifecycleScope.launch {
            val maxAttempts = 12
            val retryDelayMs = 600L
            var barcodeValue: String? = null

            for (attempt in 1..maxAttempts) {
                val bitmap = captureFrameSuspend()
                if (bitmap != null) {
                    barcodeValue = scanBarcodeSuspend(bitmap)
                }
                if (barcodeValue != null) break
                delay(retryDelayMs)
            }

            if (barcodeValue != null) {
                val description = productManager.describeProduct(barcodeValue)
                speak(description)
            } else {
                speak("I couldn't find a barcode.")
            }
            isPerformingAction = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e(TAG, "TTS language not supported.")
            } else {
                speak("VisionAssist is ready.")
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }

        checkCameraPermissionAndStart()
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCameraWithAnalysis()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechInputManager.destroy()
        super.onDestroy()
    }
}