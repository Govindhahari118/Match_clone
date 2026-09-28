package com.match.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.match.app.ui.theme.MatreeDesign
import com.match.app.ui.theme.MatreeMotif
import kotlin.math.min

/**
 * Lightweight vector ornament used on high-emotion surfaces only.
 * It is decorative, presentation-only and intentionally excluded from semantics.
 */
@Composable
fun MatreeThemeOrnament(
    modifier: Modifier = Modifier,
    motif: MatreeMotif = MatreeDesign.visual.motif,
    ornamentColor: Color? = null,
    ornamentAlpha: Float? = null
) {
    val visual = MatreeDesign.visual
    val color = (ornamentColor ?: visual.ornament)
        .copy(alpha = ornamentAlpha ?: visual.ornamentAlpha)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = min(w, h)
        val stroke = (unit * 0.018f).coerceAtLeast(1.5f)

        when (motif) {
            MatreeMotif.NEUTRAL -> {
                drawCircle(
                    color = color,
                    radius = unit * 0.22f,
                    center = Offset(w * 0.68f, h * 0.34f),
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = color,
                    radius = unit * 0.12f,
                    center = Offset(w * 0.48f, h * 0.56f),
                    style = Stroke(width = stroke)
                )
            }

            MatreeMotif.LOTUS_ARCH, MatreeMotif.LOTUS_STUPA, MatreeMotif.MARBLE_LOTUS -> {
                val cx = w * 0.62f
                val cy = h * 0.48f
                val petalW = unit * 0.18f
                val petalH = unit * 0.30f
                repeat(5) { index ->
                    val shift = (index - 2) * petalW * 0.48f
                    val path = Path().apply {
                        moveTo(cx, cy + petalH * 0.32f)
                        quadraticBezierTo(
                            cx + shift,
                            cy - petalH,
                            cx + shift,
                            cy + petalH * 0.32f
                        )
                        quadraticBezierTo(
                            cx + shift * 0.48f,
                            cy,
                            cx,
                            cy + petalH * 0.32f
                        )
                    }
                    drawPath(path, color = color, style = Stroke(width = stroke))
                }
                if (motif != MatreeMotif.MARBLE_LOTUS) {
                    drawArc(
                        color = color,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.18f, h * 0.14f),
                        size = Size(w * 0.64f, h * 0.72f),
                        style = Stroke(width = stroke)
                    )
                }
            }

            MatreeMotif.GEOMETRIC_ARCH -> {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.12f),
                    size = Size(w * 0.64f, h * 0.76f),
                    style = Stroke(width = stroke)
                )
                val step = unit * 0.16f
                var x = w * 0.28f
                while (x < w * 0.84f) {
                    drawLine(color, Offset(x, h * 0.30f), Offset(x + step, h * 0.56f), stroke, StrokeCap.Round)
                    drawLine(color, Offset(x + step, h * 0.30f), Offset(x, h * 0.56f), stroke, StrokeCap.Round)
                    x += step
                }
            }

            MatreeMotif.STAINED_ARCH -> {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.10f),
                    size = Size(w * 0.58f, h * 0.78f),
                    style = Stroke(width = stroke)
                )
                drawLine(color, Offset(w * 0.51f, h * 0.18f), Offset(w * 0.51f, h * 0.72f), stroke)
                drawLine(color, Offset(w * 0.32f, h * 0.42f), Offset(w * 0.70f, h * 0.42f), stroke)
            }

            MatreeMotif.GOLDEN_ARCH -> {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.12f),
                    size = Size(w * 0.68f, h * 0.72f),
                    style = Stroke(width = stroke * 1.2f)
                )
                drawLine(color, Offset(w * 0.26f, h * 0.48f), Offset(w * 0.74f, h * 0.48f), stroke)
                drawCircle(color, radius = unit * 0.08f, center = Offset(w * 0.50f, h * 0.48f), style = Stroke(width = stroke))
            }

            MatreeMotif.HERITAGE_DECO -> {
                val left = w * 0.24f
                val top = h * 0.22f
                val boxW = w * 0.54f
                val boxH = h * 0.56f
                drawRect(color, Offset(left, top), Size(boxW, boxH), style = Stroke(width = stroke))
                drawLine(color, Offset(left, top), Offset(left + boxW, top + boxH), stroke)
                drawLine(color, Offset(left + boxW, top), Offset(left, top + boxH), stroke)
                drawCircle(color, radius = unit * 0.10f, center = Offset(w * 0.51f, h * 0.50f), style = Stroke(width = stroke))
            }
        }
    }
}

@Composable
fun MatreeThemeOrnamentIcon(modifier: Modifier = Modifier) {
    MatreeThemeOrnament(modifier = modifier.size(72.dp))
}
