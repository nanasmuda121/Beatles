/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.SleepTimerDefaultDurationKey
import com.nanzbeatles.nanas.constants.SleepTimerFadeOutDurationKey
import com.nanzbeatles.nanas.constants.SleepTimerFadeOutEnabledKey
import com.nanzbeatles.nanas.utils.makeTimeString
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current
    val sleepTimer = playerConnection?.service?.sleepTimer

    val (defaultMinutes, onDefaultMinutesChange) = rememberPreference(
        SleepTimerDefaultDurationKey,
        defaultValue = 30f
    )
    val (fadeOutEnabled, onFadeOutEnabledChange) = rememberPreference(
        SleepTimerFadeOutEnabledKey,
        defaultValue = true
    )
    val (fadeOutDurationSec, onFadeOutDurationSecChange) = rememberPreference(
        SleepTimerFadeOutDurationKey,
        defaultValue = 30f
    )

    var selectedMinutes by remember(defaultMinutes) {
        mutableFloatStateOf(defaultMinutes.coerceIn(1f, 120f))
    }

    val isTimerActive = sleepTimer?.isActive == true
    val pauseWhenSongEnd = sleepTimer?.pauseWhenSongEnd == true
    val triggerTime = sleepTimer?.triggerTime ?: -1L
    val totalDurationMs = sleepTimer?.totalDurationMs ?: 0L
    val isFadingOut = sleepTimer?.isFadingOut == true
    val fadeMultiplier = sleepTimer?.fadeVolumeMultiplier ?: 1f

    var timeLeftMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isTimerActive, pauseWhenSongEnd, triggerTime) {
        if (isTimerActive && sleepTimer != null) {
            while (isActive && sleepTimer.isActive) {
                timeLeftMs = if (sleepTimer.pauseWhenSongEnd) {
                    val dur = playerConnection.player.duration
                    val pos = playerConnection.player.currentPosition
                    if (dur != C.TIME_UNSET && dur > 0L) (dur - pos).coerceAtLeast(0L) else 0L
                } else {
                    (sleepTimer.triggerTime - System.currentTimeMillis()).coerceAtLeast(0L)
                }
                delay(200L)
            }
        } else {
            timeLeftMs = 0L
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTimerActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bedtime),
                            contentDescription = null,
                            tint = if (isTimerActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.sleep_timer),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isTimerActive) {
                                "Timer aktif • Transisi volume lembut"
                            } else {
                                "Hentikan musik otomatis dengan pudar volume halus"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active Timer Live Card
                AnimatedVisibility(
                    visible = isTimerActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (pauseWhenSongEnd) {
                                    stringResource(R.string.end_of_song)
                                } else {
                                    makeTimeString(timeLeftMs)
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            if (pauseWhenSongEnd && timeLeftMs > 0L) {
                                Text(
                                    text = "Sisa durasi lagu: ${makeTimeString(timeLeftMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            } else if (!pauseWhenSongEnd && totalDurationMs > 0L) {
                                val progress = (timeLeftMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
                                )
                            }

                            // Live Fade-Out Status Indicator
                            if (isFadingOut) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.volume_down),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Memudarkan volume perlahan... ${(fadeMultiplier * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else if (fadeOutEnabled) {
                                Text(
                                    text = "Volume akan memudar lembut ${fadeOutDurationSec.roundToInt()} detik sebelum berhenti",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Quick Extend / Cancel Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { sleepTimer?.addTime(5) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sleep_timer_add_5m"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("+5 Mnt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { sleepTimer?.addTime(15) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sleep_timer_add_15m"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("+15 Mnt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        sleepTimer?.clear()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("sleep_timer_cancel_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Matikan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Timer Duration Selector Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Durasi Waktu Tidur",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.minute,
                                        selectedMinutes.roundToInt(),
                                        selectedMinutes.roundToInt()
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Slider(
                            value = selectedMinutes,
                            onValueChange = {
                                selectedMinutes = it.roundToInt().toFloat()
                                onDefaultMinutesChange(selectedMinutes)
                            },
                            valueRange = 1f..120f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sleep_timer_duration_slider")
                        )

                        // Preset Duration Chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 15, 30, 45, 60, 90).forEach { minutes ->
                                val isPresetSelected = selectedMinutes.roundToInt() == minutes && !pauseWhenSongEnd
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedMinutes = minutes.toFloat()
                                            onDefaultMinutesChange(minutes.toFloat())
                                            sleepTimer?.start(
                                                minute = minutes,
                                                fadeOut = fadeOutEnabled,
                                                fadeDurationSec = fadeOutDurationSec.roundToInt()
                                            )
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPresetSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = if (isPresetSelected) null else BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = "$minutes mnt",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPresetSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // End of Song Option Button
                        OutlinedButton(
                            onClick = {
                                sleepTimer?.start(
                                    minute = -1,
                                    fadeOut = fadeOutEnabled,
                                    fadeDurationSec = fadeOutDurationSec.roundToInt()
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sleep_timer_end_of_song_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.end_of_song),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Gradual Volume Fade-Out Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = !fadeOutEnabled
                                    onFadeOutEnabledChange(next)
                                    sleepTimer?.updateFadeSettings(next, fadeOutDurationSec.roundToInt())
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.volume_down),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Pudarkan Volume Perlahan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Turunkan volume bertahap sebelum jeda agar transisi tidur tenang",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = fadeOutEnabled,
                                onCheckedChange = { enabled ->
                                    onFadeOutEnabledChange(enabled)
                                    sleepTimer?.updateFadeSettings(enabled, fadeOutDurationSec.roundToInt())
                                },
                                modifier = Modifier.testTag("sleep_timer_fade_switch"),
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (fadeOutEnabled) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = fadeOutEnabled,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Durasi Pudar (Fade-Out)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${fadeOutDurationSec.roundToInt()} detik",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = fadeOutDurationSec.coerceIn(5f, 120f),
                                    onValueChange = { sec ->
                                        val rounded = ((sec / 5f).roundToInt() * 5).coerceIn(5, 120).toFloat()
                                        onFadeOutDurationSecChange(rounded)
                                        sleepTimer?.updateFadeSettings(fadeOutEnabled, rounded.roundToInt())
                                    },
                                    valueRange = 5f..120f,
                                    steps = (120 - 5) / 5 - 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sleep_timer_fade_duration_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(10, 30, 60, 90, 120).forEach { sec ->
                                        val isFadeChipSelected = fadeOutDurationSec.roundToInt() == sec
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onFadeOutDurationSecChange(sec.toFloat())
                                                    sleepTimer?.updateFadeSettings(fadeOutEnabled, sec)
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isFadeChipSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                            border = if (isFadeChipSelected) null else BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${sec}d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isFadeChipSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isFadeChipSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    Button(
                        onClick = {
                            val mins = selectedMinutes.roundToInt().coerceAtLeast(1)
                            sleepTimer?.start(
                                minute = mins,
                                fadeOut = fadeOutEnabled,
                                fadeDurationSec = fadeOutDurationSec.roundToInt()
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("sleep_timer_start_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isTimerActive) {
                                "Atur Ulang (${selectedMinutes.roundToInt()} mnt)"
                            } else {
                                "Mulai (${selectedMinutes.roundToInt()} mnt)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
