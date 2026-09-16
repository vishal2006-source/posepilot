package com.posepilot.app.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.posepilot.app.models.GuidanceInstruction
import com.posepilot.app.models.InstructionTone
import com.posepilot.app.ui.theme.Pp
import kotlin.math.roundToInt

/** The one instruction the subject should act on now. Success is shown by inverting to a white card. */
@Composable
fun InstructionCard(instruction: GuidanceInstruction, modifier: Modifier = Modifier) {
    val success = instruction.tone == InstructionTone.SUCCESS
    AnimatedContent(
        targetState = instruction.text to instruction.tone,
        transitionSpec = {
            (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 3 }) togetherWith
                (fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 3 })
        },
        label = "instruction",
        modifier = modifier,
    ) { (text, tone) ->
        val inverted = tone == InstructionTone.SUCCESS
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (inverted) Pp.Paper else Pp.Scrim)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when {
                instruction.isManual -> Icons.Outlined.Campaign
                tone == InstructionTone.SUCCESS -> Icons.Outlined.CheckCircle
                tone == InstructionTone.WARNING -> Icons.Outlined.ErrorOutline
                else -> null
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (inverted) Pp.Ink else Color.White, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text,
                    color = if (inverted) Pp.Ink else Color.White,
                    fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
                )
                val secondary = instruction.secondaryText
                if (!success && secondary != null && !instruction.isManual) {
                    Text("Next: $secondary", color = Pp.Smoke, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** "Pose match 87%" with a thin progress rule underneath. */
@Composable
fun ScoreReadout(score: Double, visible: Boolean, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(score.toFloat().coerceIn(0f, 1f), tween(250), label = "score")
    if (!visible) return
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("POSE MATCH", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.weight(1f))
            Text("${(animated * 100).roundToInt()}%", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().background(Color.White))
        }
    }
}

/** Large 3-2-1 in the middle of the preview. */
@Composable
fun CountdownOverlay(countdown: Int?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = countdown,
        transitionSpec = { (scaleIn(tween(250), initialScale = 1.6f) + fadeIn()) togetherWith (scaleOut(tween(200), targetScale = 0.6f) + fadeOut()) },
        label = "countdown",
        modifier = modifier,
    ) { value ->
        if (value != null) {
            Text(
                value.toString(), color = Color.White, fontSize = 140.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Photographer mode: tap to show an instruction on screen and speak it to the subject. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotographerPanel(instructions: List<String>, onInstruction: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Pp.Scrim).padding(12.dp),
    ) {
        Text("TELL THE SUBJECT", style = MaterialTheme.typography.labelMedium, color = Pp.Smoke, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            instructions.forEach { text ->
                Text(
                    text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)), RoundedCornerShape(18.dp))
                        .clickable { onInstruction(text) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
fun MessagePill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Pp.Ink,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(Pp.Paper).padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

/** Round shutter button. Disabled look while a capture is in flight. */
@Composable
fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(78.dp)
            .border(BorderStroke(4.dp, Color.White), CircleShape)
            .padding(8.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White else Color.White.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
    )
}
