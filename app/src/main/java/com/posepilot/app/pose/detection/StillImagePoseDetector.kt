package com.posepilot.app.pose.detection

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.posepilot.app.models.PoseFrame
import kotlinx.coroutines.tasks.await

/**
 * Accurate single-image detection for reference photos and captured shots.
 * ML Kit's pose detector returns only the most prominent person, so faces are counted separately to warn
 * when a photo contains several people.
 */
class StillImagePoseDetector {
    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
    )
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.05f)
            .build()
    )

    data class Result(val frame: PoseFrame?, val faces: List<Face>)

    /** [bitmap] must already be upright (EXIF orientation applied). */
    suspend fun detect(bitmap: Bitmap): Result {
        val image = InputImage.fromBitmap(bitmap, 0)
        val pose = poseDetector.process(image).await()
        val faces = runCatching { faceDetector.process(image).await() }.getOrDefault(emptyList())
        return Result(MlKitPoseMapper.toFrame(pose, bitmap.width, bitmap.height, 0L), faces)
    }

    fun close() {
        poseDetector.close()
        faceDetector.close()
    }
}
