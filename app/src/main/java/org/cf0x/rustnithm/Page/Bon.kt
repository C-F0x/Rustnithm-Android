package org.cf0x.rustnithm.Page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Bon() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val themeMode by dataManager.themeMode.collectAsState()
    val seedColorLong by dataManager.seedColor.collectAsState()
    val percentPage by dataManager.percentPage.collectAsState()
    val multiA by dataManager.multiA.collectAsState()
    val multiS by dataManager.multiS.collectAsState()
    val enableVibration by dataManager.enableVibration.collectAsState()
    val currentPrimary = MaterialTheme.colorScheme.primary
    val haptic = remember { Haptic.getInstance() }
    val focusManager = LocalFocusManager.current
    val accessCodes by dataManager.accessCodes.collectAsState()
    var textFieldValue by remember(accessCodes) { mutableStateOf(accessCodes) }
    var isError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            if (mimeType == "application/json") {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { r -> r.readText() }
                    dataManager.applySkinConfig(json)
                }
            } else if (mimeType?.startsWith("image/") == true) {
                dataManager.updateBackgroundAndPalette(it, context)
            }
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topAppBarHeight = 64.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = statusBarHeight + topAppBarHeight, bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showInfoDialog = true }) { Icon(Icons.Default.Info, null) }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf("Light", "Dark", "System")
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { dataManager.updateThemeMode(index) },
                                selected = themeMode == index
                            ) { Text(label) }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    ListItem(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showColorPickerDialog = true }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        headlineContent = { Text("Skin Seed Color") },
                        leadingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(seedColorLong))) },
                        trailingContent = { IconButton(onClick = { dataManager.updateSeedColor(currentPrimary.toArgb().toLong()) }) { Icon(Icons.Default.Build, null) } }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Split Ratio: ${(percentPage * 100).roundToInt()}%")
                    Slider(value = percentPage, onValueChange = { dataManager.updatePercentPage(it) }, valueRange = 0.1f..0.9f)
                    Text("Air Sensitivity: ${"%.2f".format(multiA)}")
                    Slider(value = multiA, onValueChange = { dataManager.updateMultiA(it) }, valueRange = 0f..0.5f)
                    Text("Slide Sensitivity: ${"%.2f".format(multiS)}")
                    Slider(value = multiS, onValueChange = { dataManager.updateMultiS(it) }, valueRange = 0f..0.5f)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            if (isError) isError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Access Codes (20 Digits)") },
                        isError = isError,
                        visualTransformation = if (passwordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle mask"
                                    )
                                }
                                IconButton(onClick = {
                                    val isValid = textFieldValue.length == 20 && textFieldValue.all { it.isDigit() }
                                    if (isValid) {
                                        isError = false
                                        dataManager.updateAccessCodes(textFieldValue)
                                        focusManager.clearFocus()
                                    } else {
                                        isError = true
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Save",
                                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        supportingText = {
                            if (isError) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Must be exactly 20 digits", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Haptic Feedback") },
                    trailingContent = {
                        Switch(
                            checked = enableVibration,
                            onCheckedChange = {
                                dataManager.updateEnableVibration(it)
                                if (!it) haptic.stop()
                            }
                        )
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("application/json", "image/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import")
                }

                OutlinedButton(
                    onClick = { scope.launch { dataManager.resetBackgroundAndSkin() } },
                    modifier = Modifier.wrapContentSize(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }

        item {
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Settings")
            }
        }
    }


    if (showColorPickerDialog) {
        val initialColor = Color(seedColorLong)
        val hsv = remember {
            val res = FloatArray(3)
            android.graphics.Color.colorToHSV(initialColor.toArgb(), res)
            res
        }
        var hue by remember { mutableFloatStateOf(hsv[0]) }
        var saturation by remember { mutableFloatStateOf(hsv[1]) }
        var value by remember { mutableFloatStateOf(hsv[2]) }
        val currentColor = remember(hue, saturation, value) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
        }

        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Pick Seed Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(currentColor)
                    )
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                    Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
                }
            },
            confirmButton = {
                Button(onClick = {
                    dataManager.updateSeedColor(currentColor.toArgb().toLong())
                    showColorPickerDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showColorPickerDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About Rustnithm") },
            text = { Text("Customizable rhythm controller.") },
            confirmButton = {
                TextButton(onClick = {
                    showInfoDialog = false
                }) { Text("OK") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All?") },
            text = { Text("This will restore all configurations (including keys and skin) to factory defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        dataManager.resetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset Now") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                }) { Text("Cancel") }
            }
        )
    }
}