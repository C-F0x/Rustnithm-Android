package org.cf0x.rustnithm.Theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun DefaultGameSkin(
    activatedAir: Set<Int>,
    activatedSlide: Set<Int>,
    airWeight: Float,
    slideWeight: Float,
    multiA: Float,
    touchPoints: Map<*, Offset>,
    multiS: Float,
    seedColor: Color,
    isDark: Boolean
) {
    val engine = remember(seedColor, isDark) {
        SkinColorEngine(seedColor, isDark)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalHeight = size.height
        val totalWidth = size.width
        val airAreaHeight = totalHeight * (airWeight / (airWeight + slideWeight))
        val slideAreaHeight = totalHeight - airAreaHeight
        val singleAirHeight = airAreaHeight / 6

        for (i in 0 until 6) {
            val rectTopY = airAreaHeight - (i + 1) * singleAirHeight
            val isActive = activatedAir.contains(i + 1)
            drawRect(
                color = engine.getAreaColor(index = i + 1, isActive = isActive),
                topLeft = Offset(0f, rectTopY),
                size = Size(totalWidth, singleAirHeight)
            )
            drawRect(
                color = engine.getDividerColor(index = i + 1),
                topLeft = Offset(0f, rectTopY),
                size = Size(totalWidth, singleAirHeight),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        val sw = totalWidth / 16
        val rslide = sw * multiS
        val sh = slideAreaHeight / 2
        for (index in 0 until 32) {
            val row = if (index % 2 == 0) 0 else 1
            val colFromLeft = 15 - (index / 2)
            val rectOffset = Offset(colFromLeft * sw, airAreaHeight + row * sh)

            val isActive = activatedSlide.contains(index + 1)
            drawRect(
                color = engine.getAreaColor(index = index + 1, isActive = isActive),
                topLeft = rectOffset,
                size = Size(sw, sh)
            )
            drawRect(
                color = engine.getDividerColor(alpha = 0.1f),
                topLeft = rectOffset,
                size = Size(sw, sh),
                style = Stroke(width = 0.5.dp.toPx())
            )
        }
        drawLine(
            color = engine.getDividerColor(index = 0, alpha = 0.6f),
            start = Offset(0f, airAreaHeight + sh),
            end = Offset(totalWidth, airAreaHeight + sh),
            strokeWidth = 1.dp.toPx()
        )
        for (i in 1..15) {
            val lineX = totalWidth - (i * sw)
            drawLine(
                color = engine.getDividerColor(index = i, alpha = 0.6f),
                start = Offset(lineX, airAreaHeight),
                end = Offset(lineX, totalHeight),
                strokeWidth = 1.dp.toPx()
            )
        }
        touchPoints.values.forEach { pos ->
            val pointColor = engine.getAreaColor(isActive = true, alpha = 0.5f)

            drawCircle(
                color = pointColor,
                radius = rslide,
                center = pos,
            )
            drawCircle(
                color = engine.getDividerColor(alpha = 0.8f),
                radius = rslide,
                center = pos,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}