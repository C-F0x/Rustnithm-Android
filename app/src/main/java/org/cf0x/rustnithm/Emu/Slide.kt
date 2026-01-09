package org.cf0x.rustnithm.Emu

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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.TouchLogic
import org.cf0x.rustnithm.Data.Haptic

@Composable
fun SlideContent(
    modifier: Modifier = Modifier,
    onStateChanged: (Set<Int>, Map<Int, Offset>) -> Unit
) {
    val dataManager: DataManager = viewModel(factory = DataManager.Factory(LocalView.current.context))
    val multiS by dataManager.multiS.collectAsState()
    val enableVibration by dataManager.enableVibration.collectAsState()

    val haptic = remember { Haptic.getInstance() }
    val view = LocalView.current

    LaunchedEffect(view) {
        haptic.attachView(view)
    }

    var containerOffset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var touchPoints by remember { mutableStateOf(mapOf<Int, Offset>()) }
    var lastActivatedByFinger by remember { mutableStateOf(mapOf<Int, Set<Int>>()) }
    var lastPositions by remember { mutableStateOf(mapOf<Int, Offset>()) }
    val moveThreshold = 15f

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                containerOffset = it.positionInRoot()
                size = androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat())
            }
            .pointerInput(multiS) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val currentPoints = mutableMapOf<Int, Offset>()
                        val currentActivatedByFinger = mutableMapOf<Int, Set<Int>>()
                        val currentPositions = mutableMapOf<Int, Offset>()

                        event.changes.forEach { change ->
                            if (change.pressed) {
                                val pointerId = change.id.value.toInt()
                                val globalPos = change.position + containerOffset
                                currentPoints[pointerId] = globalPos
                                currentPositions[pointerId] = change.position
                                val activated = TouchLogic.getActivatedSlide(
                                    listOf(globalPos),
                                    size.width,
                                    0f,
                                    size.height,
                                    multiS
                                )
                                currentActivatedByFinger[pointerId] = activated

                                if (enableVibration) {
                                    val lastActivated = lastActivatedByFinger[pointerId] ?: emptySet()
                                    if ((activated - lastActivated).isNotEmpty()) {
                                        haptic.onZoneActivated()
                                    }

                                    if (event.type == PointerEventType.Move) {
                                        val lastPos = lastPositions[pointerId]
                                        if (lastPos != null) {
                                            val distance = (change.position - lastPos).getDistance()
                                            if (distance > moveThreshold) {
                                                haptic.onMoveSimulated()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        touchPoints = currentPoints
                        lastActivatedByFinger = currentActivatedByFinger
                        lastPositions = currentPositions

                        val allActivated = currentActivatedByFinger.values.flatten().toSet()
                        onStateChanged(allActivated, touchPoints)
                    }
                }
            }
    ) {
        if (touchPoints.isNotEmpty()) {
            DebugOverlay(
                activated = lastActivatedByFinger.values.flatten().toSet(),
                fingers = touchPoints.size
            )
        }
    }
}

@Composable
private fun BoxScope.DebugOverlay(activated: Set<Int>, fingers: Int) {
    Surface(
        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Slide Fingers: $fingers", style = MaterialTheme.typography.labelSmall)
            Text("Active Keys: ${activated.sorted()}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

