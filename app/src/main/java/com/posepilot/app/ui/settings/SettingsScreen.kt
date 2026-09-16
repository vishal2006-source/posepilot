package com.posepilot.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.posepilot.app.data.AppContainer
import com.posepilot.app.models.UserSettings
import com.posepilot.app.ui.components.TopBar
import com.posepilot.app.ui.theme.Pp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settings.settings.collectAsState(initial = UserSettings())
    val scope = rememberCoroutineScope()
    fun update(transform: (UserSettings) -> UserSettings) {
        scope.launch { container.settings.update(transform) }
    }

    Column(Modifier.fillMaxSize().background(Pp.Ink).safeDrawingPadding()) {
        TopBar("Settings", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            SectionLabel("Coaching")
            SwitchRow("Voice guidance", "Speak each instruction out loud", settings.voiceGuidance) { on ->
                update { it.copy(voiceGuidance = on) }
            }
            SwitchRow("Auto-capture", "Take the shot once the pose is held", settings.autoCapture) { on ->
                update { it.copy(autoCapture = on) }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Countdown before capture")
            ChoiceRow(
                options = listOf(2 to "2s", 3 to "3s", 5 to "5s"),
                selected = settings.countdownSeconds,
                onSelect = { seconds -> update { it.copy(countdownSeconds = seconds) } },
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("How often to speak")
            ChoiceRow(
                options = listOf(1500L to "Often", 2500L to "Normal", 4000L to "Rarely"),
                selected = settings.instructionIntervalMs,
                onSelect = { interval -> update { it.copy(instructionIntervalMs = interval) } },
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("Overlay")
            SwitchRow("Live skeleton", "Draw the detected body over the preview", settings.showSkeleton) { on ->
                update { it.copy(showSkeleton = on) }
            }
            SwitchRow("Target pose", "Draw the ghost of the pose to match", settings.showTargetPose) { on ->
                update { it.copy(showTargetPose = on) }
            }
            SwitchRow("Match percentage", "Show the live match score", settings.showMatchPercentage) { on ->
                update { it.copy(showMatchPercentage = on) }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Camera")
            SwitchRow("Mirror front camera", "Save selfies the way they look in the preview", settings.mirrorFrontCamera) { on ->
                update { it.copy(mirrorFrontCamera = on) }
            }
            SwitchRow("Start on front camera", "Open the coach with the selfie camera", settings.useFrontCamera) { on ->
                update { it.copy(useFrontCamera = on) }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Pose detection, photo analysis and speech all run on this phone. No photo or video ever leaves it.",
                style = MaterialTheme.typography.bodyMedium,
                color = Pp.Smoke,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Pp.Smoke,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Pp.Smoke)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Pp.Ink,
                    checkedTrackColor = Pp.Paper,
                    checkedBorderColor = Pp.Paper,
                    uncheckedThumbColor = Pp.Smoke,
                    uncheckedTrackColor = Pp.Graphite,
                    uncheckedBorderColor = Pp.Line,
                ),
            )
        }
        HorizontalDivider(color = Pp.Line, thickness = 1.dp)
    }
}

@Composable
private fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (isSelected) Pp.Paper else Pp.Ink)
                    .border(BorderStroke(1.dp, if (isSelected) Pp.Paper else Pp.Line), shape)
                    .clickable { onSelect(value) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Pp.Ink else Pp.Paper,
                )
            }
        }
    }
}
