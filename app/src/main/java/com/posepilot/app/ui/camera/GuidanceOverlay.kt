package com.posepilot.app.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.posepilot.app.models.ArrowKind
import com.posepilot.app.models.CorrectionArrow
import com.posepilot.app.models.GuidancePhase
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.models.PoseFrame
import com.posepilot.app.ui.theme.Pp
import com.posepilot.app.utilities.OverlayTransform
import com.posepilot.app.utilities.Vec2
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Faces are drawn as a head circle; the individual eye/ear/mouth points would just be clutter. */
private val FACE_POINTS = setOf(
    LandmarkType.LEFT_EYE_INNER, LandmarkType.LEFT_EYE, LandmarkType.LEFT_EYE_OUTER,
    LandmarkType.RIGHT_EYE_INNER, LandmarkType.RIGHT_EYE, LandmarkType.RIGHT_EYE_OUTER,
    LandmarkType.LEFT_EAR, LandmarkType.RIGHT_EAR, LandmarkType.LEFT_MOUTH, LandmarkType.RIGHT_MOUTH,
)

/**
 * Draws everything that sits on top of the camera image: viewfinder corners, the ghost target skeleton,
 * the live skeleton, the highlighted joint and the animated correction arrow.
 * All inputs are in un-mirrored analysis-image pixels and mapped with [OverlayTransform].
 */
@Composable
fun GuidanceOverlay(state: CoachUiState, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val phaseT by pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "arrow",
    )

    Canvas(modifier) {
        val ready = state.instruction.phase == GuidancePhase.HOLD || state.instruction.phase == GuidancePhase.COUNTDOWN
        drawViewfinderCorners(if (ready) 6f else 2.5f)

        val comparison = state.comparison
        val frame = comparison.frame ?: return@Canvas
        if (state.imageWidth == 0 || state.imageHeight == 0) return@Canvas
        val tf = OverlayTransform(
            state.imageWidth, state.imageHeight, size.width, size.height,
            mirror = state.lens == com.posepilot.app.models.LensFacing.FRONT,
        )
        fun o(p: Vec2) = Offset(tf.x(p.x), tf.y(p.y))

        val torsoPx = torsoLengthPx(frame, tf)
        val stroke = (torsoPx * 0.035f).coerceIn(4f, 14f)

        if (state.settings.showTargetPose && comparison.ghost.isNotEmpty()) {
            drawGhost(comparison.ghost, ::o, stroke * 1.9f, torsoPx, solid = ready)
        }
        if (state.settings.showSkeleton) {
            drawLiveSkeleton(frame, ::o, stroke, torsoPx)
        }

        val instruction = state.instruction
        val top = comparison.corrections.firstOrNull { it.instruction == instruction.text }
        top?.highlight?.let { type ->
            frame[type]?.takeIf { it.visibility >= PoseFrame.VISIBILITY_THRESHOLD }?.let { l ->
                val c = o(l.xy())
                drawCircle(Color.White, radius = stroke * 2.2f + stroke * 2f * phaseT, center = c,
                    alpha = 1f - phaseT, style = Stroke(stroke * 0.6f))
            }
        }
        if (instruction.phase == GuidancePhase.GUIDING && !instruction.isManual) {
            instruction.arrow?.let { drawCorrectionArrow(it, ::o, stroke, phaseT) }
        }
    }
}

private fun torsoLengthPx(frame: PoseFrame, tf: OverlayTransform): Float {
    val ls = frame[LandmarkType.LEFT_SHOULDER]; val rs = frame[LandmarkType.RIGHT_SHOULDER]
    val lh = frame[LandmarkType.LEFT_HIP]; val rh = frame[LandmarkType.RIGHT_HIP]
    if (ls == null || rs == null || lh == null || rh == null) return tf.length(frame.imageHeight * 0.25)
    val dx = (ls.x + rs.x - lh.x - rh.x) / 2
    val dy = (ls.y + rs.y - lh.y - rh.y) / 2
    return tf.length(hypot(dx, dy))
}

private fun DrawScope.drawViewfinderCorners(width: Float) {
    val inset = size.minDimension * 0.06f
    val len = size.minDimension * 0.08f
    val color = Color.White.copy(alpha = 0.85f)
    val l = inset; val t = inset + size.height * 0.08f
    val r = size.width - inset; val b = size.height - inset - size.height * 0.2f
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(color, Offset(x, y), Offset(x + dx * len, y), width, StrokeCap.Round)
        drawLine(color, Offset(x, y), Offset(x, y + dy * len), width, StrokeCap.Round)
    }
    corner(l, t, 1f, 1f); corner(r, t, -1f, 1f); corner(l, b, 1f, -1f); corner(r, b, -1f, -1f)
}

private fun DrawScope.drawGhost(
    ghost: Map<LandmarkType, Vec2>, o: (Vec2) -> Offset, width: Float, torsoPx: Float, solid: Boolean,
) {
    val color = if (solid) Color.White.copy(alpha = 0.75f) else Pp.Ghost.copy(alpha = 0.45f)
    val effect = if (solid) null else PathEffect.dashPathEffect(floatArrayOf(width * 1.6f, width * 1.2f))
    for ((a, b) in LandmarkType.SKELETON_CONNECTIONS) {
        if (a in FACE_POINTS || b in FACE_POINTS) continue
        val pa = ghost[a] ?: continue; val pb = ghost[b] ?: continue
        drawLine(color, o(pa), o(pb), width, StrokeCap.Round, pathEffect = effect)
    }
    ghost[LandmarkType.NOSE]?.let { nose ->
        drawCircle(color, radius = torsoPx * 0.2f, center = o(nose), style = Stroke(width * 0.6f, pathEffect = effect))
    }
}

