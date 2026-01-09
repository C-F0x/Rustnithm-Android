package org.cf0x.rustnithm.Emu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic
import kotlin.math.abs

@Composable
fun AirContent(
    modifier: Modifier = Modifier,
    onStateChanged: (Set<Int>, Int) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val multiA by dataManager.multiA.collectAsState()
    val isVibrationEnabled by dataManager.enableVibration.collectAsState()
    val haptic = remember { Haptic.getInstance() }
    LaunchedEffect(view) {
        haptic.attachView(view)
    }

    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var containerPosition by remember { mutableStateOf(Offset.Zero) }
    var touchPoints by remember { mutableStateOf(emptyMap<PointerId, Offset>()) }
    var lastActivated by remember { mutableStateOf(setOf<Int>()) }

    val activatedRegions = remember(touchPoints, size, multiA) {
        if (size.height <= 0) emptySet()
        else {
            val activated = mutableSetOf<Int>()
            touchPoints.values.forEach { pos ->
                activated.addAll(calculateActivatedRegions(pos.y, size.height, multiA))
            }
            activated
        }
    }
    LaunchedEffect(activatedRegions) {
        if (isVibrationEnabled) {
            val newlyAdded = activatedRegions - lastActivated
            if (newlyAdded.isNotEmpty()) {
                haptic.onZoneActivated()
            }
            lastActivated = activatedRegions
        }
    }

    LaunchedEffect(activatedRegions, touchPoints.size) {
        onStateChanged(activatedRegions, touchPoints.size)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                size = androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat())
                containerPosition = it.positionInRoot()
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()
                        val newPoints = mutableMapOf<PointerId, Offset>()

                        event.changes.forEach { change ->
                            if (change.pressed) {
                                val currentPos = change.position
                                newPoints[change.id] = currentPos
                                if (isVibrationEnabled) {
                                    val prevPos = change.previousPosition
                                    val diff = abs(currentPos.x - prevPos.x) + abs(currentPos.y - prevPos.y)
                                    if (diff > 5f) {
                                        haptic.onMoveSimulated()
                                    }
                                }
                            }
                        }
                        touchPoints = newPoints
                    } while (event.changes.any { it.pressed })
                    touchPoints = emptyMap()
                    lastActivated = emptySet()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            if (h > 0) {
                val regionHeight = h / 6
                for (i in 0 until 6) {
                    val y = h - (i + 1) * regionHeight
                    drawRect(
                        color = Color.Gray.copy(alpha = 0.1f),
                        topLeft = Offset(0f, y),
                        size = androidx.compose.ui.geometry.Size(w, regionHeight),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DebugOverlay(activated: Set<Int>, fingers: Int) {
    Surface(
        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Air Fingers: $fingers", style = MaterialTheme.typography.labelSmall)
            Text("Active Air: ${activated.sorted()}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun calculateActivatedRegions(
    touchY: Float,
    totalHeight: Float,
    multiA: Float,
    numRegions: Int = 6
): Set<Int> {
    val airHeight = totalHeight / numRegions
    val pairHeight = (airHeight * multiA).coerceIn(0f, airHeight / 2f)
    val activated = mutableSetOf<Int>()
    val yFromBottom = totalHeight - touchY

    if (yFromBottom < -20f || yFromBottom > totalHeight + 20f) return emptySet()

    for (i in 0 until numRegions) {
        val irStart = i * airHeight
        val irEnd = (i + 1) * airHeight

        if (yFromBottom >= irStart && yFromBottom < irEnd) {
            if (i > 0 && yFromBottom < (irStart + pairHeight)) activated.add(i)
            if (i < (numRegions - 1) && yFromBottom > (irEnd - pairHeight)) activated.add(i + 2)
            activated.add(i + 1)
        }
    }
    return activated
}