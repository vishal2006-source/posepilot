package com.posepilot.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.posepilot.app.targetpose.PoseLibrary
import com.posepilot.app.ui.components.NavRow
import com.posepilot.app.ui.components.SkeletonFigure
import com.posepilot.app.ui.components.lerpPose
import com.posepilot.app.ui.theme.Pp
import kotlin.math.floor

@Composable
fun HomeScreen(
    onCoach: () -> Unit,
    onReference: () -> Unit,
    onLibrary: () -> Unit,
    onPhotos: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Pp.Ink).safeDrawingPadding().verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("POSEPILOT", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
        }

        MorphingFigure(Modifier.fillMaxWidth().height(260.dp).padding(vertical = 16.dp))

        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Your AI\nphotography coach", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                "Point the camera, pick a pose, and PosePilot tells the person exactly how to move.",
                style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke,
            )
        }
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Pp.Line)

        NavRow("AI Pose Coach", "Live guidance with voice and auto-capture", Icons.Outlined.CameraAlt, onCoach)
        NavRow("Reference Photo", "Recreate the pose from any photo", Icons.Outlined.Image, onReference)
        NavRow("Pose Library", "${PoseLibrary.all.size} ready-made poses", Icons.Outlined.GridView, onLibrary)
        NavRow("My Photos", "Shots you saved with PosePilot", Icons.Outlined.PhotoLibrary, onPhotos)

        Spacer(Modifier.height(24.dp))
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccessibilityNew, contentDescription = null, tint = Pp.Smoke)
            Spacer(Modifier.padding(4.dp))
            Text(
                "All pose analysis runs on this phone. Nothing is uploaded.",
                style = MaterialTheme.typography.bodyMedium, color = Pp.Smoke,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A skeleton that slowly morphs through the library poses — shows what the app does at a glance. */
@Composable
private fun MorphingFigure(modifier: Modifier) {
    val poses = remember { PoseLibrary.all.map { it.targetPose } }
    val transition = rememberInfiniteTransition(label = "morph")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = poses.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(poses.size * 2600, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val index = floor(t).toInt().coerceIn(0, poses.size - 1)
    val local = t - index
    // Hold each pose for ~60% of its slot, then ease into the next one.
    val blend = ((local - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val eased = blend * blend * (3 - 2 * blend)
    val pose = lerpPose(poses[index], poses[(index + 1) % poses.size], eased)
    Box(modifier, contentAlignment = Alignment.Center) {
        SkeletonFigure(pose, Modifier.fillMaxSize(), stroke = 4.dp)
    }
}