private fun DrawScope.drawLiveSkeleton(frame: PoseFrame, o: (Vec2) -> Offset, width: Float, torsoPx: Float) {
    fun visible(t: LandmarkType) = frame[t]?.takeIf { it.visibility >= PoseFrame.VISIBILITY_THRESHOLD }
    val shadow = Color.Black.copy(alpha = 0.35f)
    for ((a, b) in LandmarkType.SKELETON_CONNECTIONS) {
        if (a in FACE_POINTS || b in FACE_POINTS) continue
        val la = visible(a) ?: continue; val lb = visible(b) ?: continue
        drawLine(shadow, o(la.xy()), o(lb.xy()), width * 1.8f, StrokeCap.Round)
        drawLine(Color.White, o(la.xy()), o(lb.xy()), width, StrokeCap.Round)
    }
    for (type in LandmarkType.OVERLAY_JOINTS) {
        if (type in FACE_POINTS) continue
        val l = visible(type) ?: continue
        drawCircle(Color.Black, radius = width * 1.1f, center = o(l.xy()))
        drawCircle(Color.White, radius = width * 0.7f, center = o(l.xy()))
    }
    visible(LandmarkType.NOSE)?.let { nose ->
        drawCircle(Color.White, radius = torsoPx * 0.2f, center = o(nose.xy()), style = Stroke(width * 0.6f))
    }
}

/** Animated arrow: LIMB and STEP travel toward the target, ROTATE sweeps around a centre. */
private fun DrawScope.drawCorrectionArrow(arrow: CorrectionArrow, o: (Vec2) -> Offset, stroke: Float, t: Float) {
    val from = o(arrow.from)
    val to = o(arrow.to)
    val alpha = if (t < 0.8f) 1f else (1f - (t - 0.8f) / 0.2f)
    when (arrow.kind) {
        ArrowKind.LIMB -> {
            val tip = lerp(from, to, 0.35f + 0.65f * t)
            drawArrow(from, tip, stroke * 1.3f, Color.White.copy(alpha = alpha))
            drawCircle(Color.White, radius = stroke * 1.4f, center = to, alpha = 0.6f, style = Stroke(stroke * 0.5f))
        }
        ArrowKind.STEP -> {
            // Big chevron-style arrow sliding in the step direction.
            val d = to - from
            val start = from + d * (0.15f * t)
            val end = from + d * (0.85f + 0.15f * t)
            drawArrow(start, end, stroke * 2.6f, Color.White.copy(alpha = alpha), shadow = true)
        }
        ArrowKind.ROTATE -> {
            val radius = (hypot(to.x - from.x, to.y - from.y)).coerceAtLeast(stroke * 6f)
            // Screen-space direction after mirroring decides clockwise vs counter-clockwise.
            val clockwise = to.x > from.x
            val sweep = 150f * (0.4f + 0.6f * t)
            val startAngle = if (clockwise) 200f else -20f
            val signedSweep = if (clockwise) sweep else -sweep
            val rect = Rect(from.x - radius, from.y - radius * 0.55f, from.x + radius, from.y + radius * 0.55f)
            val path = Path().apply { arcTo(rect, startAngle, signedSweep, forceMoveTo = true) }
            drawPath(path, Color.Black.copy(alpha = 0.35f * alpha), style = Stroke(stroke * 2.6f, cap = StrokeCap.Round))
            drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(stroke * 1.4f, cap = StrokeCap.Round))
            // Arrowhead at the end of the arc, tangent to the ellipse.
            val endRad = Math.toRadians((startAngle + signedSweep).toDouble())
            val end = Offset(rect.center.x + radius * cos(endRad).toFloat(), rect.center.y + radius * 0.55f * sin(endRad).toFloat())
            val dir = if (clockwise) 1f else -1f
            val tangent = Offset(-radius * sin(endRad).toFloat() * dir, radius * 0.55f * cos(endRad).toFloat() * dir)
            drawHead(end, atan2(tangent.y, tangent.x), stroke * 1.4f, Color.White.copy(alpha = alpha))
        }
    }
}

private fun DrawScope.drawArrow(from: Offset, to: Offset, width: Float, color: Color, shadow: Boolean = false) {
    val d = to - from
    if (abs(d.x) + abs(d.y) < 1f) return
    val angle = atan2(d.y, d.x)
    if (shadow) {
        drawLine(Color.Black.copy(alpha = 0.3f * color.alpha), from, to, width * 1.6f, StrokeCap.Round)
    }
    drawLine(color, from, to - Offset(cos(angle), sin(angle)) * width, width, StrokeCap.Round)
    drawHead(to, angle, width, color)
}

private fun DrawScope.drawHead(tip: Offset, angle: Float, width: Float, color: Color) {
    val size = width * 3.2f
    val spread = 0.5f
    val p1 = tip - Offset(cos(angle - spread), sin(angle - spread)) * size
    val p2 = tip - Offset(cos(angle + spread), sin(angle + spread)) * size
    val head = Path().apply { moveTo(tip.x, tip.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }
    drawPath(head, color)
    drawPath(head, color, style = Stroke(width * 0.4f, join = StrokeJoin.Round))
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
