/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    var player: Player,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    private var restoreVolumeJob: Job? = null

    var triggerTime by mutableLongStateOf(-1L)
        private set

    var totalDurationMs by mutableLongStateOf(0L)
        private set

    var pauseWhenSongEnd by mutableStateOf(false)
        private set

    var fadeOutEnabled by mutableStateOf(true)
        private set

    var fadeOutDurationSec by mutableIntStateOf(30)
        private set

    var isFadingOut by mutableStateOf(false)
        private set

    var fadeVolumeMultiplier by mutableFloatStateOf(1f)
        private set

    private val _fadeVolumeMultiplierFlow = MutableStateFlow(1f)
    val fadeVolumeMultiplierFlow: StateFlow<Float> = _fadeVolumeMultiplierFlow.asStateFlow()

    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    private fun setFadeMultiplier(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        fadeVolumeMultiplier = clamped
        _fadeVolumeMultiplierFlow.value = clamped
    }

    fun updateFadeSettings(enabled: Boolean, durationSec: Int) {
        fadeOutEnabled = enabled
        fadeOutDurationSec = durationSec.coerceIn(5, 180)
        if (!enabled && isFadingOut) {
            isFadingOut = false
            setFadeMultiplier(1f)
        }
    }

    fun start(
        minute: Int,
        fadeOut: Boolean = fadeOutEnabled,
        fadeDurationSec: Int = fadeOutDurationSec,
    ) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        restoreVolumeJob?.cancel()
        restoreVolumeJob = null

        fadeOutEnabled = fadeOut
        fadeOutDurationSec = fadeDurationSec.coerceIn(5, 180)
        isFadingOut = false
        setFadeMultiplier(1f)

        if (minute == -1) {
            pauseWhenSongEnd = true
            triggerTime = -1L
            totalDurationMs = 0L
            sleepTimerJob = scope.launch {
                while (isActive && pauseWhenSongEnd) {
                    val duration = player.duration
                    val position = player.currentPosition
                    if (fadeOutEnabled && duration != C.TIME_UNSET && duration > 0L && player.isPlaying) {
                        val remainingMs = (duration - position).coerceAtLeast(0L)
                        val fadeWindowMs = minOf(
                            fadeOutDurationSec * 1000L,
                            (duration / 2L).coerceAtLeast(3000L)
                        )
                        if (remainingMs in 1L..fadeWindowMs) {
                            isFadingOut = true
                            val progress = (1f - (remainingMs.toFloat() / fadeWindowMs.toFloat())).coerceIn(0f, 1f)
                            // Smooth quadratic perceptual audio fade-out curve
                            val curveMultiplier = ((1f - progress) * (1f - progress)).coerceIn(0.01f, 1f)
                            setFadeMultiplier(curveMultiplier)
                        } else if (remainingMs > fadeWindowMs && isFadingOut) {
                            isFadingOut = false
                            setFadeMultiplier(1f)
                        }
                    } else if (!fadeOutEnabled && isFadingOut) {
                        isFadingOut = false
                        setFadeMultiplier(1f)
                    }
                    delay(80L)
                }
            }
        } else {
            val safeMinutes = minute.coerceAtLeast(1)
            val durationMs = safeMinutes.minutes.inWholeMilliseconds
            pauseWhenSongEnd = false
            totalDurationMs = durationMs
            triggerTime = System.currentTimeMillis() + durationMs

            sleepTimerJob = scope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val remainingMs = triggerTime - now
                    if (remainingMs <= 0L) {
                        break
                    }

                    val activeTotalMs = totalDurationMs.coerceAtLeast(1000L)
                    val fadeWindowMs = if (fadeOutEnabled) {
                        minOf(fadeOutDurationSec * 1000L, activeTotalMs)
                    } else {
                        0L
                    }

                    if (fadeOutEnabled && fadeWindowMs > 0L && remainingMs <= fadeWindowMs) {
                        isFadingOut = true
                        val progress = (1f - (remainingMs.toFloat() / fadeWindowMs.toFloat())).coerceIn(0f, 1f)
                        // Perceptual quadratic curve for a natural, gentle volume transition
                        val curveMultiplier = ((1f - progress) * (1f - progress)).coerceIn(0f, 1f)
                        setFadeMultiplier(curveMultiplier)
                        delay(50L)
                    } else {
                        if (isFadingOut) {
                            isFadingOut = false
                            setFadeMultiplier(1f)
                        }
                        val waitMs = if (fadeOutEnabled && fadeWindowMs > 0L) {
                            (remainingMs - fadeWindowMs).coerceIn(50L, 250L)
                        } else {
                            remainingMs.coerceIn(50L, 250L)
                        }
                        delay(waitMs)
                    }
                }

                if (fadeOutEnabled) {
                    setFadeMultiplier(0f)
                    delay(80L)
                }
                completeTimerAndPause()
            }
        }
    }

    fun addTime(extraMinutes: Int) {
        if (extraMinutes <= 0) return
        val extraMs = extraMinutes.minutes.inWholeMilliseconds
        if (pauseWhenSongEnd || triggerTime == -1L) {
            start(extraMinutes, fadeOutEnabled, fadeOutDurationSec)
        } else {
            val now = System.currentTimeMillis()
            val currentRemaining = (triggerTime - now).coerceAtLeast(0L)
            val newRemaining = currentRemaining + extraMs
            triggerTime = now + newRemaining
            totalDurationMs = maxOf(totalDurationMs + extraMs, newRemaining)
            if (isFadingOut) {
                isFadingOut = false
                smoothRestoreVolume()
            }
        }
    }

    private fun smoothRestoreVolume() {
        restoreVolumeJob?.cancel()
        val startMultiplier = fadeVolumeMultiplier
        if (startMultiplier >= 0.99f) {
            setFadeMultiplier(1f)
            return
        }
        restoreVolumeJob = scope.launch {
            val steps = 10
            for (i in 1..steps) {
                val fraction = i / steps.toFloat()
                setFadeMultiplier(startMultiplier + (1f - startMultiplier) * fraction)
                delay(25L)
            }
            setFadeMultiplier(1f)
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        restoreVolumeJob?.cancel()
        restoreVolumeJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
        totalDurationMs = 0L
        isFadingOut = false
        setFadeMultiplier(1f)
    }

    fun notifySongTransition() {
        if (pauseWhenSongEnd) {
            completeTimerAndPause()
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pauseWhenSongEnd) {
            completeTimerAndPause()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            completeTimerAndPause()
        }
    }

    private fun completeTimerAndPause() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
        totalDurationMs = 0L
        isFadingOut = false
        player.pause()
        // Restore volume multiplier shortly after pausing so the next play session has normal volume
        restoreVolumeJob?.cancel()
        restoreVolumeJob = scope.launch {
            delay(150L)
            setFadeMultiplier(1f)
        }
    }
}
