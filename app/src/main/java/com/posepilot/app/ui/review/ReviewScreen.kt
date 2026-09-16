package com.posepilot.app.ui.review

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.posepilot.app.models.CheckStatus
import com.posepilot.app.models.QualityCheck
import com.posepilot.app.ui.components.PrimaryButton
import com.posepilot.app.ui.components.SecondaryButton
import com.posepilot.app.ui.components.TopBar
import com.posepilot.app.ui.theme.Pp

@Composable
fun ReviewScreen(vm: ReviewViewModel, onRetake: () -> Unit, onDone: () -> Unit) {
    val analysis by vm.analysis.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val photo = vm.photo

    Column(Modifier.fillMaxSize().background(Pp.Ink).safeDrawingPadding()) {
        TopBar("Review", onRetake)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            if (photo == null) {
                Spacer(Modifier.height(40.dp))
                Text("That photo is no longer available.", style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke)
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Back to coaching", onRetake, Modifier.fillMaxWidth())
                return@Column
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(photo.width.toFloat() / photo.height.toFloat())
                    .clip(RoundedCornerShape(18.dp))
                    .background(Pp.Graphite),
            ) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Captured photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(Modifier.height(24.dp))
            when (val report = analysis) {
                null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Pp.Paper, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Checking the shot…", style = MaterialTheme.typography.bodyLarge, color = Pp.Smoke)
                }

                else -> {
                    Text(report.summary, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    report.checks.forEach { CheckRow(it) }
                }
            }

            Spacer(Modifier.height(28.dp))
            when (val s = saveState) {
                SaveState.Saved -> {
                    Text("Saved to Pictures/PosePilot", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Done", onDone, Modifier.fillMaxWidth())
                }

                else -> {
                    if (s is SaveState.Error) {
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = Pp.Smoke)
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton("Retake", onRetake, Modifier.weight(1f))
                        PrimaryButton(
                            text = if (s == SaveState.Saving) "Saving…" else "Save",
                            onClick = vm::save,
                            modifier = Modifier.weight(1f),
                            enabled = s != SaveState.Saving,
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CheckRow(check: QualityCheck) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (check.status) {
                CheckStatus.PASS -> Icons.Outlined.CheckCircle
                CheckStatus.WARN -> Icons.Outlined.WarningAmber
                CheckStatus.FAIL -> Icons.Outlined.ErrorOutline
                CheckStatus.UNAVAILABLE -> Icons.Outlined.HelpOutline
            }
            Icon(
                icon,
                contentDescription = check.status.name,
                tint = if (check.status == CheckStatus.UNAVAILABLE) Pp.Smoke else Pp.Paper,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(check.name, style = MaterialTheme.typography.titleMedium)
                Text(check.detail, style = MaterialTheme.typography.bodyMedium, color = Pp.Smoke)
            }
        }
        HorizontalDivider(color = Pp.Line, thickness = 1.dp)
    }
}
