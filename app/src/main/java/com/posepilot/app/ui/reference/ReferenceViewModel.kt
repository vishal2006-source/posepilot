package com.posepilot.app.ui.reference

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.posepilot.app.data.AppContainer
import com.posepilot.app.targetpose.PoseTemplate
import com.posepilot.app.targetpose.ReferencePoseFactory
import com.posepilot.app.targetpose.ReferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ReferenceState {
    data object Idle : ReferenceState
    data object Processing : ReferenceState
    data class Success(
        val template: PoseTemplate,
        val warnings: List<String>,
        val previewBitmap: Bitmap,
    ) : ReferenceState

    data class Error(val message: String) : ReferenceState
}

/**
 * Turns a reference photo into a coachable [PoseTemplate].
 * Privacy: the photo is only held in memory for the preview — only the extracted skeleton is ever saved,
 * and a photo taken with the camera is deleted from the cache as soon as it has been decoded.
 */
class ReferenceViewModel(private val container: AppContainer, app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<ReferenceState>(ReferenceState.Idle)
    val state: StateFlow<ReferenceState> = _state

    fun process(uri: Uri, deleteAfter: File? = null) {
        _state.value = ReferenceState.Processing
        viewModelScope.launch {
            try {
                val bitmap = decode(uri)
                deleteAfter?.delete()
                val detection = container.stillDetector.detect(bitmap)
                val result = ReferencePoseFactory.fromFrame(
                    frame = detection.frame,
                    id = "ref-" + System.currentTimeMillis(),
                    facesDetected = detection.faces.size,
                )
                _state.value = when (result) {
                    is ReferenceResult.Success -> ReferenceState.Success(result.template, result.warnings, bitmap)
                    is ReferenceResult.Failure -> ReferenceState.Error(result.message)
                }
            } catch (e: Exception) {
                deleteAfter?.delete()
                _state.value = ReferenceState.Error("Couldn't read that photo (${e.message}). Try another one.")
            }
        }
    }

    /** Makes the extracted pose coachable without saving it, and returns its id. */
    fun startCoaching(): String? {
        val success = _state.value as? ReferenceState.Success ?: return null
        container.templates.setTransient(success.template)
        return success.template.id
    }

    fun save(name: String) {
        val success = _state.value as? ReferenceState.Success ?: return
        viewModelScope.launch { container.templates.save(success.template.copy(name = name)) }
    }

    fun reset() {
        _state.value = ReferenceState.Idle
    }

    /** ImageDecoder applies EXIF orientation itself; SOFTWARE allocation keeps the pixels readable by ML Kit. */
    private suspend fun decode(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    }

    private companion object {
        const val MAX_SIDE = 1600
    }
}
