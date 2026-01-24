package org.cf0x.rustnithm.Page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic
import org.cf0x.rustnithm.Data.Net
import org.cf0x.rustnithm.Data.TouchLogic
import org.cf0x.rustnithm.Theme.DefaultGameSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Jour() {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = remember { Haptic.getInstance() }
    LaunchedEffect(view) {
        haptic.attachView(view)
    }

    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val savedIp by dataManager.targetIp.collectAsState()
    val savedPort by dataManager.targetPort.collectAsState()
    val bgUri by dataManager.backgroundImage.collectAsState()
    val percentPage by dataManager.percentPage.collectAsState()
    val multiA by dataManager.multiA.collectAsState()
    val multiS by dataManager.multiS.collectAsState()
    val seedColorLong by dataManager.seedColor.collectAsState()
    val themeMode by dataManager.themeMode.collectAsState()
    val isVibrationEnabled by dataManager.enableVibration.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    var isConnected by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf("") }
    var tempPort by remember { mutableStateOf("") }

    val accessCodes by dataManager.accessCodes.collectAsState()

    LaunchedEffect(savedIp, savedPort) {
        if (tempIp.isEmpty()) tempIp = savedIp
        if (tempPort.isEmpty()) tempPort = savedPort
    }

    var activatedAir by remember { mutableStateOf(setOf<Int>()) }
    var activatedSlide by remember { mutableStateOf(setOf<Int>()) }
    var lastAir by remember { mutableStateOf(setOf<Int>()) }
    var lastSlide by remember { mutableStateOf(setOf<Int>()) }
    var touchPoints by remember { mutableStateOf(mapOf<PointerId, Offset>()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var coinPressed by remember { mutableStateOf(false) }
    var servicePressed by remember { mutableStateOf(false) }
    var testPressed by remember { mutableStateOf(false) }
    var cardPressed by remember { mutableStateOf(false) }

    val isReallyDark = when (themeMode) {
        0 -> false
        1 -> true
        else -> isSystemDark
    }
    LaunchedEffect(isConnected, activatedAir, activatedSlide, coinPressed, servicePressed, testPressed, cardPressed) {
        if (isConnected) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    Net.sendFullState(
                        air = activatedAir,
                        slide = activatedSlide,
                        coin = coinPressed,
                        service = servicePressed,
                        test = testPressed,
                        isCardActive = cardPressed,
                        accessCode = accessCodes
                    )
                    delay(1)
                }
            }
        }
    }

    LaunchedEffect(activatedAir, activatedSlide, touchPoints) {
        if (isVibrationEnabled) {
            val newAir = activatedAir - lastAir
            val newSlide = activatedSlide - lastSlide

            if (newAir.isNotEmpty() || newSlide.isNotEmpty()) {
                haptic.onZoneActivated()
            } else if (touchPoints.isNotEmpty()) {
                haptic.onMoveSimulated()
            }
        }
        lastAir = activatedAir
        lastSlide = activatedSlide
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 58.dp)
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentPoints = event.changes
                                .filter { it.pressed }
                                .associate { it.id to it.position }

                            touchPoints = currentPoints

                            if (containerSize.width > 0 && containerSize.height > 0) {
                                val totalW = containerSize.width.toFloat()
                                val totalH = containerSize.height.toFloat()
                                val airH = totalH * percentPage
                                val slideH = totalH - airH

                                activatedAir = TouchLogic.getActivatedAir(currentPoints.values, airH, multiA)
                                activatedSlide = TouchLogic.getActivatedSlide(currentPoints.values, totalW, airH, slideH, multiS)
                            }
                        }
                    }
                }
        ) {
            bgUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.4f
                )
            }
            DefaultGameSkin(
                activatedAir = activatedAir,
                activatedSlide = activatedSlide,
                airWeight = percentPage,
                slideWeight = 1f - percentPage,
                multiA = multiA,
                multiS = multiS,
                touchPoints = touchPoints,
                seedColor = Color(seedColorLong),
                isDark = isReallyDark
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.width(110.dp))
            Surface(
                modifier = Modifier.weight(1.5f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1.2f).height(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                            if (tempIp.isEmpty()) Text("IP address", fontSize = 11.sp, color = Color.Gray)
                            BasicTextField(
                                value = tempIp,
                                onValueChange = { if (it.length <= 15) tempIp = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.width(65.dp).height(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                            if (tempPort.isEmpty()) Text("Port", fontSize = 11.sp, color = Color.Gray)
                            BasicTextField(
                                value = tempPort,
                                onValueChange = { if (it.length <= 5) tempPort = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }

                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            if (!isConnected) {
                                dataManager.updateTargetIp(tempIp)
                                dataManager.updateTargetPort(tempPort)
                                Net.start(tempIp.ifEmpty { "127.0.0.1" }, tempPort.toIntOrNull() ?: 8080)
                                isConnected = true
                            } else {
                                Net.stop()
                                isConnected = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val buttons = listOf(
                        Icons.Default.MonetizationOn to { v: Boolean -> coinPressed = v },
                        Icons.Default.Build to { v: Boolean -> servicePressed = v },
                        Icons.Default.Science to { v: Boolean -> testPressed = v },
                        Icons.Default.CreditCard to { v: Boolean -> cardPressed = v }
                    )

                    buttons.forEach { (icon, update) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .pointerInput(isConnected) {
                                    if (isConnected) {
                                        detectTapGestures(
                                            onPress = {
                                                try {
                                                    update(true)
                                                    if (isVibrationEnabled) haptic.onZoneActivated()
                                                    awaitRelease()
                                                } finally {
                                                    update(false)
                                                }
                                            }
                                        )
                                    }
                                },
                            shape = CircleShape,
                            color = if (isConnected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isConnected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Gray.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(130.dp))
        }
    }
}