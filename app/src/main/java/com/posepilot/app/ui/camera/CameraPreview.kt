package com.posepilot.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.posepilot.app.camera.CameraSession
import com.posepilot.app.models.LensFacing
import com.posepilot.app.pose.detection.LivePoseAnalyzer
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/**
 * Camera preview + live pose analysis. The [CameraSession] lives as long as this composable; switching lens
 * re-binds the same use cases. Everything heavy happens on the session's analysis executor.
 */
@Composable
fun CameraPreview(
    vm: CoachViewModel,
    lens: LensFacing,
    onSessionReady: (CameraSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val session = remember { CameraSession(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER // must match OverlayTransform (fit = false)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzer = remember {
        // ML Kit may deliver a result after the executor was shut down on dispose; drop it quietly.
        val safeExecutor = Executor { r ->
            try { session.analysisExecutor.execute(r) } catch (_: RejectedExecutionException) { }
        }
        LivePoseAnalyzer(safeExecutor, vm::onPoseFrame, vm::onDetectorError)
    }

    LaunchedEffect(lens) {
        session.bind(lifecycleOwner, previewView, lens, analyzer, vm::onCameraError)
        onSessionReady(session)
    }

    DisposableEffect(Unit) {
        onDispose {
            session.release()
            analyzer.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
