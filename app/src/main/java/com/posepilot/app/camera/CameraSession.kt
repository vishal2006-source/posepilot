package com.posepilot.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.posepilot.app.models.LensFacing
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Owns the CameraX use cases: Preview, ImageAnalysis (pose detection) and ImageCapture.
 * All three use the same 4:3 aspect ratio so analysis coordinates line up with the preview.
 */
class CameraSession(private val context: Context) {

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var provider: ProcessCameraProvider? = null
    private var lens: LensFacing = LensFacing.BACK

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: LensFacing,
        analyzer: ImageAnalysis.Analyzer,
        onError: (String) -> Unit,
    ) {
        lens = lensFacing
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider

                val aspect43 = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
                val analysisSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    // ~640x480 is plenty for body landmarks and keeps detection fast on mid-range phones.
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    )
                    .build()

                val preview = Preview.Builder().setResolutionSelector(aspect43).build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(analysisSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }
                val capture = ImageCapture.Builder()
                    .setResolutionSelector(aspect43)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                val selector = if (lensFacing == LensFacing.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
                if (!cameraProvider.hasCamera(selector)) {
                    onError("This camera isn't available on your device.")
                    return@addListener
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis, capture)
            } catch (e: Exception) {
                onError("Couldn't start the camera: ${e.message ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Captures a photo into memory (nothing is written to storage here). */
    fun takePhoto(mirrorFront: Boolean, onResult: (Bitmap) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture ?: return onError("Camera not ready yet")
        capture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val raw = image.toBitmap()
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        if (lens == LensFacing.FRONT && mirrorFront) postScale(-1f, 1f)
                    }
                    val upright = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                    if (upright !== raw) raw.recycle()
                    onResult(upright)
                } catch (e: Exception) {
                    onError("Couldn't process the photo")
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError("Capture failed: ${exception.message ?: exception.imageCaptureError}")
            }
        })
    }

    fun release() {
        provider?.unbindAll()
        analysisExecutor.shutdown()
    }
}
