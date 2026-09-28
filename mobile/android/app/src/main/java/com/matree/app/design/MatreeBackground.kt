package com.matree.app.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun MatreeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = MatreeTheme.tokens
    val colors = tokens.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(tokens.gradients.background),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minSide = min(w, h)

            when (tokens.atmosphere) {
                AtmosphereStyle.NEUTRAL_HALO -> {
                    drawCircle(
                        color = colors.brandAccent.copy(alpha = 0.08f),
                        radius = minSide * 0.48f,
                        center = Offset(w * 0.86f, h * 0.08f),
                    )
                    drawCircle(
                        color = colors.brandSecondary.copy(alpha = 0.06f),
                        radius = minSide * 0.36f,
                        center = Offset(w * 0.06f, h * 0.72f),
                    )
                }

                AtmosphereStyle.WARM_ARCH -> {
                    val stroke = Stroke(width = 1.25.dp.toPx())
                    repeat(3) { index ->
                        drawCircle(
                            color = colors.brandAccent.copy(alpha = 0.08f - index * 0.015f),
                            radius = minSide * (0.42f + index * 0.13f),
                            center = Offset(w * 0.5f, h * 0.05f),
                            style = stroke,
                        )
                    }
                }

                AtmosphereStyle.GEOMETRIC -> {
                    val step = 48.dp.toPx()
                    var y = 0f
                    while (y < h) {
                        var x = 0f
                        while (x < w) {
                            val r = 11.dp.toPx()
                            drawRect(
                                color = colors.brandPrimary.copy(alpha = 0.025f),
                                topLeft = Offset(x + r, y),
                                size = androidx.compose.ui.geometry.Size(r * 1.35f, r * 1.35f),
                                style = Stroke(width = 1.dp.toPx()),
                            )
                            x += step
                        }
                        y += step
                    }
                }

                AtmosphereStyle.SOFT_LIGHT -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = minSide * 0.75f,
                        center = Offset(w * 0.68f, 0f),
                    )
                    drawCircle(
                        color = colors.brandSecondary.copy(alpha = 0.07f),
                        radius = minSide * 0.46f,
                        center = Offset(w * 0.12f, h * 0.55f),
                    )
                }

                AtmosphereStyle.INDIGO_WEAVE -> {
                    val step = 34.dp.toPx()
                    var x = -h
                    while (x < w) {
                        drawLine(
                            color = colors.brandPrimary.copy(alpha = 0.028f),
                            start = Offset(x, 0f),
                            end = Offset(x + h, h),
                            strokeWidth = 1.dp.toPx(),
                        )
                        x += step
                    }
                }
            }
        }

        content()
    }
}
