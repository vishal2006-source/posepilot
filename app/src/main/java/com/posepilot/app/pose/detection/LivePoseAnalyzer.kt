package com.posepilot.app.pose.detection

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.posepilot.app.models.PoseFrame
import java.util.concurrent.Executor

/**
 * CameraX analyzer running ML Kit pose detection in STREAM_MODE, fully on-device.
 *
 * Back-pressure: the ImageProxy is only closed when ML Kit finishes, and the ImageAnalysis use case is
 * configured with STRATEGY_KEEP_ONLY_LATEST, so frames that arrive while we're busy are dropped and the
 * next analysed frame is always the newest one.
 */
class LivePoseAnalyzer(
    private val callbackExecutor: Executor,
    private val onFrame: (frame: PoseFrame?, uprightWidth: Int, uprightHeight: Int, timestampMs: Long, lowLight: Boolean) -> Unit,
    private val onError: (Exception) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private var frameCount = 0L
    @Volatile private var lowLight = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        // ML Kit returns coordinates in the rotated (upright) image, so swap dimensions for 90/270.
        val uprightW = if (rotation % 180 == 0) imageProxy.width else imageProxy.height
        val uprightH = if (rotation % 180 == 0) imageProxy.height else imageProxy.width
        val timestamp = SystemClock.elapsedRealtime()
        if (frameCount++ % 15 == 0L) lowLight = averageLuma(imageProxy) < 45
        val input = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(input)
            .addOnSuccessListener(callbackExecutor) { pose ->
                onFrame(MlKitPoseMapper.toFrame(pose, uprightW, uprightH, timestamp), uprightW, uprightH, timestamp, lowLight)
            }
            .addOnFailureListener(callbackExecutor) { e -> onError(e) }
            .addOnCompleteListener(callbackExecutor) { imageProxy.close() }
    }

    /** Cheap brightness estimate: samples the Y (luminance) plane on a sparse grid. */
    private fun averageLuma(image: ImageProxy): Int {
        val plane = image.planes.firstOrNull() ?: return 255
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        var sum = 0L
        var n = 0
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                val index = y * rowStride + x
                if (index < buffer.limit()) { sum += buffer.get(index).toInt() and 0xFF; n++ }
                x += 24
            }
            y += 24
        }
        return if (n == 0) 255 else (sum / n).toInt()
    }

    fun close() = detector.close()
}
