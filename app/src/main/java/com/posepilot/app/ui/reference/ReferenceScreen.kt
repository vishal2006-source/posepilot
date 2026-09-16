package com.posepilot.app.ui.reference

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.posepilot.app.ui.components.PrimaryButton
import com.posepilot.app.ui.components.SecondaryButton
import com.posepilot.app.ui.components.SkeletonFigure
import com.posepilot.app.ui.components.TopBar
import com.posepilot.app.ui.theme.Pp
import java.io.File

@Composable
fun ReferenceScreen(vm: ReferenceViewModel, onBack: () -> Unit, onStartCoaching: (String) -> Unit) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var pendingCapture by remember { mutableStateOf<Pair<Uri, File>?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.process(uri)
    }
    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.process(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val pending = pendingCapture
        pendingCapture = null
        if (pending == null) return@rememberLauncherForActivityResult
        if (taken) vm.process(pending.first, deleteAfter = pending.second) else pending.second.delete()
    }

    fun launchCamera() {
        val capture = newCaptureTarget(context)
        pendingCapture = capture
        cameraLauncher.launch(capture.first)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    Column(Modifier.fillMaxSize().background(Pp.Ink).safeDrawingPadding()) {
        TopBar("Reference Photo", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            when (val s = state) {
                ReferenceState.Idle -> IdleContent(
                    onCamera = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onFiles = { filesLauncher.launch(arrayOf("image/*")) },
                )

                ReferenceState.Processing -> ProcessingContent()

                is ReferenceState.Success -> SuccessContent(
                    state = s,
                    onStartCoaching = { vm.startCoaching()?.let(onStartCoaching) },
                    onSave = vm::save,
                    onAnother = vm::reset,
                )

                is ReferenceState.Error -> ErrorContent(s.message, onAnother = vm::reset)
            }
        }
    }
}

/** A cache file the camera app can write into, shared through the app's FileProvider. */
private fun newCaptureTarget(context: Context): Pair<Uri, File> {
    val dir = File(context.cacheDir, "reference").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file
}

@Composable
private fun IdleContent(onCamera: () -> Unit, onGallery: () -> Unit, onFiles: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text("Recreate any pose", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(
        "Pick a photo of a pose you like. PosePilot reads the pose out of it and then coaches the person " +
            "in front of your camera into the same shape. The photo itself never leaves this screen.",
        style = MaterialTheme.typography.bodyLarge,
        color = Pp.Smoke,
    )
    Spacer(Modifier.height(32.dp))
    PrimaryButton("Take a photo", onCamera, Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    SecondaryButton("Choose from gallery", onGallery, Modifier.fillMaxWidth(), icon = Icons.Outlined.PhotoLibrary)
    Spacer(Modifier.height(12.dp))
    SecondaryButton("Browse files", onFiles, Modifier.fillMaxWidth(), icon = Icons.Outlined.FolderOpen)
}

@Composable
private fun ProcessingContent() {
    Column(
        Modifier.fillMaxWidth().height(320.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Pp.Paper)
        Spacer(Modifier.height(20.dp))
        Text("Reading the pose…", style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke)
    }
}

@Composable
private fun ErrorContent(message: String, onAnother: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text("Couldn't use that photo", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(message, style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke)
    Spacer(Modifier.height(28.dp))
    PrimaryButton("Try another photo", onAnother, Modifier.fillMaxWidth())
}

@Composable
private fun SuccessContent(
    state: ReferenceState.Success,
    onStartCoaching: () -> Unit,
    onSave: (String) -> Unit,
    onAnother: () -> Unit,
) {
    var name by rememberSaveable(state.template.id) { mutableStateOf(state.template.name) }
    var saved by rememberSaveable(state.template.id) { mutableStateOf(false) }

    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).aspectRatio(0.8f).clip(RoundedCornerShape(16.dp)).background(Pp.Graphite)) {
            Image(
                bitmap = state.previewBitmap.asImageBitmap(),
                contentDescription = "Reference photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(Modifier.weight(1f).aspectRatio(0.8f).clip(RoundedCornerShape(16.dp)).background(Pp.Graphite)) {
            SkeletonFigure(state.template.targetPose, Modifier.fillMaxSize().padding(16.dp))
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(
        "${state.template.rules.size} body angles will be coached.",
        style = MaterialTheme.typography.bodyMedium,
        color = Pp.Smoke,
    )
    state.warnings.forEach { warning ->
        Spacer(Modifier.height(6.dp))
        Text("· $warning", style = MaterialTheme.typography.bodyMedium, color = Pp.Smoke)
    }

    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it; saved = false },
        label = { Text("Pose name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Pp.Paper,
            unfocusedTextColor = Pp.Paper,
            focusedBorderColor = Pp.Paper,
            unfocusedBorderColor = Pp.Line,
            focusedLabelColor = Pp.Paper,
            unfocusedLabelColor = Pp.Smoke,
            cursorColor = Pp.Paper,
        ),
    )

    Spacer(Modifier.height(20.dp))
    PrimaryButton("Start coaching", onStartCoaching, Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    SecondaryButton(
        if (saved) "Saved to library" else "Save to library",
        {
            onSave(name.ifBlank { state.template.name })
            saved = true
        },
        Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    SecondaryButton("Choose a different photo", onAnother, Modifier.fillMaxWidth())
    Spacer(Modifier.height(24.dp))
}
