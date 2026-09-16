package com.posepilot.app.ui.camera

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.posepilot.app.audio.VoiceGuide
import com.posepilot.app.data.AppContainer
import com.posepilot.app.guidance.CorrectionEngine
import com.posepilot.app.guidance.GuidanceConfig
import com.posepilot.app.guidance.GuidanceEngine
import com.posepilot.app.models.GuidanceInstruction
import com.posepilot.app.models.GuidancePhase
import com.posepilot.app.models.InstructionTone
import com.posepilot.app.models.LensFacing
import com.posepilot.app.models.PoseComparison
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.models.UserSettings
import com.posepilot.app.pose.landmarks.LandmarkSmoother
import com.posepilot.app.targetpose.PoseLibrary
import com.posepilot.app.targetpose.PoseTemplate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

data class CoachUiState(
    val template: PoseTemplate,
    val settings: UserSettings = UserSettings(),
    val comparison: PoseComparison = PoseComparison.EMPTY,
    val instruction: GuidanceInstruction = GuidanceInstruction("Point the camera at the person", InstructionTone.NEUTRAL, GuidancePhase.NO_PERSON),
    val score: Double = 0.0,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val lowLight: Boolean = false,
    val cameraError: String? = null,
    val isCapturing: Boolean = false,
) {
    val lens: LensFacing get() = if (settings.useFrontCamera) LensFacing.FRONT else LensFacing.BACK
}

sealed interface CoachEvent {
    data object TakePhoto : CoachEvent
    data object OpenReview : CoachEvent
    data class Message(val text: String) : CoachEvent
}

/**
 * Pipeline per analysed frame (on the camera analysis thread, never the UI thread):
 * ML Kit PoseFrame -> smoothing -> CorrectionEngine (geometry + rules) -> GuidanceEngine -> UI state / voice.
 */
class CoachViewModel(
    private val container: AppContainer,
    app: Application,
    initialPoseId: String?,
) : AndroidViewModel(app) {

    private val voice = VoiceGuide(app)
    private val smoother = LandmarkSmoother()
    private val guidance = GuidanceEngine()

    private val _state = MutableStateFlow(
        CoachUiState(template = container.templates.byId(initialPoseId) ?: PoseLibrary.all.first())
    )
    val state: StateFlow<CoachUiState> = _state

    private val _events = Channel<CoachEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Written from the UI thread, consumed on the analysis thread.
    @Volatile private var template: PoseTemplate = _state.value.template
    @Volatile private var resetRequested = false
    private val manualQueue = ConcurrentLinkedQueue<String>()
    private var detectorErrors = 0

    init {
        viewModelScope.launch {
            container.settings.settings.collect { s ->
                voice.enabled = s.voiceGuidance
                guidance.speech.minIntervalMs = s.instructionIntervalMs
                guidance.config = guidance.config.copy(autoCapture = s.autoCapture, countdownSeconds = s.countdownSeconds)
                _state.update { it.copy(settings = s) }
            }
        }
    }

    /** Called on the analysis executor for every processed frame (frame == null when nobody is detected). */
    fun onPoseFrame(frame: PoseFrame?, width: Int, height: Int, timestampMs: Long, lowLight: Boolean) {
        detectorErrors = 0
        if (resetRequested) {
            resetRequested = false
            guidance.reset()
            smoother.reset()
        }
        while (true) {
            val manual = manualQueue.poll() ?: break
            guidance.pushManual(manual, timestampMs)
        }
        val smoothed = frame?.let { smoother.smooth(it) }
        val comparison = CorrectionEngine.analyze(smoothed, template)
        var update = guidance.update(comparison, timestampMs)

        if (!comparison.personDetected && lowLight && update.instruction.phase == GuidancePhase.NO_PERSON) {
            update = update.copy(instruction = update.instruction.copy(text = "No person detected. Try better lighting."))
        }
        update.speech?.let { voice.speak(it) }
        if (update.captureNow) _events.trySend(CoachEvent.TakePhoto)

        _state.update {
            it.copy(
                comparison = comparison, instruction = update.instruction, score = update.displayScore,
                imageWidth = width, imageHeight = height, lowLight = lowLight,
            )
        }
    }

    fun onDetectorError(e: Exception) {
        detectorErrors++
        if (detectorErrors == 30) {
            _events.trySend(CoachEvent.Message("Pose detection isn't working on this device right now (${e.message})."))
        }
    }

    fun onCameraError(message: String) = _state.update { it.copy(cameraError = message) }

    fun selectTemplate(t: PoseTemplate) {
        template = t
        resetRequested = true
        _state.update { it.copy(template = t, comparison = PoseComparison.EMPTY) }
    }

    fun manualInstruction(text: String) {
        manualQueue.add(text)
    }

    fun captureNow() {
        if (_state.value.isCapturing) return
        _events.trySend(CoachEvent.TakePhoto)
    }

    fun onCaptureStarted() = _state.update { it.copy(isCapturing = true) }

    fun onPhotoCaptured(bitmap: Bitmap) {
        container.captures.set(bitmap, template.id)
        _state.update { it.copy(isCapturing = false) }
        _events.trySend(CoachEvent.OpenReview)
    }

    fun onCaptureFailed(message: String) {
        _state.update { it.copy(isCapturing = false) }
        resetRequested = true
        _events.trySend(CoachEvent.Message(message))
    }

    /** Resume coaching when coming back from the review screen. */
    fun onResume() {
        resetRequested = true
        _state.update { it.copy(cameraError = null) }
    }

    fun onPause() = voice.stop()

    fun toggleVoice() = viewModelScope.launch { container.settings.update { it.copy(voiceGuidance = !it.voiceGuidance) } }

    fun flipCamera() = viewModelScope.launch {
        resetRequested = true
        _state.update { it.copy(cameraError = null, comparison = PoseComparison.EMPTY) }
        container.settings.update { it.copy(useFrontCamera = !it.useFrontCamera) }
    }

    override fun onCleared() {
        voice.shutdown()
        super.onCleared()
    }

    companion object {
        val MANUAL_INSTRUCTIONS = listOf(
            "Raise your left hand", "Raise your right hand", "Step to your left", "Step to your right",
            "Move closer", "Move back", "Turn to your left", "Turn to your right", "Stand up straight",
            "Look at the camera", "Look away", "Hands in your pockets", "Smile", "Relax your shoulders",
        )
    }
}
