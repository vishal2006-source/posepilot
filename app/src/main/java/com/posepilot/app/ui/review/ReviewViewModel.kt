package com.posepilot.app.ui.review

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posepilot.app.data.AppContainer
import com.posepilot.app.models.PhotoAnalysis
import com.posepilot.app.photoanalysis.PhotoAnalyzer
import com.posepilot.app.targetpose.PoseTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Saved : SaveState
    data class Error(val message: String) : SaveState
}

/** Quality report for the shot that was just captured. Nothing is written to storage until [save]. */
class ReviewViewModel(private val container: AppContainer) : ViewModel() {

    private val capture = container.captures.current

    val photo: Bitmap? = capture?.bitmap
    val template: PoseTemplate? = container.templates.byId(capture?.templateId)

    private val _analysis = MutableStateFlow<PhotoAnalysis?>(null)
    val analysis: StateFlow<PhotoAnalysis?> = _analysis

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    init {
        val bitmap = photo
        if (bitmap != null) {
            viewModelScope.launch {
                _analysis.value = PhotoAnalyzer(container.stillDetector).analyze(bitmap, template)
            }
        }
    }

    fun save() {
        val bitmap = photo ?: return
        if (_saveState.value == SaveState.Saving || _saveState.value == SaveState.Saved) return
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                container.photos.save(bitmap)
                SaveState.Saved
            } catch (e: Exception) {
                SaveState.Error(e.message ?: "Couldn't save the photo")
            }
        }
    }

    override fun onCleared() {
        container.captures.clear()
        super.onCleared()
    }
}
