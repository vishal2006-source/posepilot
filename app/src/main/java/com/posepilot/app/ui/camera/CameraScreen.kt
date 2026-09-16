package com.posepilot.app.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.posepilot.app.camera.CameraSession
import com.posepilot.app.data.AppContainer
import com.posepilot.app.models.GuidancePhase
import com.posepilot.app.ui.components.GlassIconButton
import com.posepilot.app.ui.components.PrimaryButton
import com.posepilot.app.ui.components.SecondaryButton
import com.posepilot.app.ui.components.SkeletonFigure
import com.posepilot.app.ui.theme.Pp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    vm: CoachViewModel,
    container: AppContainer,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onReview: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        askedOnce = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission && !askedOnce) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CoachContent(vm, container, onBack, onSettings, onReview)
        } else {
            PermissionRequired(
                askedOnce = askedOnce,
                onRequest = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                },
                onBack = onBack,
            )
        }
    }

    // Re-check after returning from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun CoachContent(
    vm: CoachViewModel,
    container: AppContainer,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onReview: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val references by container.templates.references.collectAsState()
    var session by remember { mutableStateOf<CameraSession?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var photographerMode by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val flash = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // One-shot events from the ViewModel.
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                CoachEvent.TakePhoto -> {
                    val s = session
                    if (s == null) {
                        vm.onCaptureFailed("Camera not ready yet")
                    } else if (!vm.state.value.isCapturing) {
                        vm.onCaptureStarted()
                        scope.launch { flash.snapTo(0.9f); flash.animateTo(0f, tween(350)) }
                        s.takePhoto(vm.state.value.settings.mirrorFrontCamera, vm::onPhotoCaptured, vm::onCaptureFailed)
                    }
                }
                CoachEvent.OpenReview -> onReview()
                is CoachEvent.Message -> message = event.text
            }
        }
    }
    LaunchedEffect(message) {
        if (message != null) { delay(3000); message = null }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.onResume()
                Lifecycle.Event.ON_PAUSE -> vm.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(vm, state.lens, onSessionReady = { session = it }, modifier = Modifier.fillMaxSize())
        GuidanceOverlay(state, Modifier.fillMaxSize())

        // Capture flash.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }.background(Color.White))

        CountdownOverlay(state.instruction.countdown, Modifier.align(Alignment.Center))

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopControls(
                state = state,
                onBack = onBack,
                onPickPose = { showPicker = true },
                onToggleVoice = { vm.toggleVoice() },
                onFlip = { vm.flipCamera() },
                onSettings = onSettings,
            )
            if (state.lowLight) {
                Row(
                    Modifier.padding(start = 16.dp, top = 4.dp).clip(RoundedCornerShape(14.dp)).background(Pp.Scrim)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Low light — try better lighting", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
            message?.let {
                MessagePill(it, Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, start = 24.dp, end = 24.dp))
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = photographerMode,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                PhotographerPanel(
                    CoachViewModel.MANUAL_INSTRUCTIONS,
                    onInstruction = vm::manualInstruction,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ScoreReadout(
                    state.score,
                    visible = state.settings.showMatchPercentage && state.instruction.phase != GuidancePhase.NO_PERSON,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                )
                InstructionCard(state.instruction, Modifier.fillMaxWidth())
            }

            BottomControls(
                state = state,
                photographerMode = photographerMode,
                onPickPose = { showPicker = true },
                onCapture = { vm.captureNow() },
                onTogglePhotographer = { photographerMode = !photographerMode },
            )
        }

        if (state.cameraError != null) {
            CameraErrorView(state.cameraError!!, onBack, onFlip = { vm.flipCamera() })
        }
    }

    if (showPicker) {
        PosePickerSheet(
            templates = com.posepilot.app.targetpose.PoseLibrary.all + references,
            selectedId = state.template.id,
            onSelect = { vm.selectTemplate(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun TopControls(
    state: CoachUiState,
    onBack: () -> Unit,
    onPickPose: () -> Unit,
    onToggleVoice: () -> Unit,
    onFlip: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Text(
            state.template.name,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Pp.Scrim)
                .clickable(onClick = onPickPose)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        )
        GlassIconButton(
            if (state.settings.voiceGuidance) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            if (state.settings.voiceGuidance) "Mute voice" else "Unmute voice",
            onToggleVoice,
        )
        GlassIconButton(Icons.Outlined.Cameraswitch, "Switch camera", onFlip)
        GlassIconButton(Icons.Outlined.Settings, "Settings", onSettings)
    }
}

@Composable
private fun BottomControls(
    state: CoachUiState,
    photographerMode: Boolean,
    onPickPose: () -> Unit,
    onCapture: () -> Unit,
    onTogglePhotographer: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Target pose thumbnail doubles as the pose picker.
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Pp.Scrim).clickable(onClick = onPickPose).padding(6.dp),
        ) {
            SkeletonFigure(state.template.targetPose, Modifier.fillMaxSize(), stroke = 2.dp)
        }
        Spacer(Modifier.weight(1f))
        ShutterButton(enabled = !state.isCapturing, onClick = onCapture)
        Spacer(Modifier.weight(1f))
        GlassIconButton(Icons.Outlined.RecordVoiceOver, "Photographer mode", onTogglePhotographer, active = photographerMode)
    }
}

@Composable
private fun PermissionRequired(askedOnce: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Camera access needed", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "PosePilot needs the camera to see the person and guide their pose. Video is analysed on this phone and never uploaded.",
            style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(if (askedOnce) "Try again" else "Allow camera", onRequest, Modifier.fillMaxWidth())
        if (askedOnce) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Open app settings", onOpenSettings, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Back", onBack, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CameraErrorView(message: String, onBack: () -> Unit, onFlip: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Pp.Ink).safeDrawingPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Camera unavailable", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke)
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Switch camera", onFlip, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Back", onBack, Modifier.fillMaxWidth())
    }
}
