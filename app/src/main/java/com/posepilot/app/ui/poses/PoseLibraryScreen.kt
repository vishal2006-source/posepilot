package com.posepilot.app.ui.poses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.posepilot.app.data.AppContainer
import com.posepilot.app.models.FramingType
import com.posepilot.app.targetpose.PoseCategory
import com.posepilot.app.targetpose.PoseLibrary
import com.posepilot.app.targetpose.PoseTemplate
import com.posepilot.app.targetpose.TemplateSource
import com.posepilot.app.ui.components.SkeletonFigure
import com.posepilot.app.ui.components.TopBar
import com.posepilot.app.ui.theme.Pp
import kotlinx.coroutines.launch

@Composable
fun PoseLibraryScreen(container: AppContainer, onBack: () -> Unit, onSelect: (String) -> Unit) {
    val references by container.templates.references.collectAsState()
    val all = PoseLibrary.all + references
    // Only categories that actually contain poses are shown — no empty placeholder tabs.
    val categories = listOf<PoseCategory?>(null) + PoseCategory.entries.filter { c -> all.any { it.category == c } }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = categories.firstOrNull { it?.name == selectedName }
    val shown = if (selected == null) all else all.filter { it.category == selected }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Pp.Ink).safeDrawingPadding()) {
        TopBar("Pose Library", onBack)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it?.name ?: "all" }) { c ->
                CategoryChip(c?.label ?: "All", selected = c == selected) { selectedName = c?.name }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.id }) { t ->
                PoseCard(
                    template = t,
                    onClick = { onSelect(t.id) },
                    onDelete = if (t.source == TemplateSource.REFERENCE_PHOTO) {
                        { scope.launch { container.templates.delete(t.id) } }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Pp.Ink else Pp.Paper,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Pp.Paper else Pp.Ink)
            .border(BorderStroke(1.dp, if (selected) Pp.Paper else Pp.Line), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PoseCard(template: PoseTemplate, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.clip(shape).background(Pp.Graphite).border(BorderStroke(1.dp, Pp.Line), shape).clickable(onClick = onClick),
    ) {
        Box {
            SkeletonFigure(template.targetPose, Modifier.fillMaxWidth().aspectRatio(0.85f).padding(16.dp), stroke = 3.dp)
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete reference pose", tint = Pp.Smoke)
                }
            }
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                template.description, style = MaterialTheme.typography.bodyMedium, color = Pp.Smoke,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    (if (template.framing == FramingType.FULL_BODY) "FULL BODY" else "UPPER BODY") + " · " + template.category.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium, color = Pp.Smoke,
                )
            }
        }
    }
}
