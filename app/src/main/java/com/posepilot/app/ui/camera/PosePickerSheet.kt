package com.posepilot.app.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.posepilot.app.targetpose.PoseTemplate
import com.posepilot.app.ui.components.SkeletonFigure
import com.posepilot.app.ui.theme.Pp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosePickerSheet(
    templates: List<PoseTemplate>,
    selectedId: String,
    onSelect: (PoseTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Pp.Graphite) {
        Text("Choose a pose", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 520.dp),
        ) {
            items(templates, key = { it.id }) { t ->
                PoseTile(t, selected = t.id == selectedId, onClick = { onSelect(t) })
            }
        }
    }
}

@Composable
fun PoseTile(template: PoseTemplate, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Pp.Paper else Pp.Ink)
            .border(BorderStroke(1.dp, if (selected) Pp.Paper else Pp.Line), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        SkeletonFigure(
            template.targetPose,
            Modifier.fillMaxWidth().aspectRatio(0.8f),
            color = if (selected) Pp.Ink else Pp.Paper,
            stroke = 2.5.dp,
        )
        Text(
            template.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Pp.Ink else Pp.Paper,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
