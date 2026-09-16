package com.posepilot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.pose.landmarks.NormalizedPose
import com.posepilot.app.utilities.Vec3

/** Draws a normalized pose scaled to fit its bounds. Used for library thumbnails and the home figure. */
@Composable
fun SkeletonFigure(
    pose: NormalizedPose,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    stroke: Dp = 3.dp,
) {
    Canvas(modifier) {
        val pts = pose.points.filterKeys { it in LandmarkType.OVERLAY_JOINTS }
        if (pts.isEmpty()) return@Canvas
        val minX = pts.values.minOf { it.x }; val maxX = pts.values.maxOf { it.x }
        val minY = pts.values.minOf { it.y } - 0.15; val maxY = pts.values.maxOf { it.y }
        val pad = 0.12
        val w = (maxX - minX) + 2 * pad
        val h = (maxY - minY) + 2 * pad
        val scale = minOf(size.width / w, size.height / h).toFloat()
        val ox = size.width / 2f - ((minX + maxX) / 2).toFloat() * scale
        val oy = size.height / 2f - ((minY + maxY) / 2).toFloat() * scale
        fun o(p: Vec3) = Offset(ox + p.x.toFloat() * scale, oy + p.y.toFloat() * scale)
        val sw = stroke.toPx()

        for ((a, b) in LandmarkType.SKELETON_CONNECTIONS) {
            if (a == LandmarkType.NOSE || b == LandmarkType.NOSE || a.name.contains("EYE") || b.name.contains("EYE")) continue
            val pa = pts[a] ?: continue; val pb = pts[b] ?: continue
            drawLine(color, o(pa), o(pb), strokeWidth = sw, cap = StrokeCap.Round)
        }
        val nose = pts[LandmarkType.NOSE]
        val le = pts[LandmarkType.LEFT_EAR]; val re = pts[LandmarkType.RIGHT_EAR]
        if (nose != null && le != null && re != null) {
            val cx = (le.x + re.x) / 2; val cy = (le.y + re.y) / 2 - 0.02
            drawCircle(color, radius = (0.17 * scale).toFloat(), center = Offset(ox + cx.toFloat() * scale, oy + cy.toFloat() * scale), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawCircle(color, radius = sw * 0.9f, center = o(nose))
        }
    }
}

/** Linear blend between two poses (for the animated home figure). */
fun lerpPose(a: NormalizedPose, b: NormalizedPose, t: Float): NormalizedPose {
    val tt = t.toDouble()
    val keys = a.points.keys intersect b.points.keys
    return a.copy(points = keys.associateWith { k ->
        val pa = a.points.getValue(k); val pb = b.points.getValue(k)
        Vec3(pa.x + (pb.x - pa.x) * tt, pa.y + (pb.y - pa.y) * tt, pa.z + (pb.z - pa.z) * tt)
    })
}
