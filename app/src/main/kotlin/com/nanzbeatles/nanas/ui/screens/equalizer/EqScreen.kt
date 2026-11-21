package com.nanzbeatles.nanas.ui.screens.equalizer

import android.annotation.SuppressLint
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.session.PlaybackState
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.nanzbeatles.nanas.ui.utils.frostedGlassSurface
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.eq.data.BuiltInEQPresets
import com.nanzbeatles.nanas.eq.data.GraphicEQBands
import com.nanzbeatles.nanas.eq.data.GraphicEQPreset
import com.nanzbeatles.nanas.eq.data.SavedEQProfile
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 10-Band Graphic Equalizer Screen with preset modes (Rock, Jazz, Pop, Classical, etc.)
 * and dedicated Bass Boost low-frequency enhancer, powered by Media3's
 * [com.nanzbeatles.nanas.eq.audio.CustomEqualizerAudioProcessor].
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun EqScreen(
    viewModel: EQViewModel = hiltViewModel(),
    playbackState: PlaybackState? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    var showError by remember { mutableStateOf<String?>(null) }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "custom_eq.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex >= 0) {
                            val name = cursor.getString(displayNameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importCustomProfile(
                        fileName = fileName,
                        inputStream = inputStream,
                        onSuccess = {
                            Timber.d("Custom EQ profile imported successfully: $fileName")
                        },
                        onError = { error ->
                            showError = context.getString(R.string.import_error_title) + ": " + error.message
                        }
                    )
                } else {
                    showError = context.getString(R.string.error_file_read)
                }
            } catch (e: Exception) {
                showError = context.getString(R.string.error_file_open, e.message)
            }
        }
    }

    EqScreenContent(
        state = state,
        onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
        onPresetSelected = { viewModel.selectPreset(it) },
        onBandGainChanged = { index, gainDb -> viewModel.setBandGain(index, gainDb) },
        onBassBoostChanged = { percent -> viewModel.setBassBoost(percent) },
        onPreampChanged = { viewModel.setPreamp(it) },
        onResetFlat = { viewModel.resetToFlat() },
        onSaveCustomPreset = { showSavePresetDialog = true },
        onProfileSelected = { viewModel.selectProfile(it) },
        onImportCustomEQ = { filePickerLauncher.launch("text/plain") },
        onOpenSystemEqualizer = {
            playerConnection?.let { connection ->
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, connection.player.audioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    activityResultLauncher.launch(intent)
                }
            }
        },
        onDeleteProfile = { viewModel.deleteProfile(it) }
    )

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Simpan Preset EQ") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Nama preset") },
                    placeholder = { Text("Contoh: Bass Favorit") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_save_preset_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveCurrentAsCustomProfile(presetName.ifBlank { "Custom 10-Band" })
                        showSavePresetDialog = false
                    },
                    modifier = Modifier.testTag("eq_save_preset_confirm")
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text(stringResource(R.string.import_error_title)) },
            text = { Text(showError ?: "") },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(R.string.error_eq_apply_failed, state.error ?: "")) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqScreenContent(
    state: EQState,
    onToggleEnabled: (Boolean) -> Unit,
    onPresetSelected: (GraphicEQPreset) -> Unit,
    onBandGainChanged: (Int, Float) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onPreampChanged: (Float) -> Unit,
    onResetFlat: () -> Unit,
    onSaveCustomPreset: () -> Unit,
    onProfileSelected: (String?) -> Unit,
    onImportCustomEQ: () -> Unit,
    onOpenSystemEqualizer: () -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    val activePresetName = remember(state.selectedPresetId, state.profiles, state.isEnabled, state.bassBoostPercent) {
        if (!state.isEnabled) {
            "Nonaktif"
        } else {
            val baseName = BuiltInEQPresets.findById(state.selectedPresetId)?.name
                ?: state.profiles.find { it.id == state.selectedPresetId }?.name
                ?: "Custom"
            if (state.bassBoostPercent > 0) {
                "$baseName • Bass +${state.bassBoostPercent}%"
            } else {
                baseName
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = 700.dp)
            .padding(vertical = 12.dp)
            .frostedGlassSurface(shape = RoundedCornerShape(28.dp), isHighlighted = true)
            .testTag("equalizer_dialog_surface")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header with Master Switch and Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.equalizer_header),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "10-Band Graphic EQ • $activePresetName",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetFlat,
                        modifier = Modifier.testTag("eq_reset_flat_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.replay),
                            contentDescription = "Reset Flat"
                        )
                    }
                    IconButton(
                        onClick = onImportCustomEQ,
                        modifier = Modifier.testTag("eq_import_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = stringResource(R.string.import_profile)
                        )
                    }
                    IconButton(
                        onClick = onOpenSystemEqualizer,
                        modifier = Modifier.testTag("eq_system_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.equalizer),
                            contentDescription = stringResource(R.string.system_equalizer)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                // 1. Preset Modes Row (Flat, Rock, Pop, Jazz, Classical, Hip-Hop, Electronic, etc.)
                item {
                    PresetModesRow(
                        presets = state.presets,
                        selectedPresetId = state.selectedPresetId,
                        isEnabled = state.isEnabled,
                        onPresetSelected = onPresetSelected
                    )
                }

                // 2. Real-time Frequency Response Curve Visualization (includes Bass Boost low-frequency response)
                item {
                    EqFrequencyResponseCurve(
                        bandGains = state.bandGains,
                        bassBoostPercent = state.bassBoostPercent,
                        isEnabled = state.isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // 3. 10-Band Vertical Graphic Equalizer Faders
                item {
                    TenBandGraphicSliders(
                        bandGains = state.bandGains,
                        isEnabled = state.isEnabled,
                        onBandGainChanged = onBandGainChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // 4. Dedicated Bass Boost Control Feature (Low-Frequency Enhancer)
                item {
                    BassBoostControlSection(
                        bassBoostPercent = state.bassBoostPercent,
                        isEnabled = state.isEnabled,
                        onBassBoostChanged = onBassBoostChanged
                    )
                }

                // 5. Preamp Gain & Save Custom Preset Row
                item {
                    PreampAndActionsSection(
                        preampDb = state.preampDb,
                        isEnabled = state.isEnabled,
                        onPreampChanged = onPreampChanged,
                        onResetFlat = onResetFlat,
                        onSaveCustomPreset = onSaveCustomPreset
                    )
                }

                // 6. Saved & Imported Custom EQ Profiles
                val customProfiles = state.profiles.filter { it.isCustom }
                if (customProfiles.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        Text(
                            text = "Profil Tersimpan & AutoEQ (${customProfiles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    items(customProfiles, key = { it.id }) { profile ->
                        EQProfileItem(
                            profile = profile,
                            isSelected = state.isEnabled && state.selectedPresetId == profile.id,
                            onSelected = { onProfileSelected(profile.id) },
                            onDelete = { onDeleteProfile(profile.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetModesRow(
    presets: List<GraphicEQPreset>,
    selectedPresetId: String,
    isEnabled: Boolean,
    onPresetSelected: (GraphicEQPreset) -> Unit
) {
    val scrollState = rememberScrollState()
    val isCustomSelected = selectedPresetId == BuiltInEQPresets.CUSTOM_PRESET_ID

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("eq_presets_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val selected = isEnabled && selectedPresetId == preset.id
            FilterChip(
                selected = selected,
                onClick = { onPresetSelected(preset) },
                label = {
                    Text(
                        text = preset.name,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("eq_preset_chip_${preset.id}")
            )
        }

        if (isCustomSelected) {
            FilterChip(
                selected = isEnabled,
                onClick = { },
                label = {
                    Text(
                        text = "Custom",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier.testTag("eq_preset_chip_custom")
            )
        }
    }
}

@Composable
private fun EqFrequencyResponseCurve(
    bandGains: List<Float>,
    bassBoostPercent: Int,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLowest

    // Compute how Bass Boost contributes to the low-frequency response curve (31 Hz, 62 Hz, 125 Hz, 250 Hz)
    val bassBoostDb = GraphicEQBands.bassBoostPercentToDb(bassBoostPercent)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(surfaceColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height * 0.5f

            // Draw +12 dB, 0 dB, -12 dB horizontal reference lines
            drawLine(
                color = gridColor,
                start = Offset(0f, 4f),
                end = Offset(width, 4f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor.copy(alpha = 0.32f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, height - 4f),
                end = Offset(width, height - 4f),
                strokeWidth = 1.dp.toPx()
            )

            if (bandGains.isEmpty()) return@Canvas

            val count = bandGains.size
            val stepX = if (count > 1) width / (count - 1).toFloat() else width
            val usableHalfHeight = (height * 0.5f) - 6f

            val points = bandGains.mapIndexed { index, gainDb ->
                val x = index * stepX
                val bassContribution = when (index) {
                    0 -> bassBoostDb * 0.78f // 31 Hz sub-bass shelf
                    1 -> bassBoostDb * 0.92f // 62 Hz sub-bass shelf + 60 Hz punch peak
                    2 -> bassBoostDb * 0.38f // 125 Hz upper-bass transition
                    3 -> -bassBoostDb * 0.12f // 250 Hz anti-mud dip
                    else -> 0f
                }
                val combinedDb = (gainDb + bassContribution).coerceIn(
                    GraphicEQBands.MIN_GAIN_DB,
                    GraphicEQBands.MAX_GAIN_DB
                )
                val normalized = (combinedDb / GraphicEQBands.MAX_GAIN_DB).coerceIn(-1f, 1f)
                val y = centerY - (normalized * usableHalfHeight)
                Offset(x, y)
            }

            // Build smooth cubic spline path through all 10 band points
            val curvePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX1 = p0.x + (p1.x - p0.x) * 0.45f
                    val controlX2 = p0.x + (p1.x - p0.x) * 0.55f
                    cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(curvePath)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f),
                        primaryColor.copy(alpha = 0.02f)
                    )
                )
            )

            drawPath(
                path = curvePath,
                color = primaryColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEach { pt ->
                drawCircle(
                    color = primaryColor,
                    radius = 3.5.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun TenBandGraphicSliders(
    bandGains: List<Float>,
    isEnabled: Boolean,
    onBandGainChanged: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("eq_10band_sliders_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GraphicEQBands.LABELS.forEachIndexed { index, freqLabel ->
            val gainDb = bandGains.getOrElse(index) { 0f }
            VerticalEqBandSlider(
                bandIndex = index,
                frequencyLabel = freqLabel,
                gainDb = gainDb,
                isEnabled = isEnabled,
                onGainChanged = { newGain -> onBandGainChanged(index, newGain) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VerticalEqBandSlider(
    bandIndex: Int,
    frequencyLabel: String,
    gainDb: Float,
    isEnabled: Boolean,
    onGainChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnGainChanged by rememberUpdatedState(onGainChanged)
    val activeColor = if (isEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val trackBgColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    val formattedGain = remember(gainDb) {
        val rounded = (gainDb * 2f).roundToInt() / 2f
        when {
            rounded > 0f -> String.format(Locale.US, "+%.1f", rounded)
            rounded < 0f -> String.format(Locale.US, "%.1f", rounded)
            else -> "0.0"
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .testTag("eq_band_column_$bandIndex"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // dB label above fader
        Text(
            text = formattedGain,
            fontSize = 9.sp,
            fontWeight = if (gainDb != 0f && isEnabled) FontWeight.Bold else FontWeight.Normal,
            color = if (gainDb != 0f && isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical touch-drag fader track
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(172.dp)
                .pointerInput(bandIndex) {
                    detectTapGestures(
                        onDoubleTap = {
                            currentOnGainChanged(0f)
                        },
                        onTap = { offset ->
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val paddingPx = 10.dp.toPx()
                            val usableH = (h - 2f * paddingPx).coerceAtLeast(1f)
                            val clampedY = (offset.y - paddingPx).coerceIn(0f, usableH)
                            val fractionFromBottom = 1f - (clampedY / usableH)
                            val db = GraphicEQBands.MIN_GAIN_DB +
                                fractionFromBottom * (GraphicEQBands.MAX_GAIN_DB - GraphicEQBands.MIN_GAIN_DB)
                            val steppedDb = (db * 2f).roundToInt() / 2f
                            currentOnGainChanged(steppedDb)
                        }
                    )
                }
                .pointerInput(bandIndex) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val paddingPx = 10.dp.toPx()
                        val usableH = (h - 2f * paddingPx).coerceAtLeast(1f)
                        val clampedY = (change.position.y - paddingPx).coerceIn(0f, usableH)
                        val fractionFromBottom = 1f - (clampedY / usableH)
                        val db = GraphicEQBands.MIN_GAIN_DB +
                            fractionFromBottom * (GraphicEQBands.MAX_GAIN_DB - GraphicEQBands.MIN_GAIN_DB)
                        val steppedDb = (db * 2f).roundToInt() / 2f
                        currentOnGainChanged(steppedDb)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width * 0.5f
                val paddingPx = 10.dp.toPx()
                val topY = paddingPx
                val bottomY = size.height - paddingPx
                val centerY = (topY + bottomY) * 0.5f
                val usableHalfH = (bottomY - topY) * 0.5f

                // 0 dB center tick mark
                drawLine(
                    color = zeroLineColor,
                    start = Offset(centerX - 7.dp.toPx(), centerY),
                    end = Offset(centerX + 7.dp.toPx(), centerY),
                    strokeWidth = 1.2.dp.toPx()
                )

                // Full background vertical track
                drawLine(
                    color = trackBgColor,
                    start = Offset(centerX, topY),
                    end = Offset(centerX, bottomY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val normalized = (gainDb / GraphicEQBands.MAX_GAIN_DB).coerceIn(-1f, 1f)
                val thumbY = centerY - (normalized * usableHalfH)

                // Active fill from 0 dB center line to thumb
                drawLine(
                    color = activeColor,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Thumb outer ring + inner circle
                drawCircle(
                    color = activeColor,
                    radius = 7.5.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency label below fader (31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k)
        Text(
            text = frequencyLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Dedicated Bass Boost Control Feature inside the 10-band equalizer settings.
 * Allows users to enhance low-frequency output (60 Hz sub-punch + 85 Hz low-shelf)
 * from 0% (0.0 dB) up to 100% (+12.0 dB) independently of the 10-band preset curve.
 */
@Composable
private fun BassBoostControlSection(
    bassBoostPercent: Int,
    isEnabled: Boolean,
    onBassBoostChanged: (Int) -> Unit
) {
    val bassDb = remember(bassBoostPercent) {
        GraphicEQBands.bassBoostPercentToDb(bassBoostPercent)
    }
    val quickLevels = remember { listOf(0, 25, 50, 75, 100) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("eq_bass_boost_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bass Boost",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Penguat frekuensi rendah & sub-bass (60–85 Hz)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val badgeText = if (bassBoostPercent > 0) {
                    String.format(Locale.US, "%d%% (+%.1f dB)", bassBoostPercent, bassDb)
                } else {
                    "Off (0.0 dB)"
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled && bassBoostPercent > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.testTag("eq_bass_boost_value_label")
                )
            }

            Slider(
                value = bassBoostPercent.toFloat(),
                onValueChange = { raw ->
                    onBassBoostChanged(raw.roundToInt().coerceIn(0, 100))
                },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eq_bass_boost_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickLevels.forEach { level ->
                    val selected = isEnabled && bassBoostPercent == level
                    val label = if (level == 0) "Off" else "$level%"
                    FilterChip(
                        selected = selected,
                        onClick = { onBassBoostChanged(level) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_bass_boost_chip_$level")
                    )
                }
            }
        }
    }
}

@Composable
private fun PreampAndActionsSection(
    preampDb: Float,
    isEnabled: Boolean,
    onPreampChanged: (Float) -> Unit,
    onResetFlat: () -> Unit,
    onSaveCustomPreset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preamp (Headroom)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            val roundedPreamp = (preampDb * 2f).roundToInt() / 2f
            val preampLabel = if (roundedPreamp > 0f) {
                String.format(Locale.US, "+%.1f dB", roundedPreamp)
            } else {
                String.format(Locale.US, "%.1f dB", roundedPreamp)
            }
            Text(
                text = preampLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = preampDb,
            onValueChange = { raw ->
                val stepped = (raw * 2f).roundToInt() / 2f
                onPreampChanged(stepped)
            },
            valueRange = GraphicEQBands.MIN_GAIN_DB..GraphicEQBands.MAX_GAIN_DB,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eq_preamp_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onResetFlat,
                modifier = Modifier
                    .weight(1f)
                    .testTag("eq_flat_action_button")
            ) {
                Text("Reset (Flat)")
            }
            OutlinedButton(
                onClick = onSaveCustomPreset,
                modifier = Modifier
                    .weight(1f)
                    .testTag("eq_save_custom_button")
            ) {
                Text("Simpan Preset")
            }
        }
    }
}

@Composable
private fun EQProfileItem(
    profile: SavedEQProfile,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = profile.deviceModel,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val bandsText = if (profile.bands.size == 1) "${profile.bands.size} band" else "${profile.bands.size} bands"
            val bassText = if (profile.bassBoostDb > 0.05) {
                String.format(Locale.US, " • Bass Boost +%.1f dB", profile.bassBoostDb)
            } else {
                ""
            }
            Text(bandsText + bassText)
        },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )
        },
        trailingContent = {
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.delete_profile_desc),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = Modifier
            .clickable(onClick = onSelected)
            .padding(horizontal = 8.dp)
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_profile_desc)) },
            text = {
                Text(stringResource(R.string.delete_profile_confirmation, profile.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}