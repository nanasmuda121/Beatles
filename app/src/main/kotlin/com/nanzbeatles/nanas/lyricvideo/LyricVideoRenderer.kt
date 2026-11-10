/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyricvideo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.nanzbeatles.nanas.constants.VideoLyricsCardStyle
import com.nanzbeatles.nanas.constants.VideoLyricsEffectStyle
import com.nanzbeatles.nanas.lyrics.LyricsEntry
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-impact Canvas renderer for the NanzBeatles Lyric Video Generator.
 * Supports:
 * - KINETIC_SHAKE (Alight Motion / CapCut style):
 *   - 3D floating camera sway, tilt, and beat/word impact camera shake + zoom punch
 *   - Automatic clause chunking (2-5 words per screen) with cumulative word-by-word pop-in
 *   - Multi-pass directional vertical motion blur trails on word & phrase transitions
 *   - White neon bloom / outer glow on lyrics and CD jewel case
 *   - Iconic CD center label ring, top-right crown accent, and edge light pulse vignette
 * - SMOOTH_FLOAT:
 *   - Smooth 3D floating camera motion & beat pulse with glowing lyrics
 * - STATIC_CLASSIC:
 *   - Classic vertical lyric scroll
 */
class LyricVideoRenderer(
    val width: Int = 1280,
    val height: Int = 720,
    private val caseBitmap: Bitmap? = null,
    private val brandText: String = "@BeatlesMusic",
    private val artistHandle: String = "@Beatles",
    var cardStyle: VideoLyricsCardStyle = VideoLyricsCardStyle.KASET,
    var effectStyle: VideoLyricsEffectStyle = VideoLyricsEffectStyle.KINETIC_SHAKE,
    var clipStartTimeMs: Long = -1L,
    lyricsScale: Float = 1.0f,
    lyricsOffsetX: Float = 0f,
    lyricsOffsetY: Float = 0f,
    var cardScale: Float = 1.0f,
    var cardOffsetX: Float = 0f,
    var cardOffsetY: Float = 0f,
    var cardAlpha: Float = 1.0f,
    var lyricsSpacingScale: Float = 1.0f,
    var showUpcomingLyrics: Boolean = true,
    var hideInactiveLyrics: Boolean = false,
    var lyricsRotation: Float = 0f,
    var cardRotation: Float = 0f,
    var jedagJedugEnabled: Boolean = true,
    var snowEnabled: Boolean = false,
    var waveAmplitudeScale: Float = 1.0f,
    var waveSpeedScale: Float = 1.0f
) {

    var lyricsScale: Float = lyricsScale
        set(value) {
            if (field != value) {
                field = value
                entryLinesCache.clear()
                kineticChunksCacheKey = null
            }
        }

    var lyricsOffsetX: Float = lyricsOffsetX
        set(value) {
            if (field != value) {
                field = value
                entryLinesCache.clear()
                kineticChunksCacheKey = null
            }
        }

    var lyricsOffsetY: Float = lyricsOffsetY

    // Paints
    private val bgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = height * 0.042f
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.04f
        setShadowLayer(10f, 0f, 2f, Color.parseColor("#B0000000"))
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = height * 0.048f
        typeface = Typeface.create("serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.04f
        setShadowLayer(12f, 0f, 0f, Color.argb(160, 255, 255, 255))
    }

    private val cassetteFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(95, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = height * 0.0055f
    }

    private val cassetteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(28, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val cassetteDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 203, 213, 225)
        style = Paint.Style.STROKE
        strokeWidth = height * 0.003f
    }

    private val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(145, 203, 213, 225)
        style = Paint.Style.FILL
    }

    private val waveformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = height * 0.007f
    }

    // Proportional lyrics typography
    private val activeTextSize = height * 0.070f
    private val kineticTextSize = height * 0.084f // ~60.5px on 720p (Bold, punchy Reference Video look)
    private val secTextSize = height * 0.052f
    private val prevTextSize = height * 0.048f

    private val lyricsActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = activeTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
        setShadowLayer(14f, 0f, 4f, Color.parseColor("#A0000000"))
    }

    private val lyricsKineticCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = kineticTextSize
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val lyricsKineticBloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = kineticTextSize
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val lyricsKineticGhostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = kineticTextSize
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val lyricsUpcomingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 255, 255, 255)
        textSize = activeTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val lyricsNextLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(115, 255, 255, 255)
        textSize = secTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    private val lyricsPrevPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(95, 255, 255, 255)
        textSize = prevTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    private val hubOuterRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 230, 230, 230)
        style = Paint.Style.STROKE
        strokeWidth = 3.2f
    }

    private val hubInnerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 190, 190, 190)
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
    }

    private val hubSpokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 210, 210, 210)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val centerHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val centerHoleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 180, 180, 180)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Exact geometry coordinates matched with reference video
    private val discCenterX = width * 0.695f
    private val discCenterY = height * 0.505f
    private val discRadius = height * 0.295f

    // Jewel case dimensions & position
    private val caseWidth = discRadius * 2.31f
    private val caseHeight = discRadius * 2.15f
    private val caseRect = RectF(
        discCenterX - (caseWidth * 0.53f),
        discCenterY - (caseHeight * 0.50f),
        discCenterX + (caseWidth * 0.47f),
        discCenterY + (caseHeight * 0.50f)
    )

    // Waveform / Bass Beat Visualizer geometry beneath the CD case
    private val waveformBaseY = caseRect.bottom + (height * 0.056f)
    private val clusterCenters = floatArrayOf(
        discCenterX - (discRadius * 0.68f),
        discCenterX,
        discCenterX + (discRadius * 0.68f)
    )

    // Text positions (safe left & width so right edge never touches or gets clipped by CD case)
    private val brandTextY = caseRect.top - (height * 0.035f)
    private val handleX = caseRect.right + (height * 0.052f)
    private val lyricsX = width * 0.065f
    private val lyricsMaxWidth = (caseRect.left - 34f - lyricsX).coerceAtLeast(width * 0.36f)

    // Vertical focal center for active lyrics
    private val slotActiveY = height * 0.490f

    // Pre-allocated circular disc bitmap
    private var circularCoverBitmap: Bitmap? = null
    private var cachedSourceCover: Bitmap? = null

    // Omni-directional Snow Particles for Atmospheric Background
    private data class SnowParticle(
        val startX: Float,
        val startY: Float,
        val dirX: Float,
        val dirY: Float,
        val slowSpeed: Float,
        val burstSpeed: Float,
        val swirlFreq: Float,
        val swirlAmp: Float,
        val phase: Float,
        val baseRadius: Float,
        val baseAlpha: Float
    )

    private val snowParticles: List<SnowParticle> = List(88) { idx ->
        // Deterministic low-discrepancy distribution so particles move in all random 360° directions
        val hash1 = ((idx * 1664525L + 1013904223L) and 0xFFFFFFFFL).toFloat() / 0xFFFFFFFFL.toFloat()
        val hash2 = (((idx + 37) * 22695477L + 1L) and 0xFFFFFFFFL).toFloat() / 0xFFFFFFFFL.toFloat()
        val hash3 = (((idx + 73) * 1103515245L + 12345L) and 0xFFFFFFFFL).toFloat() / 0xFFFFFFFFL.toFloat()
        val hash4 = (((idx + 19) * 134775813L + 1L) and 0xFFFFFFFFL).toFloat() / 0xFFFFFFFFL.toFloat()

        // Full 360-degree random direction angle (never unidirectional!)
        val angleRad = (idx * 2.3999632f) + (hash3 * 1.25f)
        val dx = cos(angleRad)
        val dy = sin(angleRad)

        val scaleFactor = height / 720f
        SnowParticle(
            startX = hash1 * width,
            startY = hash2 * height,
            dirX = dx,
            dirY = dy,
            slowSpeed = (14f + hash3 * 22f) * scaleFactor,
            burstSpeed = (185f + hash4 * 210f) * scaleFactor,
            swirlFreq = 0.75f + hash4 * 1.45f,
            swirlAmp = (5f + hash3 * 14f) * scaleFactor,
            phase = hash2 * 6.283185f,
            baseRadius = (1.5f + hash4 * 2.9f) * scaleFactor,
            baseAlpha = 0.26f + hash3 * 0.48f
        )
    }

    private val snowCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val snowGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val snowStreakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val bassBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val bassBarGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Stateful bass transient & snow burst trackers
    private var lastFrameTimeMs: Long = -1L
    private var prevFrameAmplitude: Float = 0.2f
    private var runningBassAvg: Float = 0.25f
    private var smoothedBassHitImpulse: Float = 0f
    private var snowBurstTravelSec: Float = 0f

    /**
     * Prepares and caches circular cropped cover art.
     */
    fun setCoverBitmap(cover: Bitmap?) {
        if (cover == null) {
            circularCoverBitmap = null
            cachedSourceCover = null
            entryLinesCache.clear()
            kineticChunksCacheKey = null
            return
        }
        if (cover == cachedSourceCover && circularCoverBitmap != null) {
            return
        }

        entryLinesCache.clear()
        kineticChunksCacheKey = null

        cachedSourceCover = cover
        val size = (discRadius * 2).toInt().coerceAtLeast(64)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        // Circle mask
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        // Scale and crop center
        val minDim = minOf(cover.width, cover.height)
        val cropX = (cover.width - minDim) / 2
        val cropY = (cover.height - minDim) / 2
        val centerSrcRect = Rect(cropX, cropY, cropX + minDim, cropY + minDim)

        canvas.drawBitmap(cover, centerSrcRect, rect, paint)

        circularCoverBitmap = output
    }

    private data class CameraRigState(
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val rotationDeg: Float = 0f,
        val scale: Float = 1f,
        val chunkImpulse: Float = 0f,
        val wordImpulse: Float = 0f,
        val beatImpulse: Float = 0f,
        val bassHitImpulse: Float = 0f,
        val zoomShakeGlowImpulse: Float = 0f,
        val introImpulse: Float = 0f
    )

    private var detectedFirstFrameMs: Long = -1L

    private fun updateBassAndSnowState(currentTimeMs: Long, amplitude: Float): Pair<Float, Float> {
        val rawDeltaMs = if (lastFrameTimeMs < 0L) 33L else (currentTimeMs - lastFrameTimeMs)
        val isSeekJump = rawDeltaMs < 0L || rawDeltaMs > 650L
        val dtSec = if (isSeekJump) 0.033f else (rawDeltaMs / 1000f).coerceIn(0.004f, 0.10f)
        lastFrameTimeMs = currentTimeMs

        // Two-zone contract from AudioAmplitudeAnalyzer & SilenceDetectorAudioProcessor:
        // Zone 1 [0.10..0.40]: Smooth musical envelope when NO bass kick is present (smooth zoom only)
        // Zone 2 [0.42..1.00]: Verified sub-bass / kick drum transient peak (triggers bass shake + zoom punch)
        val continuousBass = ((amplitude - 0.12f) / 0.28f).coerceIn(0f, 1f)
        val verifiedKick = if (amplitude > 0.42f) {
            ((amplitude - 0.42f) / 0.58f).coerceIn(0f, 1f)
        } else {
            0f
        }

        if (isSeekJump) {
            prevFrameAmplitude = verifiedKick
            runningBassAvg = continuousBass
            smoothedBassHitImpulse = verifiedKick
            snowBurstTravelSec = (currentTimeMs / 1000f) * 0.08f
            if (clipStartTimeMs < 0L) {
                detectedFirstFrameMs = currentTimeMs
            }
        }
        if (detectedFirstFrameMs < 0L) {
            detectedFirstFrameMs = currentTimeMs
        }

        val kickRise = (verifiedKick - prevFrameAmplitude).coerceAtLeast(0f)
        prevFrameAmplitude = verifiedKick
        runningBassAvg = (runningBassAvg * 0.85f) + (continuousBass * 0.15f)

        // Only trigger bass hit impulse when a verified kick is present (> 0.44f amplitude)
        val rawBassTrigger = if (verifiedKick > 0.04f) {
            (verifiedKick * 0.80f + kickRise * 0.35f).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Fast attack on verified bass kick, smooth ~72ms exponential damping so the shake settles quickly and stays stable
        val decayed = smoothedBassHitImpulse * exp(-dtSec / 0.072f)
        smoothedBassHitImpulse = maxOf(rawBassTrigger, if (decayed > 0.04f) decayed else 0f).coerceIn(0f, 1f)

        // Advance cumulative snow burst travel strictly when bass hits
        val burstIntensity = (smoothedBassHitImpulse * smoothedBassHitImpulse * 1.15f)
        snowBurstTravelSec += dtSec * burstIntensity

        return Pair(continuousBass, smoothedBassHitImpulse)
    }

    private fun computeCameraRig(
        currentTimeMs: Long,
        amplitude: Float,
        lyrics: List<LyricsEntry>?,
        continuousBass: Float,
        bassHitImpulse: Float
    ): CameraRigState {
        val effectiveStartMs = if (clipStartTimeMs >= 0L) clipStartTimeMs else detectedFirstFrameMs.coerceAtLeast(0L)
        val clipElapsedMs = (currentTimeMs - effectiveStartMs).coerceAtLeast(0L)

        // Reference-video style Cinematic Intro over the first 900ms of the video
        val introDurationMs = 900f
        val introRaw = (1f - (clipElapsedMs / introDurationMs)).coerceIn(0f, 1f)
        val introImpulse = introRaw * introRaw * (3f - 2f * introRaw) // Smooth S-curve 1.0 -> 0.0

        if (effectStyle == VideoLyricsEffectStyle.STATIC_CLASSIC) {
            return CameraRigState(
                beatImpulse = continuousBass,
                bassHitImpulse = bassHitImpulse,
                zoomShakeGlowImpulse = 0f,
                introImpulse = introImpulse
            )
        }

        val tSec = currentTimeMs / 1000f
        val validLyrics = lyrics?.filter { it.text.isNotBlank() } ?: emptyList()
        val chunks = getKineticChunks(validLyrics)
        val activeChunkIdx = chunks.indexOfLast { it.startMs <= currentTimeMs }

        var chunkAgeMs = 9999L
        var wordAgeMs = 9999L
        if (activeChunkIdx >= 0) {
            val chunk = chunks[activeChunkIdx]
            chunkAgeMs = (currentTimeMs - chunk.startMs).coerceAtLeast(0L)
            val activeWord = chunk.words.lastOrNull { it.startMs <= currentTimeMs } ?: chunk.words.firstOrNull()
            if (activeWord != null) {
                wordAgeMs = (currentTimeMs - activeWord.startMs).coerceAtLeast(0L)
            }
        }

        val chunkImpulse = exp(-chunkAgeMs / 140f).coerceIn(0f, 1f)
        val wordImpulse = exp(-wordAgeMs / 90f).coerceIn(0f, 1f)

        // Gate bass vibration by jedagJedugEnabled toggle ("tombol tuas jedag-jedug")
        val bassGate = if (jedagJedugEnabled && bassHitImpulse > 0.08f) bassHitImpulse else 0f
        // Smooth wave damping curve so bass hits produce a punchy bass vibration on top of the mandatory wave sway
        val dampedBass = bassGate * bassGate

        // Short white text glow strictly when zooming/shaking on verified Bass hits (or intro whip)
        val rawGlowTrigger = maxOf(bassGate, introImpulse * 0.85f).coerceIn(0f, 1f)
        val zoomShakeGlowImpulse = (rawGlowTrigger * rawGlowTrigger).coerceIn(0f, 1f)

        // Bass vibration oscillation when jedag-jedug is enabled
        val bassVibPhase = currentTimeMs * 0.045f
        val bassShakeWaveX = sin(bassVibPhase) * cos(bassVibPhase * 0.50f)
        val bassShakeWaveY = cos(bassVibPhase * 0.82f)
        val bassShakeWaveRot = sin(bassVibPhase * 0.60f)

        // Cinematic Intro 3D Whip-Zoom & Tilt (matches 00:00 of reference video, kept smooth)
        val introOffsetX = -introImpulse * (width * 0.022f) * cos(clipElapsedMs * 0.010f)
        val introOffsetY = introImpulse * (height * 0.038f) * cos(clipElapsedMs * 0.012f)
        val introRot = -introImpulse * 2.6f * cos(clipElapsedMs * 0.009f)
        val introScaleBoost = introImpulse * 0.16f

        val ampScale = waveAmplitudeScale.coerceIn(0.15f, 2.60f)
        val spdScale = waveSpeedScale.coerceIn(0.25f, 2.60f)
        val wSec = tSec * spdScale

        return if (effectStyle == VideoLyricsEffectStyle.KINETIC_SHAKE) {
            // MANDATORY 3D Wave Sway ("Ayunan Gelombang"): customizable amplitude & speed via Pengaturan Gelombang
            val waveX = ((sin(wSec * 2.15f) * (width * 0.0185f)) + (cos(wSec * 1.25f) * (width * 0.0085f))) * ampScale
            val waveY = ((cos(wSec * 1.90f) * (height * 0.0250f)) + (sin(wSec * 2.60f) * (height * 0.0100f))) * ampScale
            val waveRot = ((sin(wSec * 1.70f) * 2.10f) + (cos(wSec * 2.35f) * 0.75f)) * ampScale

            // Visible 3D wave zoom breathing + optional bass boost
            val bassWaveBoost = if (jedagJedugEnabled) continuousBass * 0.018f else 0f
            val zoomDelta = ((0.032f * sin(wSec * 2.05f)) + (0.014f * cos(wSec * 1.20f))) * ampScale
            val smoothZoomWave = 1.0f + (0.032f * ampScale.coerceAtMost(1.8f)) + zoomDelta + bassWaveBoost

            // Bass vibration ("Jedag-Jedug") when jedagJedugEnabled is ON
            val bassShakeX = dampedBass * bassShakeWaveX * (width * 0.0110f)
            val bassShakeY = dampedBass * bassShakeWaveY * (height * 0.0145f)
            val bassShakeRot = dampedBass * bassShakeWaveRot * 1.15f
            val bassZoomPunch = (dampedBass * 0.048f) + introScaleBoost

            CameraRigState(
                offsetX = waveX + bassShakeX + introOffsetX,
                offsetY = waveY + bassShakeY + introOffsetY,
                rotationDeg = waveRot + bassShakeRot + introRot,
                scale = (smoothZoomWave + bassZoomPunch).coerceIn(0.92f, 1.35f),
                chunkImpulse = chunkImpulse,
                wordImpulse = wordImpulse,
                beatImpulse = continuousBass,
                bassHitImpulse = bassGate,
                zoomShakeGlowImpulse = zoomShakeGlowImpulse,
                introImpulse = introImpulse
            )
        } else {
            // SMOOTH_FLOAT ("3D Smooth Float"): Mandatory, customizable 3D wave sway + optional bass pulse
            val waveX = ((sin(wSec * 1.95f) * (width * 0.0165f)) + (cos(wSec * 1.15f) * (width * 0.0075f))) * ampScale
            val waveY = ((cos(wSec * 1.75f) * (height * 0.0220f)) + (sin(wSec * 2.35f) * (height * 0.0090f))) * ampScale
            val waveRot = ((sin(wSec * 1.55f) * 1.85f) + (cos(wSec * 2.15f) * 0.65f)) * ampScale

            val bassWaveBoost = if (jedagJedugEnabled) continuousBass * 0.015f else 0f
            val zoomDelta = ((0.028f * sin(wSec * 1.85f)) + (0.012f * cos(wSec * 1.10f))) * ampScale
            val smoothZoomWave = 1.0f + (0.028f * ampScale.coerceAtMost(1.8f)) + zoomDelta + bassWaveBoost

            val bassShakeX = dampedBass * bassShakeWaveX * (width * 0.0085f)
            val bassShakeY = dampedBass * bassShakeWaveY * (height * 0.0110f)
            val bassShakeRot = dampedBass * bassShakeWaveRot * 0.85f
            val bassZoomPunch = (dampedBass * 0.038f) + (introScaleBoost * 0.75f)

            CameraRigState(
                offsetX = waveX + bassShakeX + (introOffsetX * 0.70f),
                offsetY = waveY + bassShakeY + (introOffsetY * 0.70f),
                rotationDeg = waveRot + bassShakeRot + (introRot * 0.70f),
                scale = (smoothZoomWave + bassZoomPunch).coerceIn(0.92f, 1.32f),
                chunkImpulse = chunkImpulse * 0.80f,
                wordImpulse = wordImpulse * 0.70f,
                beatImpulse = continuousBass,
                bassHitImpulse = bassGate,
                zoomShakeGlowImpulse = zoomShakeGlowImpulse,
                introImpulse = introImpulse
            )
        }
    }

    /**
     * Renders a complete frame on the given Canvas.
     */
    fun renderFrame(
        canvas: Canvas,
        currentTimeMs: Long,
        amplitude: Float,
        lyrics: List<LyricsEntry>?,
        songTitle: String = "Beatles",
        songArtist: String = ""
    ) {
        // Clip the entire canvas strictly to 1280x720 video boundaries
        val frameSaveCount = canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())

        // Update real-time bass transient & snow burst state
        val (continuousBass, bassHitImpulse) = updateBassAndSnowState(currentTimeMs, amplitude)

        // Compute 3D Camera Rig state
        val cam = computeCameraRig(currentTimeMs, amplitude, lyrics, continuousBass, bassHitImpulse)

        // 1. Atmospheric Dark Background (with optional Snow Particles when snowEnabled == true)
        renderSnowParticlesBackground(canvas, currentTimeMs, cam)

        // Apply whole-scene 3D camera transform
        val sceneSave = canvas.save()
        if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
            val pivotX = width * 0.52f
            val pivotY = height * 0.50f
            canvas.translate(cam.offsetX, cam.offsetY)
            canvas.rotate(cam.rotationDeg, pivotX, pivotY)
            canvas.scale(cam.scale, cam.scale, pivotX, pivotY)
        }

        // 2. Render Card / Cover Art / CD Jewel Case if visible
        if (cardAlpha > 0.005f) {
            val margin = 16f
            val baseTop: Float
            val baseBottom: Float
            val baseLeft: Float
            val baseRight: Float

            if (cardStyle == VideoLyricsCardStyle.NORMAL) {
                val cardSize = discRadius * 1.82f
                baseTop = discCenterY - (cardSize * 0.62f)
                baseBottom = baseTop + cardSize
                baseLeft = discCenterX - (cardSize / 2f)
                baseRight = discCenterX + (cardSize / 2f)
            } else {
                baseTop = brandTextY - 12f
                baseBottom = waveformBaseY + 16f
                baseLeft = caseRect.left
                baseRight = caseRect.right
            }

            // Calculate scaled bounds around (discCenterX, discCenterY)
            val scaledTop = discCenterY + (baseTop - discCenterY) * cardScale
            val scaledBottom = discCenterY + (baseBottom - discCenterY) * cardScale
            val scaledLeft = discCenterX + (baseLeft - discCenterX) * cardScale
            val scaledRight = discCenterX + (baseRight - discCenterX) * cardScale

            val minOffsetY = margin - scaledTop
            val maxOffsetY = (height - margin) - scaledBottom
            val clampedOffsetY = if (minOffsetY <= maxOffsetY) {
                cardOffsetY.coerceIn(minOffsetY, maxOffsetY)
            } else {
                cardOffsetY.coerceAtLeast(minOffsetY)
            }

            val minOffsetX = margin - scaledLeft
            val maxOffsetX = (width - margin) - scaledRight
            val clampedOffsetX = if (minOffsetX <= maxOffsetX) {
                cardOffsetX.coerceIn(minOffsetX, maxOffsetX)
            } else {
                cardOffsetX.coerceIn(maxOffsetX, minOffsetX)
            }

            val cardLayerAlpha = (cardAlpha.coerceIn(0f, 1f) * 255).toInt()
            val saveCount = if (cardLayerAlpha < 255) {
                canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), cardLayerAlpha)
            } else {
                canvas.save()
            }

            // 3D depth parallax wave on the CD case so it floats with natural depth separation
            val parallaxX = if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                cam.offsetX * 0.22f
            } else 0f
            val parallaxY = if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                -cam.offsetY * 0.24f + (cam.introImpulse * height * 0.022f)
            } else 0f
            val dynamicCardRot = cardRotation + if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                -cam.rotationDeg * 0.25f
            } else 0f
            val dynamicCardScale = cardScale * if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                1.0f + (cam.bassHitImpulse * cam.bassHitImpulse * 0.026f) + (cam.beatImpulse * 0.010f)
            } else 1.0f

            canvas.translate(clampedOffsetX + parallaxX, clampedOffsetY + parallaxY)
            if (dynamicCardRot != 0f) {
                canvas.rotate(dynamicCardRot, discCenterX, discCenterY)
            }
            if (dynamicCardScale != 1.0f) {
                canvas.scale(dynamicCardScale, dynamicCardScale, discCenterX, discCenterY)
            }

            if (cardStyle == VideoLyricsCardStyle.NORMAL) {
                renderNormalCard(canvas, songTitle, songArtist, currentTimeMs, amplitude, cam)
            } else {
                renderCaseAmbientGlow(canvas, cam)
                renderSpinningDisc(canvas, currentTimeMs, cam)
                renderSpindleHub(canvas)
                renderJewelCase(canvas)
                renderCrownAccent(canvas, cam)
                renderBrandText(canvas, cam)
                renderArtistHandle(canvas)
                renderBassBeatVisualizer(canvas, currentTimeMs, amplitude, cam)
            }

            canvas.restoreToCount(saveCount)
        }

        // 3. Render Lyrics (Kinetic Pop + Motion Blur in KINETIC_SHAKE, or Smooth/Classic)
        if (effectStyle == VideoLyricsEffectStyle.KINETIC_SHAKE) {
            renderKineticLyrics(canvas, currentTimeMs, lyrics, songTitle, songArtist, cam)
        } else {
            renderLyrics(canvas, currentTimeMs, lyrics, songTitle, songArtist, cam)
        }

        canvas.restoreToCount(sceneSave)

        // 4. Cinematic Edge Light Vignette & Reference-Video Intro Lens Opening Overlay
        if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
            renderCinematicEdgeGlow(canvas, cam)
        }
        if (cam.introImpulse > 0.01f) {
            renderCinematicIntroOverlay(canvas, cam)
        }

        canvas.restoreToCount(frameSaveCount)
    }

    /**
     * Renders the Reference-Video style Cinematic Intro (00:00 - 00:01):
     * Smoky white/silver oval lens vignette ring + tunnel iris opening + subtle exposure flash.
     */
    private fun renderCinematicIntroOverlay(canvas: Canvas, cam: CameraRigState) {
        val intro = cam.introImpulse.coerceIn(0f, 1f)
        val cx = width * 0.50f
        val cy = height * 0.50f

        // Oval aspect-corrected tunnel vignette matching frame 00:00 of the reference video
        canvas.save()
        canvas.scale(1.0f, height.toFloat() / width.toFloat(), cx, cy)
        val radius = width * (0.48f + 0.30f * (1f - intro))
        val smokyWhiteAlpha = (intro * 145f).toInt().coerceIn(0, 180)
        val outerBlackAlpha = (intro * 245f).toInt().coerceIn(0, 255)

        val introLensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((smokyWhiteAlpha * 0.35f).toInt(), 225, 232, 245),
                    Color.argb(smokyWhiteAlpha, 240, 245, 255),
                    Color.argb(outerBlackAlpha, 0, 0, 0)
                ),
                floatArrayOf(0f, 0.54f, 0.76f, 1.0f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        // Draw over scaled circle covering the full frame
        val coverSpan = width * 1.2f
        canvas.drawRect(cx - coverSpan, cy - coverSpan, cx + coverSpan, cy + coverSpan, introLensPaint)
        canvas.restore()

        // Initial fade-from-black veil in the very first ~250ms
        if (intro > 0.65f) {
            val veilFactor = ((intro - 0.65f) / 0.35f).coerceIn(0f, 1f)
            val veilPaint = Paint().apply {
                color = Color.argb((veilFactor * veilFactor * 210f).toInt().coerceIn(0, 220), 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), veilPaint)
        }
    }

    /**
     * Renders a rich dark atmospheric background.
     * When [snowEnabled] is true, omni-directional floating snow particles drift across the screen
     * and briefly glow/accelerate when bass hits. When [snowEnabled] is false ("Tanpa Salju"),
     * renders a clean solid black background matching the reference video.
     */
    private fun renderSnowParticlesBackground(
        canvas: Canvas,
        currentTimeMs: Long,
        cam: CameraRigState
    ) {
        if (!snowEnabled) {
            bgPaint.shader = null
            bgPaint.color = Color.BLACK
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            return
        }

        val bassFlash = (cam.bassHitImpulse * cam.bassHitImpulse).coerceIn(0f, 1f)

        // Deep midnight charcoal radial background when snow is enabled
        val bgCenterR = (10 + (bassFlash * 18)).toInt().coerceIn(0, 255)
        val bgCenterG = (14 + (bassFlash * 22)).toInt().coerceIn(0, 255)
        val bgCenterB = (22 + (bassFlash * 30)).toInt().coerceIn(0, 255)
        bgPaint.shader = RadialGradient(
            width * 0.50f,
            height * 0.50f,
            width * 0.72f,
            intArrayOf(
                Color.rgb(bgCenterR, bgCenterG, bgCenterB),
                Color.rgb(6, 8, 13),
                Color.rgb(2, 3, 5)
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val tSec = currentTimeMs / 1000f
        val margin = 24f
        val spanW = width + margin * 2f
        val spanH = height + margin * 2f

        for (p in snowParticles) {
            // Base slow drift + accumulated bass burst travel along each particle's unique random (dirX, dirY)
            val totalDistSlow = tSec * p.slowSpeed
            val totalDistBurst = snowBurstTravelSec * p.burstSpeed
            val totalDist = totalDistSlow + totalDistBurst

            // Perpendicular organic swirl so movement is natural and non-linear
            val perpX = -p.dirY
            val perpY = p.dirX
            val swirl = sin(tSec * p.swirlFreq + p.phase) * p.swirlAmp

            val rawX = p.startX + (p.dirX * totalDist) + (perpX * swirl)
            val rawY = p.startY + (p.dirY * totalDist) + (perpY * swirl)

            // Seamless wrap-around within [-margin, width + margin] and [-margin, height + margin]
            val px = (((rawX + margin) % spanW) + spanW) % spanW - margin
            val py = (((rawY + margin) % spanH) + spanH) % spanH - margin

            val twinkle = 0.80f + 0.20f * sin(tSec * 2.8f + p.phase)
            val coreAlphaFloat = (p.baseAlpha * twinkle + bassFlash * 0.58f).coerceIn(0.15f, 1.0f)
            val coreAlpha = (coreAlphaFloat * 255f).toInt().coerceIn(0, 255)
            val radius = p.baseRadius * (1.0f + 0.48f * bassFlash)

            // When bass hits, draw a brief glowing halo + short random-direction speed streak
            if (bassFlash > 0.06f) {
                val glowAlpha = (bassFlash * p.baseAlpha * 165f).toInt().coerceIn(0, 210)
                if (glowAlpha > 6) {
                    val haloRadius = radius * (2.6f + 2.2f * bassFlash)
                    snowGlowPaint.alpha = glowAlpha
                    canvas.drawCircle(px, py, haloRadius, snowGlowPaint)
                }

                // Short motion trail opposite to particle's random velocity vector during fast bass burst
                val streakLen = (p.burstSpeed * 0.032f * bassFlash).coerceAtMost(height * 0.032f)
                if (streakLen > 2.0f) {
                    snowStreakPaint.strokeWidth = (radius * 1.15f).coerceAtLeast(1.5f)
                    snowStreakPaint.alpha = (coreAlpha * 0.48f * bassFlash).toInt().coerceIn(0, 200)
                    canvas.drawLine(
                        px,
                        py,
                        px - p.dirX * streakLen,
                        py - p.dirY * streakLen,
                        snowStreakPaint
                    )
                }
            } else {
                // Subtle soft snow halo even when drifting slowly
                val softHaloAlpha = (p.baseAlpha * 42f).toInt().coerceIn(0, 60)
                if (softHaloAlpha > 4) {
                    snowGlowPaint.alpha = softHaloAlpha
                    canvas.drawCircle(px, py, radius * 1.85f, snowGlowPaint)
                }
            }

            snowCorePaint.alpha = coreAlpha
            canvas.drawCircle(px, py, radius, snowCorePaint)
        }
    }

    private fun renderCaseAmbientGlow(canvas: Canvas, cam: CameraRigState) {
        if (effectStyle == VideoLyricsEffectStyle.STATIC_CLASSIC) return
        val glowIntensity = (0.22f + cam.chunkImpulse * 0.35f + cam.beatImpulse * 0.20f).coerceIn(0.15f, 0.70f)
        val glowRadius = discRadius * 1.48f
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                discCenterX,
                discCenterY,
                glowRadius,
                intArrayOf(
                    Color.argb((glowIntensity * 95).toInt(), 255, 255, 255),
                    Color.argb((glowIntensity * 40).toInt(), 220, 230, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.45f, 0.78f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(discCenterX, discCenterY, glowRadius, glowPaint)
    }

    /**
     * Renders the stylized tilted crown on the top-right corner of the CD jewel case
     * matching the reference video aesthetic.
     */
    private fun renderCrownAccent(canvas: Canvas, cam: CameraRigState) {
        if (effectStyle == VideoLyricsEffectStyle.STATIC_CLASSIC) return

        val crownCx = caseRect.right - (height * 0.008f)
        val crownCy = caseRect.top - (height * 0.008f)
        val crownW = height * 0.088f
        val crownH = height * 0.062f

        canvas.save()
        canvas.translate(crownCx, crownCy)
        canvas.rotate(28f + cam.chunkImpulse * 6f)

        val path = Path().apply {
            // Base of crown
            moveTo(-crownW * 0.42f, crownH * 0.35f)
            lineTo(crownW * 0.42f, crownH * 0.35f)
            // Right outer spike
            lineTo(crownW * 0.52f, -crownH * 0.25f)
            // Right valley
            lineTo(crownW * 0.18f, crownH * 0.08f)
            // Center tall spike
            lineTo(0f, -crownH * 0.52f)
            // Left valley
            lineTo(-crownW * 0.18f, crownH * 0.08f)
            // Left outer spike
            lineTo(-crownW * 0.52f, -crownH * 0.25f)
            close()
        }

        val crownFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                -crownW * 0.5f, -crownH * 0.5f,
                crownW * 0.5f, crownH * 0.4f,
                intArrayOf(
                    Color.argb(230, 255, 255, 255),
                    Color.argb(175, 190, 205, 225),
                    Color.argb(135, 120, 140, 165)
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
            setShadowLayer(14f, 0f, 0f, Color.argb(180, 255, 255, 255))
        }
        canvas.drawPath(path, crownFillPaint)

        val crownStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        canvas.drawPath(path, crownStrokePaint)

        // Crown tip pearls
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 0f, Color.WHITE)
        }
        val pearlR = height * 0.0055f
        canvas.drawCircle(-crownW * 0.52f, -crownH * 0.25f, pearlR, tipPaint)
        canvas.drawCircle(0f, -crownH * 0.52f, pearlR * 1.15f, tipPaint)
        canvas.drawCircle(crownW * 0.52f, -crownH * 0.25f, pearlR, tipPaint)

        canvas.restore()
    }

    /**
     * Renders the subtle atmospheric edge glow & vignette around the 16:9 frame
     * matching the reference video.
     */
    private fun renderCinematicEdgeGlow(canvas: Canvas, cam: CameraRigState) {
        val pulse = (0.18f + cam.chunkImpulse * 0.34f + cam.beatImpulse * 0.16f).coerceIn(0.12f, 0.65f)
        val cx = width / 2f
        val cy = height / 2f
        val outerR = width * 0.68f
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                outerR,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    Color.argb((pulse * 48).toInt(), 210, 220, 235),
                    Color.argb(165, 0, 0, 0)
                ),
                floatArrayOf(0f, 0.72f, 0.90f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), edgePaint)
    }

    private fun renderNormalCard(
        canvas: Canvas,
        songTitle: String,
        songArtist: String,
        currentTimeMs: Long,
        amplitude: Float,
        cam: CameraRigState
    ) {
        val cx = discCenterX
        val cy = discCenterY
        val cardSize = discRadius * 1.82f
        val cardCornerRadius = height * 0.035f

        val cardLeft = cx - (cardSize / 2f)
        val cardTop = cy - (cardSize * 0.62f)
        val cardRight = cardLeft + cardSize
        val cardBottom = cardTop + cardSize
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // 1. Soft Glow / Shadow behind cover
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40000000")
            val glowColor = if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                Color.argb((90 + cam.chunkImpulse * 90).toInt().coerceAtMost(220), 255, 255, 255)
            } else {
                Color.parseColor("#C0000000")
            }
            setShadowLayer(26f, 0f, 6f, glowColor)
        }
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, shadowPaint)

        // 2. Draw Rounded Album Art Cover
        val cover = cachedSourceCover
        if (cover != null && !cover.isRecycled) {
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val minDim = minOf(cover.width, cover.height)
            val cropX = (cover.width - minDim) / 2
            val cropY = (cover.height - minDim) / 2
            val srcRect = Rect(cropX, cropY, cropX + minDim, cropY + minDim)
            canvas.drawBitmap(cover, srcRect, cardRect, null)
            canvas.restore()
        } else {
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    cardLeft, cardTop, cardRight, cardBottom,
                    Color.parseColor("#1E293B"), Color.parseColor("#0F172A"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, placeholderPaint)
        }

        // 3. Subtle stroke border around cover
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(75, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

        // 3b. @BeatlesMusic Brand Tag above cover
        val tagText = when {
            brandText.isBlank() || brandText.equals("NanzBeatles", ignoreCase = true) -> "@BeatlesMusic"
            brandText.startsWith("@") -> brandText
            else -> "@$brandText"
        }
        canvas.drawText(tagText, cx, cardTop - (height * 0.026f), brandPaint)

        // 4. Song Title below cover
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = height * 0.046f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 2f, Color.argb(150, 255, 255, 255))
        }
        val maxTextWidth = cardSize * 1.15f
        var displayTitle = songTitle
        while (titlePaint.measureText("$displayTitle…") > maxTextWidth && displayTitle.isNotEmpty()) {
            displayTitle = displayTitle.dropLast(1).trimEnd()
        }
        if (displayTitle.length < songTitle.length) displayTitle = "$displayTitle…"

        val titleY = cardBottom + (height * 0.052f)
        canvas.drawText(displayTitle, cx, titleY, titlePaint)

        // 5. Artist Name below song title
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 203, 213, 225)
            textSize = height * 0.035f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        var displayArtist = if (songArtist.isNotBlank()) songArtist else "Beatles"
        while (artistPaint.measureText("$displayArtist…") > maxTextWidth && displayArtist.isNotEmpty()) {
            displayArtist = displayArtist.dropLast(1).trimEnd()
        }
        if (displayArtist.length < songArtist.length) displayArtist = "$displayArtist…"

        val artistY = titleY + (height * 0.040f)
        canvas.drawText(displayArtist, cx, artistY, artistPaint)

        // 6. Sleek Audio Waveform below artist
        val waveBaseY = artistY + (height * 0.050f)
        val barCount = 15
        val barSpacing = height * 0.016f
        val startX = cx - ((barCount - 1) * barSpacing / 2f)
        val minH = height * 0.012f
        val maxH = height * 0.050f

        for (b in 0 until barCount) {
            val bx = startX + (b * barSpacing)
            val centerDist = abs(b - (barCount / 2f)) / (barCount / 2f)
            val bell = exp(-centerDist * centerDist * 2.0).toFloat()
            val phase = (b * 0.5) + (currentTimeMs * 0.008)
            val waveAmp = (amplitude * 0.75f + (sin(phase) * 0.25).toFloat()).coerceIn(0.15f, 1.0f)
            val barH = minH + (waveAmp * (maxH - minH) * bell)

            val yTop = waveBaseY - (barH / 2f)
            val yBot = waveBaseY + (barH / 2f)
            canvas.drawLine(bx, yTop, bx, yBot, waveformPaint)
        }
    }

    private fun renderSpinningDisc(canvas: Canvas, currentTimeMs: Long, cam: CameraRigState) {
        // One full 360-degree rotation every 5.2 seconds + fast spin-in acceleration during the intro
        val baseAngle = (currentTimeMs % 5200L) / 5200f * 360f
        val introSpinBoost = -cam.introImpulse * cam.introImpulse * 195f
        val rotationAngle = baseAngle + introSpinBoost

        canvas.save()
        canvas.rotate(rotationAngle, discCenterX, discCenterY)

        // Draw album art circular bitmap
        val cover = circularCoverBitmap
        if (cover != null && !cover.isRecycled) {
            val left = discCenterX - discRadius
            val top = discCenterY - discRadius
            canvas.drawBitmap(cover, left, top, null)
        } else {
            val defaultRecordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    discCenterX - discRadius, discCenterY - discRadius,
                    discCenterX + discRadius, discCenterY + discRadius,
                    Color.parseColor("#1E293B"), Color.parseColor("#0F172A"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(discCenterX, discCenterY, discRadius, defaultRecordPaint)
        }

        // Optical CD sheen reflections (diagonal light wedges that spin with the disc)
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                discCenterX - discRadius, discCenterY - discRadius,
                discCenterX + discRadius, discCenterY + discRadius,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(38, 255, 255, 255),
                    Color.TRANSPARENT,
                    Color.argb(28, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.32f, 0.50f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(discCenterX, discCenterY, discRadius, sheenPaint)

        // CD outer silver rim
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 225, 225, 225)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawCircle(discCenterX, discCenterY, discRadius, rimPaint)

        canvas.restore()
    }

    private fun renderSpindleHub(canvas: Canvas) {
        val darkOuterR = discRadius * 0.34f
        val warmLabelR = discRadius * 0.24f
        val hubR = discRadius * 0.25f
        val innerR = discRadius * 0.14f
        val holeR = discRadius * 0.065f

        // Dark obsidian CD inner ring (matching the reference video CD center)
        val obsidianRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 10, 10, 12)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(discCenterX, discCenterY, darkOuterR, obsidianRingPaint)

        // Warm amber/orange-gold metallic center ring (iconic 2Pac / vintage CD center hub look)
        val amberHubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                discCenterX - warmLabelR * 0.2f,
                discCenterY - warmLabelR * 0.2f,
                warmLabelR * 1.2f,
                intArrayOf(
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#EA580C"),
                    Color.parseColor("#9A3412")
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(discCenterX, discCenterY, warmLabelR, amberHubPaint)

        // Hub clamp outer and inner plastic rings
        canvas.drawCircle(discCenterX, discCenterY, darkOuterR, hubOuterRingPaint)
        canvas.drawCircle(discCenterX, discCenterY, hubR, hubOuterRingPaint)
        canvas.drawCircle(discCenterX, discCenterY, innerR, hubInnerRingPaint)

        // Radial clamp teeth spokes
        for (angle in 0 until 360 step 60) {
            val rad = Math.toRadians(angle.toDouble())
            val x1 = discCenterX + (innerR * 0.72f * cos(rad)).toFloat()
            val y1 = discCenterY + (innerR * 0.72f * sin(rad)).toFloat()
            val x2 = discCenterX + (hubR * 0.95f * cos(rad)).toFloat()
            val y2 = discCenterY + (hubR * 0.95f * sin(rad)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, hubSpokePaint)
        }

        // Center spindle hole
        canvas.drawCircle(discCenterX, discCenterY, holeR, centerHolePaint)
        canvas.drawCircle(discCenterX, discCenterY, holeR, centerHoleBorderPaint)
    }

    private fun renderJewelCase(canvas: Canvas) {
        if (caseBitmap != null && !caseBitmap.isRecycled) {
            val destRect = RectF(caseRect.left, caseRect.top, caseRect.right, caseRect.bottom)
            canvas.drawBitmap(caseBitmap, null, destRect, null)
            return
        }

        // Fallback procedural acrylic jewel case
        val cornerRadius = height * 0.025f
        canvas.drawRoundRect(caseRect, cornerRadius, cornerRadius, cassetteBgPaint)
        canvas.drawRoundRect(caseRect, cornerRadius, cornerRadius, cassetteFramePaint)

        // Left spine ridge of CD jewel case
        val spineRight = caseRect.left + (caseRect.width() * 0.085f)
        canvas.drawLine(spineRight, caseRect.top, spineRight, caseRect.bottom, cassetteFramePaint)

        val innerMargin = height * 0.018f
        val innerRect = RectF(
            caseRect.left + innerMargin,
            caseRect.top + innerMargin,
            caseRect.right - innerMargin,
            caseRect.bottom - innerMargin
        )
        canvas.drawRoundRect(innerRect, cornerRadius * 0.7f, cornerRadius * 0.7f, cassetteDetailPaint)

        val screwRadius = height * 0.009f
        val screwOffset = height * 0.024f
        val corners = listOf(
            caseRect.left + screwOffset to caseRect.top + screwOffset,
            caseRect.right - screwOffset to caseRect.top + screwOffset,
            caseRect.left + screwOffset to caseRect.bottom - screwOffset,
            caseRect.right - screwOffset to caseRect.bottom - screwOffset
        )
        for ((cx, cy) in corners) {
            canvas.drawCircle(cx, cy, screwRadius, screwPaint)
        }

        val highlightPath = Path().apply {
            moveTo(caseRect.left + (caseRect.width() * 0.25f), caseRect.top)
            lineTo(caseRect.left + (caseRect.width() * 0.55f), caseRect.top)
            lineTo(caseRect.left + (caseRect.width() * 0.15f), caseRect.bottom)
            lineTo(caseRect.left, caseRect.bottom)
            close()
        }
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(18, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawPath(highlightPath, glassPaint)
    }

    private fun renderBrandText(canvas: Canvas, cam: CameraRigState) {
        val tagText = when {
            brandText.isBlank() || brandText.equals("NanzBeatles", ignoreCase = true) -> "@BeatlesMusic"
            brandText.startsWith("@") -> brandText
            else -> "@$brandText"
        }
        val glow = cam.zoomShakeGlowImpulse
        if (glow > 0.06f) {
            brandPaint.setShadowLayer(
                12f + 16f * glow,
                0f,
                0f,
                Color.argb((220 * glow).toInt().coerceIn(0, 255), 255, 255, 255)
            )
        } else {
            brandPaint.setShadowLayer(10f, 0f, 2f, Color.parseColor("#B0000000"))
        }
        canvas.drawText(tagText, discCenterX, brandTextY, brandPaint)
    }

    private fun renderArtistHandle(canvas: Canvas) {
        val handle = if (artistHandle.isNotBlank()) {
            if (artistHandle.startsWith("@")) artistHandle else "@$artistHandle"
        } else {
            "@Beatles"
        }
        canvas.save()
        // Rotate 90 degrees clockwise right along the right edge of the jewel case
        canvas.translate(handleX, discCenterY)
        canvas.rotate(90f)
        canvas.drawText(handle, 0f, 0f, handlePaint)
        canvas.restore()
    }

    /**
     * Renders a sleek, bass-reactive Audio Spectrum / Equalizer Beat Visualizer directly beneath the CD jewel case.
     */
    private fun renderBassBeatVisualizer(
        canvas: Canvas,
        currentTimeMs: Long,
        baseAmp: Float,
        cam: CameraRigState
    ) {
        val barCount = 29
        val totalVisualizerWidth = caseWidth * 0.84f
        val barSpacing = totalVisualizerWidth / (barCount - 1).coerceAtLeast(1)
        val barWidth = (barSpacing * 0.54f).coerceAtLeast(3.5f)
        val startX = discCenterX - (totalVisualizerWidth / 2f)
        val centerY = waveformBaseY

        val minBarH = height * 0.012f
        val maxBarH = height * 0.092f
        val rawBassHit = smoothedBassHitImpulse.coerceIn(0f, 1f)
        val rawBeat = runningBassAvg.coerceIn(0f, 1f)
        val bassBoost = (rawBassHit * 0.85f + rawBeat * 0.45f).coerceIn(0f, 1.15f)
        val glowFlash = (rawBassHit * rawBassHit).coerceIn(0f, 1f)

        // Subtle glowing horizontal baseline under the CD case
        val lineHalfW = totalVisualizerWidth * (0.42f + 0.14f * bassBoost)
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                discCenterX - lineHalfW, centerY,
                discCenterX + lineHalfW, centerY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((65 + 115 * glowFlash).toInt().coerceIn(0, 210), 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        canvas.drawLine(discCenterX - lineHalfW, centerY, discCenterX + lineHalfW, centerY, baselinePaint)

        val midIndex = barCount / 2
        val tSec = currentTimeMs / 1000.0

        for (i in 0 until barCount) {
            val bx = startX + (i * barSpacing)
            val distFromCenter = abs(i - midIndex).toFloat() / midIndex.toFloat()

            // Multi-band envelope: Center = deep sub-bass kick, Mid = punchy synth/snare, Outer = hi-hat/air
            val subBassWeight = exp((-distFromCenter * distFromCenter * 4.2).toDouble()).toFloat()
            val midBandWeight = exp((-(distFromCenter - 0.48f) * (distFromCenter - 0.48f) * 9.0).toDouble()).toFloat()
            val outerBandWeight = (1f - distFromCenter).coerceIn(0.15f, 1f)

            val wave1 = abs(sin(tSec * 9.5 + i * 0.45)).toFloat()
            val wave2 = abs(cos(tSec * 14.2 - i * 0.72)).toFloat()

            val bandEnergy = (
                subBassWeight * (rawBassHit * 0.95f + baseAmp * 0.55f) +
                    midBandWeight * (baseAmp * 0.65f + wave1 * 0.35f * (0.25f + rawBeat * 0.75f)) +
                    outerBandWeight * (wave2 * 0.32f * (0.25f + rawBeat * 0.75f))
                ).coerceIn(0.08f, 1.0f)

            val barH = minBarH + (bandEnergy * (maxBarH - minBarH))
            val halfH = barH / 2f
            val rect = RectF(bx - barWidth / 2f, centerY - halfH, bx + barWidth / 2f, centerY + halfH)
            val cornerR = barWidth / 2f

            // Brief white neon bloom on bass hits
            if (glowFlash > 0.06f && subBassWeight + midBandWeight > 0.35f) {
                val glowAlpha = (glowFlash * (110 + 95 * subBassWeight)).toInt().coerceIn(0, 210)
                bassBarGlowPaint.alpha = glowAlpha
                bassBarGlowPaint.setShadowLayer(
                    12f + 12f * glowFlash,
                    0f,
                    0f,
                    Color.argb(glowAlpha, 255, 255, 255)
                )
                canvas.drawRoundRect(rect, cornerR, cornerR, bassBarGlowPaint)
            }

            val barAlpha = (165 + (90 * bandEnergy) + (45 * glowFlash)).toInt().coerceIn(130, 255)
            bassBarPaint.alpha = barAlpha
            canvas.drawRoundRect(rect, cornerR, cornerR, bassBarPaint)
        }
    }

    private data class TimedWord(
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val hasTrailingSpace: Boolean = true
    ) {
        fun progressAt(currentTimeMs: Long): Float {
            if (currentTimeMs >= endMs) return 1f
            if (currentTimeMs < startMs) return 0f
            val dur = (endMs - startMs).coerceAtLeast(1L)
            val raw = ((currentTimeMs - startMs).toFloat() / dur).coerceIn(0f, 1f)
            return raw * raw * (3f - 2f * raw)
        }
    }

    /**
     * A short, punchy 1–5 word phrase card used in KINETIC_SHAKE mode
     * so words pop in cumulatively on screen with vertical motion blur and glow.
     */
    private data class KineticChunk(
        val startMs: Long,
        val endMs: Long,
        val words: List<TimedWord>,
        val wrappedLines: List<List<TimedWord>>
    )

    private var kineticChunksCacheKey: Int? = null
    private var cachedKineticChunks: List<KineticChunk> = emptyList()

    private fun extractTimedWordsForEntry(entry: LyricsEntry, nextEntry: LyricsEntry?): List<TimedWord> {
        val timedWords = mutableListOf<TimedWord>()
        val wordTimings = entry.words?.filter { it.text.isNotBlank() }

        if (!wordTimings.isNullOrEmpty()) {
            for ((wIdx, w) in wordTimings.withIndex()) {
                val isLast = (wIdx == wordTimings.size - 1)
                val sMs = (w.startTime * 1000).toLong()
                val rawEndMs = (w.endTime * 1000).toLong()
                val safeEndMs = if (isLast) maxOf(rawEndMs, sMs + 420L) else maxOf(rawEndMs, sMs + 220L)
                timedWords.add(TimedWord(w.text.trim(), sMs, safeEndMs, w.hasTrailingSpace))
            }
        } else {
            val rawWords = entry.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (rawWords.isEmpty()) return emptyList()
            val lineStartMs = entry.time
            val estimatedDur = (rawWords.size * 380L).coerceIn(1400L, 4800L)
            val maxDur = nextEntry?.let { (it.time - lineStartMs - 60L).coerceAtLeast(500L) } ?: 3600L
            val lineDur = minOf(estimatedDur, maxDur)
            val count = rawWords.size
            for ((idx, w) in rawWords.withIndex()) {
                val sMs = lineStartMs + (idx * lineDur / count)
                val eMs = lineStartMs + ((idx + 1) * lineDur / count)
                timedWords.add(TimedWord(w, sMs, eMs, hasTrailingSpace = idx < count - 1))
            }
        }
        return timedWords
    }

    /**
     * Splits lyric entries into punchy 2-5 word KineticChunks (breaking naturally at commas/clauses
     * or every ~3-4 words) so that each screen displays big, bold, cumulative word-by-word text
     * just like the reference video.
     */
    private fun getKineticChunks(validLyrics: List<LyricsEntry>): List<KineticChunk> {
        val cacheKey = validLyrics.hashCode() * 31 + lyricsScale.hashCode()
        if (cacheKey == kineticChunksCacheKey && cachedKineticChunks.isNotEmpty()) {
            return cachedKineticChunks
        }

        val chunks = mutableListOf<KineticChunk>()
        lyricsKineticCorePaint.textSize = kineticTextSize * lyricsScale
        val chunkWrapWidth = lyricsMaxWidth * 0.92f

        for (i in validLyrics.indices) {
            val entry = validLyrics[i]
            val nextEntry = validLyrics.getOrNull(i + 1)
            val words = extractTimedWordsForEntry(entry, nextEntry)
            if (words.isEmpty()) continue

            // Break long lines into punchy clauses of 2..4 words
            val subLists = mutableListOf<List<TimedWord>>()
            var currentSub = mutableListOf<TimedWord>()

            for ((wIdx, tw) in words.withIndex()) {
                currentSub.add(tw)
                val endsWithClausePunct = tw.text.endsWith(",") ||
                    tw.text.endsWith(";") ||
                    tw.text.endsWith(".") ||
                    tw.text.endsWith("!") ||
                    tw.text.endsWith("?") ||
                    tw.text.endsWith("—")
                val remaining = words.size - (wIdx + 1)

                val shouldSplit = when {
                    remaining == 0 -> true
                    currentSub.size >= 4 && remaining >= 2 -> true
                    currentSub.size >= 2 && endsWithClausePunct && remaining >= 2 -> true
                    currentSub.size >= 3 && endsWithClausePunct && remaining >= 1 -> true
                    else -> false
                }

                if (shouldSplit) {
                    subLists.add(currentSub)
                    currentSub = mutableListOf()
                }
            }
            if (currentSub.isNotEmpty()) {
                subLists.add(currentSub)
            }

            for ((sIdx, sub) in subLists.withIndex()) {
                val cStart = sub.first().startMs
                val nextChunkStart = subLists.getOrNull(sIdx + 1)?.firstOrNull()?.startMs
                    ?: nextEntry?.time
                    ?: (sub.last().endMs + 900L)
                val cEnd = maxOf(cStart + 250L, minOf(nextChunkStart, sub.last().endMs + 1200L))
                val wrapped = wrapTimedWords(sub, chunkWrapWidth, lyricsKineticCorePaint, maxLines = 3)
                chunks.add(
                    KineticChunk(
                        startMs = cStart,
                        endMs = cEnd,
                        words = sub,
                        wrappedLines = wrapped
                    )
                )
            }
        }

        kineticChunksCacheKey = cacheKey
        cachedKineticChunks = chunks
        return chunks
    }

    /**
     * Renders the reference-video style Kinetic Lyrics:
     * - Cumulative word-by-word pop-in within each short phrase chunk
     * - Directional vertical motion-blur ghost trails on word appearance & chunk transition
     * - Crisp white typography with soft neon white outer bloom
     */
    private fun renderKineticLyrics(
        canvas: Canvas,
        currentTimeMs: Long,
        lyrics: List<LyricsEntry>?,
        songTitle: String,
        songArtist: String,
        cam: CameraRigState
    ) {
        val validLyrics = lyrics?.filter { it.text.isNotBlank() } ?: emptyList()
        val scaledTextSize = kineticTextSize * lyricsScale
        val curSlotActiveY = (slotActiveY + lyricsOffsetY).coerceIn(
            36f + scaledTextSize,
            height - 36f - scaledTextSize
        )

        val lyricsClipRight = caseRect.left - 18f
        val clipSave = canvas.save()
        canvas.clipRect(0f, 0f, lyricsClipRight, height.toFloat())

        if (lyricsRotation != 0f) {
            val lyricPivotX = lyricsX + lyricsOffsetX + (lyricsMaxWidth / 2f)
            canvas.rotate(lyricsRotation, lyricPivotX, curSlotActiveY)
        }

        try {
            if (validLyrics.isEmpty()) {
                renderKineticTitleCard(canvas, songTitle, songArtist, curSlotActiveY, 1.0f, cam.chunkImpulse)
                return
            }

            val chunks = getKineticChunks(validLyrics)
            if (chunks.isEmpty()) {
                renderKineticTitleCard(canvas, songTitle, songArtist, curSlotActiveY, 1.0f, cam.chunkImpulse)
                return
            }

            val activeChunkIndex = chunks.indexOfLast { it.startMs <= currentTimeMs }
            if (activeChunkIndex == -1) {
                val firstChunk = chunks.first()
                val timeUntilFirst = (firstChunk.startMs - currentTimeMs).coerceAtLeast(0L)
                val fadeOut = (timeUntilFirst / 300f).coerceIn(0f, 1f)
                renderKineticTitleCard(
                    canvas = canvas,
                    title = songTitle,
                    artist = songArtist,
                    centerY = curSlotActiveY,
                    alpha = fadeOut,
                    blurImpulse = 1f - fadeOut
                )
                return
            }

            val chunk = chunks[activeChunkIndex]
            val lines = chunk.wrappedLines
            if (lines.isEmpty()) return

            lyricsKineticCorePaint.textSize = scaledTextSize
            val spW = lyricsKineticCorePaint.measureText(" ")
            val safeKineticMaxWidth = (lyricsClipRight - (lyricsX + lyricsOffsetX) - 18f).coerceAtLeast(width * 0.30f)

            val maxLineWidth = lines.maxOfOrNull { line ->
                line.sumOf {
                    (lyricsKineticCorePaint.measureText(it.text) + (if (it.hasTrailingSpace) spW else 0f)).toDouble()
                }.toFloat()
            } ?: 0f

            val autoscale = if (maxLineWidth > safeKineticMaxWidth) {
                (safeKineticMaxWidth / maxLineWidth).coerceIn(0.60f, 1.0f)
            } else 1.0f

            val effectiveTextSize = scaledTextSize * autoscale
            lyricsKineticCorePaint.textSize = effectiveTextSize
            lyricsKineticBloomPaint.textSize = effectiveTextSize
            lyricsKineticGhostPaint.textSize = effectiveTextSize

            val effectiveSpaceW = lyricsKineticCorePaint.measureText(" ")
            val lineHeight = effectiveTextSize * 1.18f * lyricsSpacingScale
            val totalBlockHeight = (lines.size - 1) * lineHeight
            var lineBaseY = curSlotActiveY - (totalBlockHeight / 2f) + (effectiveTextSize * 0.34f)

            val chunkAgeMs = (currentTimeMs - chunk.startMs).coerceAtLeast(0L)
            val timeUntilChunkEndMs = (chunk.endMs - currentTimeMs).coerceAtLeast(0L)
            val chunkEntryImpulse = exp(-chunkAgeMs / 75f).coerceIn(0f, 1f)
            val chunkExitImpulse = if (timeUntilChunkEndMs < 110L) {
                ((110L - timeUntilChunkEndMs) / 110f).coerceIn(0f, 1f)
            } else 0f

            val firstWordInChunk = chunk.words.firstOrNull()

            for (line in lines) {
                var curX = lyricsX + lyricsOffsetX
                for (tw in line) {
                    val wordW = lyricsKineticCorePaint.measureText(tw.text)
                    val isWordRevealed = currentTimeMs >= tw.startMs || tw === firstWordInChunk
                    if (isWordRevealed) {
                        val wordAgeMs = (currentTimeMs - tw.startMs).coerceAtLeast(0L)
                        val wordEntryImpulse = exp(-wordAgeMs / 68f).coerceIn(0f, 1f)
                        val motionBlurStrength = maxOf(
                            wordEntryImpulse,
                            chunkEntryImpulse * 0.75f,
                            chunkExitImpulse,
                            cam.introImpulse * 0.88f
                        )

                        // Kinetic vertical slide + spring overshoot + intro whip slide
                        val slideOffsetY = (wordEntryImpulse * effectiveTextSize * 0.34f * cos(wordAgeMs * 0.024f)) -
                            (chunkExitImpulse * effectiveTextSize * 0.42f) +
                            (cam.introImpulse * effectiveTextSize * 0.45f)
                        val popScale = 1.0f + (0.14f * wordEntryImpulse * cos(wordAgeMs * 0.030f))
                        val wordAlpha = ((1f - chunkExitImpulse * 0.65f) * 255f).toInt().coerceIn(0, 255)

                        val wordCenterX = curX + (wordW / 2f)
                        val wordCenterY = lineBaseY + slideOffsetY - (effectiveTextSize * 0.32f)

                        canvas.save()
                        if (abs(popScale - 1.0f) > 0.005f) {
                            canvas.scale(popScale, popScale, wordCenterX, wordCenterY)
                        }

                        val drawY = lineBaseY + slideOffsetY

                        // 1. Directional Vertical Motion Blur Ghost Layers (matches 00:00 and 00:07 "with Bad" in reference video)
                        if (motionBlurStrength > 0.04f) {
                            val stepY = effectiveTextSize * 0.20f * motionBlurStrength
                            val ghostMultipliers = floatArrayOf(-3.2f, -2.1f, -1.1f, 1.1f, 2.1f, 3.2f)
                            val ghostAlphas = floatArrayOf(0.11f, 0.20f, 0.32f, 0.32f, 0.20f, 0.11f)
                            for (g in ghostMultipliers.indices) {
                                val gAlpha = (wordAlpha * ghostAlphas[g] * motionBlurStrength).toInt().coerceIn(0, 255)
                                if (gAlpha > 4) {
                                    lyricsKineticGhostPaint.alpha = gAlpha
                                    canvas.drawText(
                                        tw.text,
                                        curX,
                                        drawY + (ghostMultipliers[g] * stepY),
                                        lyricsKineticGhostPaint
                                    )
                                }
                            }
                        }

                        // 2. Brief White Neon Bloom ONLY during Bass Zoom / Shake ("pas zoom/getar teks nya jadi glow putih singkat")
                        val wordGlowFlash = cam.zoomShakeGlowImpulse.coerceIn(0f, 1f)

                        if (wordGlowFlash > 0.06f) {
                            val bloomRadius = 14f + (26f * wordGlowFlash)
                            val bloomAlpha = (wordAlpha * wordGlowFlash * 0.95f).toInt().coerceIn(0, 255)
                            lyricsKineticBloomPaint.alpha = wordAlpha
                            lyricsKineticBloomPaint.setShadowLayer(
                                bloomRadius,
                                0f,
                                0f,
                                Color.argb(bloomAlpha, 255, 255, 255)
                            )
                            canvas.drawText(tw.text, curX, drawY, lyricsKineticBloomPaint)
                        }

                        // 3. Crisp Bright White Core Pass (clean dark shadow when calm, white glow only on zoom/shake)
                        lyricsKineticCorePaint.alpha = wordAlpha
                        lyricsKineticCorePaint.setShadowLayer(
                            10f,
                            0f,
                            3f,
                            Color.argb((wordAlpha * 0.65f).toInt().coerceIn(0, 180), 0, 0, 0)
                        )
                        canvas.drawText(tw.text, curX, drawY, lyricsKineticCorePaint)

                        canvas.restore()
                    }
                    curX += wordW + (if (tw.hasTrailingSpace) effectiveSpaceW else 0f)
                }
                lineBaseY += lineHeight
            }
        } finally {
            canvas.restoreToCount(clipSave)
        }
    }

    private fun renderKineticTitleCard(
        canvas: Canvas,
        title: String,
        artist: String,
        centerY: Float,
        alpha: Float,
        blurImpulse: Float
    ) {
        if (alpha <= 0.01f) return
        val cardAlpha = (255 * alpha).toInt().coerceIn(0, 255)
        val scaledSize = kineticTextSize * 0.88f * lyricsScale
        lyricsKineticCorePaint.textSize = scaledSize
        lyricsKineticBloomPaint.textSize = scaledSize
        lyricsKineticGhostPaint.textSize = scaledSize

        val titleLines = wrapText(title, lyricsMaxWidth, lyricsKineticCorePaint, maxLines = 2)
        val lineHeight = scaledSize * 1.22f * lyricsSpacingScale
        val hasArtist = artist.isNotBlank()
        val totalH = (titleLines.size * lineHeight) + (if (hasArtist) secTextSize * lyricsScale * 1.25f else 0f)

        var curY = centerY - (totalH / 2f) + (scaledSize * 0.72f)
        for (line in titleLines) {
            if (blurImpulse > 0.05f) {
                val step = scaledSize * 0.18f * blurImpulse
                lyricsKineticGhostPaint.alpha = (cardAlpha * 0.25f * blurImpulse).toInt().coerceIn(0, 255)
                canvas.drawText(line, lyricsX + lyricsOffsetX, curY - step, lyricsKineticGhostPaint)
                canvas.drawText(line, lyricsX + lyricsOffsetX, curY + step, lyricsKineticGhostPaint)

                lyricsKineticBloomPaint.alpha = cardAlpha
                lyricsKineticBloomPaint.setShadowLayer(
                    22f * blurImpulse,
                    0f,
                    0f,
                    Color.argb((cardAlpha * 0.85f * blurImpulse).toInt().coerceIn(0, 255), 255, 255, 255)
                )
                canvas.drawText(line, lyricsX + lyricsOffsetX, curY, lyricsKineticBloomPaint)
            }
            lyricsKineticCorePaint.alpha = cardAlpha
            lyricsKineticCorePaint.setShadowLayer(10f, 0f, 3f, Color.argb((cardAlpha * 0.65f).toInt(), 0, 0, 0))
            canvas.drawText(line, lyricsX + lyricsOffsetX, curY, lyricsKineticCorePaint)
            curY += lineHeight
        }

        if (hasArtist) {
            lyricsNextLinePaint.textSize = secTextSize * lyricsScale
            lyricsNextLinePaint.alpha = (cardAlpha * 0.80f).toInt().coerceIn(0, 255)
            canvas.drawText(artist, lyricsX + lyricsOffsetX, curY + (height * 0.015f), lyricsNextLinePaint)
        }
    }

    private enum class EntryRenderStyle {
        UPCOMING,
        ACTIVE,
        PREVIOUS
    }

    private val entryLinesCache = mutableMapOf<LyricsEntry, List<List<TimedWord>>>()

    private fun sanitizeLyricsList(lyrics: List<LyricsEntry>?): List<LyricsEntry> {
        if (lyrics.isNullOrEmpty()) return emptyList()
        val result = ArrayList<LyricsEntry>(lyrics.size)
        for (entry in lyrics) {
            val primaryText = entry.text.lineSequence().firstOrNull()?.trim() ?: entry.text.trim()
            if (primaryText.isBlank()) continue
            val cleaned = if (primaryText == entry.text) entry else entry.copy(text = primaryText)
            val prev = result.lastOrNull()
            if (prev != null && abs(cleaned.time - prev.time) < 60L) {
                // Avoid duplicate/stacked timestamps that cause two lines to overlap on the same frame
                if (!prev.text.equals(cleaned.text, ignoreCase = true)) {
                    result[result.lastIndex] = prev.copy(text = "${prev.text} • ${cleaned.text}")
                }
            } else {
                result.add(cleaned)
            }
        }
        return result
    }

    private fun getAvailableLyricsWidth(): Float {
        val clipRight = caseRect.left - 16f
        return (clipRight - (lyricsX + lyricsOffsetX) - 18f).coerceAtLeast(width * 0.30f)
    }

    private fun getWrappedLines(entry: LyricsEntry, nextEntry: LyricsEntry?): List<List<TimedWord>> {
        return entryLinesCache.getOrPut(entry) {
            val timedWords = extractTimedWordsForEntry(entry, nextEntry)
            lyricsActivePaint.textSize = activeTextSize * lyricsScale
            wrapTimedWords(timedWords, getAvailableLyricsWidth(), lyricsActivePaint, maxLines = 6)
        }
    }

    private fun paintForStyle(style: EntryRenderStyle): Paint = when (style) {
        EntryRenderStyle.UPCOMING -> lyricsNextLinePaint
        EntryRenderStyle.PREVIOUS -> lyricsPrevPaint
        EntryRenderStyle.ACTIVE -> lyricsActivePaint
    }

    private fun computeEffectiveTextSize(
        lines: List<List<TimedWord>>?,
        targetTextSize: Float,
        style: EntryRenderStyle
    ): Float {
        if (lines.isNullOrEmpty()) return targetTextSize
        val measurePaint = paintForStyle(style)
        measurePaint.textSize = targetTextSize
        val spaceWidth = measurePaint.measureText(" ")
        val availableWidth = getAvailableLyricsWidth()

        val maxLineWidth = lines.maxOfOrNull { line ->
            line.sumOf {
                (measurePaint.measureText(it.text) + (if (it.hasTrailingSpace) spaceWidth else 0f)).toDouble()
            }.toFloat()
        } ?: 0f

        val widthScale = if (maxLineWidth > availableWidth && maxLineWidth > 0f) {
            (availableWidth / maxLineWidth).coerceIn(0.55f, 1.0f)
        } else {
            1.0f
        }

        val lineCountScale = when {
            lines.size >= 5 -> 0.72f
            lines.size == 4 -> 0.80f
            lines.size == 3 -> 0.90f
            else -> 1.0f
        }

        return targetTextSize * minOf(widthScale, lineCountScale)
    }

    private fun computeBlockHeight(
        lines: List<List<TimedWord>>?,
        targetTextSize: Float,
        style: EntryRenderStyle
    ): Float {
        val count = lines?.size ?: 0
        if (count <= 0) return 0f
        val effectiveTextSize = computeEffectiveTextSize(lines, targetTextSize, style)
        val lineHeight = effectiveTextSize * 1.26f * lyricsSpacingScale
        return count * lineHeight
    }

    private fun computeVerticalEdgeFade(centerY: Float, blockHeight: Float): Float {
        val topY = centerY - (blockHeight / 2f)
        val bottomY = centerY + (blockHeight / 2f)
        val topMargin = 22f
        val bottomMargin = height - 22f
        val fadeZone = height * 0.085f

        val topFade = when {
            topY >= topMargin -> 1f
            bottomY <= topMargin - fadeZone -> 0f
            else -> ((topY - (topMargin - fadeZone)) / fadeZone).coerceIn(0f, 1f)
        }
        val bottomFade = when {
            bottomY <= bottomMargin -> 1f
            topY >= bottomMargin + fadeZone -> 0f
            else -> (((bottomMargin + fadeZone) - bottomY) / fadeZone).coerceIn(0f, 1f)
        }
        return minOf(topFade, bottomFade)
    }

    private fun renderLyrics(
        canvas: Canvas,
        currentTimeMs: Long,
        lyrics: List<LyricsEntry>?,
        songTitle: String,
        songArtist: String,
        cam: CameraRigState
    ) {
        val validLyrics = sanitizeLyricsList(lyrics)

        val scaledActiveTextSize = activeTextSize * lyricsScale
        val scaledSecTextSize = secTextSize * lyricsScale
        val scaledPrevTextSize = prevTextSize * lyricsScale

        val minSlotY = 44f + scaledActiveTextSize
        val maxSlotY = height - 44f - scaledActiveTextSize
        val baseActiveY = if (hideInactiveLyrics) height * 0.485f else slotActiveY
        val curSlotActiveY = (baseActiveY + lyricsOffsetY).coerceIn(minSlotY, maxSlotY)

        val lyricsClipRight = caseRect.left - 16f
        val clipSave = canvas.save()
        canvas.clipRect(0f, 0f, lyricsClipRight, height.toFloat())

        if (lyricsRotation != 0f) {
            val lyricPivotX = lyricsX + lyricsOffsetX + (getAvailableLyricsWidth() / 2f)
            val lyricPivotY = curSlotActiveY
            canvas.rotate(lyricsRotation, lyricPivotX, lyricPivotY)
        }

        try {
            if (validLyrics.isEmpty()) {
                renderTitleCard(canvas, songTitle, songArtist, curSlotActiveY, 1.0f)
                return
            }

            val activeIndex = validLyrics.indexOfLast { it.time <= currentTimeMs }
            val transitionDurationMs = 380f

            if (activeIndex == -1) {
                val firstEntry = validLyrics.first()
                val nextAfterFirst = validLyrics.getOrNull(1)
                val firstLines = getWrappedLines(firstEntry, nextAfterFirst)
                val timeUntilFirstMs = firstEntry.time - currentTimeMs

                val titleH = (scaledActiveTextSize * 1.35f) + (if (songArtist.isNotBlank()) scaledSecTextSize * 1.35f else 0f)
                val firstH = computeBlockHeight(firstLines, scaledSecTextSize, EntryRenderStyle.UPCOMING)
                val verticalGap = (height * 0.052f) * lyricsSpacingScale
                val firstDistance = (titleH / 2f) + verticalGap + (firstH / 2f)

                if (timeUntilFirstMs in 0L..380L) {
                    val p = 1f - (timeUntilFirstMs.toFloat() / transitionDurationMs).coerceIn(0f, 1f)
                    val ease = 1f - (1f - p) * (1f - p) * (1f - p)

                    val activeCenterY = if (hideInactiveLyrics) {
                        curSlotActiveY + (1f - ease) * (height * 0.045f)
                    } else {
                        curSlotActiveY + (firstDistance * (1f - ease))
                    }
                    val titleCenterY = activeCenterY - firstDistance
                    val titleAlpha = if (hideInactiveLyrics) {
                        (1f - ease * 1.6f).coerceIn(0f, 1f)
                    } else {
                        (1f - ease).coerceIn(0f, 1f)
                    }
                    renderTitleCard(canvas, songTitle, songArtist, titleCenterY, titleAlpha)

                    val curTextSize = if (hideInactiveLyrics) {
                        scaledActiveTextSize
                    } else {
                        scaledSecTextSize + (scaledActiveTextSize - scaledSecTextSize) * ease
                    }
                    val activeAlpha = (0.50f + 0.50f * ease).coerceIn(0f, 1f)
                    renderEntry(canvas, firstLines, currentTimeMs, activeCenterY, curTextSize, activeAlpha, EntryRenderStyle.ACTIVE, cam)
                } else {
                    renderTitleCard(canvas, songTitle, songArtist, curSlotActiveY, 1.0f)
                    if (!hideInactiveLyrics && showUpcomingLyrics) {
                        val firstCenterY = curSlotActiveY + firstDistance
                        val edgeFade = computeVerticalEdgeFade(firstCenterY, firstH)
                        renderEntry(canvas, firstLines, currentTimeMs, firstCenterY, scaledSecTextSize, 0.45f * edgeFade, EntryRenderStyle.UPCOMING, cam)
                    }
                }
                return
            }

            val activeEntry = validLyrics[activeIndex]
            val prevEntry = if (activeIndex > 0) validLyrics[activeIndex - 1] else null
            val nextEntry = if (activeIndex + 1 < validLyrics.size) validLyrics[activeIndex + 1] else null
            val olderEntry = if (activeIndex > 1) validLyrics[activeIndex - 2] else null
            val futureEntry = if (activeIndex + 2 < validLyrics.size) validLyrics[activeIndex + 2] else null

            val activeLines = getWrappedLines(activeEntry, nextEntry)
            val prevLines = prevEntry?.let { getWrappedLines(it, activeEntry) }
            val nextLines = nextEntry?.let { getWrappedLines(it, futureEntry) }
            val olderLines = olderEntry?.let { getWrappedLines(it, prevEntry) }

            val timeSinceLineStartMs = (currentTimeMs - activeEntry.time).coerceAtLeast(0L)
            val transProgress = (timeSinceLineStartMs / transitionDurationMs).coerceIn(0f, 1f)
            val ease = 1f - (1f - transProgress) * (1f - transProgress) * (1f - transProgress)

            val curActiveTextSize = if (hideInactiveLyrics) {
                scaledActiveTextSize
            } else {
                scaledSecTextSize + (scaledActiveTextSize - scaledSecTextSize) * ease
            }
            val curPrevTextSize = scaledActiveTextSize - (scaledActiveTextSize - scaledPrevTextSize) * ease

            val activeH = computeBlockHeight(activeLines, curActiveTextSize, EntryRenderStyle.ACTIVE)
            val prevH = computeBlockHeight(prevLines, curPrevTextSize, EntryRenderStyle.PREVIOUS)
            val nextH = computeBlockHeight(nextLines, scaledSecTextSize, EntryRenderStyle.UPCOMING)
            val olderH = computeBlockHeight(olderLines, scaledPrevTextSize, EntryRenderStyle.PREVIOUS)

            val verticalGap = (height * 0.052f) * lyricsSpacingScale
            val prevToActiveDistance = (prevH / 2f) + verticalGap + (activeH / 2f)
            val activeToNextDistance = (activeH / 2f) + verticalGap + (nextH / 2f)
            val olderToPrevDistance = (olderH / 2f) + verticalGap + (prevH / 2f)

            // Position activeCenterY first, then anchor prevCenterY, olderCenterY, and nextCenterY
            // strictly relative to activeCenterY so blocks NEVER collide or overlap at any ease value!
            val activeCenterY = if (hideInactiveLyrics) {
                curSlotActiveY + (1f - ease) * (height * 0.045f)
            } else {
                curSlotActiveY + (prevToActiveDistance * (1f - ease))
            }
            val prevCenterY = activeCenterY - prevToActiveDistance
            val olderCenterY = prevCenterY - olderToPrevDistance
            val nextCenterY = activeCenterY + activeToNextDistance

            if (!hideInactiveLyrics) {
                if (olderEntry != null && olderLines != null && ease < 1.0f) {
                    val edgeFade = computeVerticalEdgeFade(olderCenterY, olderH)
                    val olderAlpha = (0.32f * (1f - ease) * edgeFade).coerceIn(0f, 1f)
                    if (olderAlpha > 0.02f) {
                        renderEntry(canvas, olderLines, currentTimeMs, olderCenterY, scaledPrevTextSize, olderAlpha, EntryRenderStyle.PREVIOUS, cam)
                    }
                }

                val prevEdgeFade = computeVerticalEdgeFade(prevCenterY, prevH)
                val prevAlpha = ((1.0f - (0.55f * ease)) * prevEdgeFade).coerceIn(0f, 1f)
                if (prevEntry != null && prevLines != null && prevAlpha > 0.02f) {
                    renderEntry(canvas, prevLines, currentTimeMs, prevCenterY, curPrevTextSize, prevAlpha, EntryRenderStyle.PREVIOUS, cam)
                } else if (activeIndex == 0 && prevAlpha > 0.05f) {
                    val titleH = (scaledActiveTextSize * 1.35f) + (if (songArtist.isNotBlank()) scaledSecTextSize * 1.35f else 0f)
                    val titleDistance = (activeH / 2f) + verticalGap + (titleH / 2f)
                    val titleCenterY = activeCenterY - titleDistance
                    val titleEdgeFade = computeVerticalEdgeFade(titleCenterY, titleH)
                    renderTitleCard(canvas, songTitle, songArtist, titleCenterY, prevAlpha * titleEdgeFade)
                }
            } else {
                // Single-line focus mode: fade out previous line cleanly at prevCenterY (never overlapping activeCenterY)
                if (prevEntry != null && prevLines != null && ease < 0.75f) {
                    val prevEdgeFade = computeVerticalEdgeFade(prevCenterY, prevH)
                    val prevAlpha = ((1.0f - (ease / 0.75f)) * 0.45f * prevEdgeFade).coerceIn(0f, 1f)
                    if (prevAlpha > 0.02f) {
                        renderEntry(canvas, prevLines, currentTimeMs, prevCenterY, curPrevTextSize, prevAlpha, EntryRenderStyle.PREVIOUS, cam)
                    }
                }
            }

            val activeAlpha = (0.55f + 0.45f * ease).coerceIn(0f, 1f)
            renderEntry(canvas, activeLines, currentTimeMs, activeCenterY, curActiveTextSize, activeAlpha, EntryRenderStyle.ACTIVE, cam)

            if (!hideInactiveLyrics && showUpcomingLyrics && nextEntry != null && nextLines != null) {
                val nextEdgeFade = computeVerticalEdgeFade(nextCenterY, nextH)
                val baseNextAlpha = if (ease >= 1.0f) 0.45f else (0.18f + 0.27f * ease)
                val nextAlpha = (baseNextAlpha * nextEdgeFade).coerceIn(0f, 1f)
                if (nextAlpha > 0.02f) {
                    renderEntry(canvas, nextLines, currentTimeMs, nextCenterY, scaledSecTextSize, nextAlpha, EntryRenderStyle.UPCOMING, cam)
                }
            }
        } finally {
            canvas.restoreToCount(clipSave)
        }
    }

    private fun renderEntry(
        canvas: Canvas,
        lines: List<List<TimedWord>>,
        currentTimeMs: Long,
        centerY: Float,
        targetTextSize: Float,
        alpha: Float,
        style: EntryRenderStyle,
        cam: CameraRigState
    ) {
        if (alpha <= 0.01f || lines.isEmpty()) return

        val effectiveTextSize = computeEffectiveTextSize(lines, targetTextSize, style)
        val lineHeight = effectiveTextSize * 1.26f * lyricsSpacingScale
        val lineCount = lines.size
        val totalSpanBetweenLineCenters = (lineCount - 1) * lineHeight

        val refPaint = paintForStyle(style).apply { textSize = effectiveTextSize }
        val fm = refPaint.fontMetrics
        val baselineCenterOffset = -((fm.ascent + fm.descent) / 2f)
        var currentY = (centerY - (totalSpanBetweenLineCenters / 2f)) + baselineCenterOffset

        val curAlpha = (255 * alpha).toInt().coerceIn(0, 255)
        val dimAlpha = (115 * alpha).toInt().coerceIn(0, 255)
        val prevAlpha = (95 * alpha).toInt().coerceIn(0, 255)

        when (style) {
            EntryRenderStyle.UPCOMING -> {
                lyricsNextLinePaint.textSize = effectiveTextSize
                lyricsNextLinePaint.alpha = dimAlpha
                val spW = lyricsNextLinePaint.measureText(" ")
                for (line in lines) {
                    var curX = lyricsX + lyricsOffsetX
                    for (tw in line) {
                        canvas.drawText(tw.text, curX, currentY, lyricsNextLinePaint)
                        curX += lyricsNextLinePaint.measureText(tw.text) + (if (tw.hasTrailingSpace) spW else 0f)
                    }
                    currentY += lineHeight
                }
            }
            EntryRenderStyle.PREVIOUS -> {
                lyricsPrevPaint.textSize = effectiveTextSize
                lyricsPrevPaint.alpha = prevAlpha
                val spW = lyricsPrevPaint.measureText(" ")
                for (line in lines) {
                    var curX = lyricsX + lyricsOffsetX
                    for (tw in line) {
                        canvas.drawText(tw.text, curX, currentY, lyricsPrevPaint)
                        curX += lyricsPrevPaint.measureText(tw.text) + (if (tw.hasTrailingSpace) spW else 0f)
                    }
                    currentY += lineHeight
                }
            }
            EntryRenderStyle.ACTIVE -> {
                lyricsActivePaint.textSize = effectiveTextSize
                lyricsUpcomingPaint.textSize = effectiveTextSize

                // Brief white glow ONLY during zoom/bass shake in SMOOTH_FLOAT / KINETIC_SHAKE
                val glowFlash = if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                    cam.zoomShakeGlowImpulse
                } else 0f

                val activeWordPaint = Paint(lyricsActivePaint).apply {
                    this.alpha = curAlpha
                    if (glowFlash > 0.06f) {
                        setShadowLayer(
                            14f + 22f * glowFlash,
                            0f,
                            0f,
                            Color.argb((curAlpha * glowFlash * 0.95f).toInt().coerceIn(0, 255), 255, 255, 255)
                        )
                    } else {
                        setShadowLayer(12f * alpha, 0f, 3f, Color.parseColor("#A0000000"))
                    }
                }
                val upcomingWordPaint = Paint(lyricsUpcomingPaint).apply {
                    this.alpha = dimAlpha
                }
                val spW = lyricsActivePaint.measureText(" ")

                for (line in lines) {
                    var curX = lyricsX + lyricsOffsetX
                    for (tw in line) {
                        val wordWidth = lyricsActivePaint.measureText(tw.text)
                        val p = tw.progressAt(currentTimeMs)
                        when {
                            p >= 1f -> {
                                canvas.drawText(tw.text, curX, currentY, activeWordPaint)
                            }
                            p <= 0f -> {
                                canvas.drawText(tw.text, curX, currentY, upcomingWordPaint)
                            }
                            else -> {
                                val sweepPaint = Paint(activeWordPaint).apply {
                                    val pSpread = 0.08f
                                    val p0 = (p - pSpread).coerceAtLeast(0f)
                                    val p1 = (p + pSpread).coerceAtMost(1f)
                                    shader = LinearGradient(
                                        curX, 0f, curX + wordWidth, 0f,
                                        intArrayOf(
                                            Color.argb(curAlpha, 255, 255, 255),
                                            Color.argb(curAlpha, 255, 255, 255),
                                            Color.argb(dimAlpha, 255, 255, 255),
                                            Color.argb(dimAlpha, 255, 255, 255)
                                        ),
                                        floatArrayOf(0f, p0, p1, 1f),
                                        Shader.TileMode.CLAMP
                                    )
                                }
                                canvas.drawText(tw.text, curX, currentY, sweepPaint)
                            }
                        }
                        curX += wordWidth + (if (tw.hasTrailingSpace) spW else 0f)
                    }
                    currentY += lineHeight
                }
            }
        }
    }

    private fun renderTitleCard(
        canvas: Canvas,
        title: String,
        artist: String,
        centerY: Float,
        alpha: Float
    ) {
        if (alpha <= 0.01f) return
        val cardAlpha = (255 * alpha).toInt().coerceIn(0, 255)
        val availW = getAvailableLyricsWidth()
        lyricsActivePaint.textSize = activeTextSize * lyricsScale
        lyricsActivePaint.alpha = cardAlpha

        val titleLines = wrapText(title, availW, lyricsActivePaint, maxLines = 3)
        val lineHeight = lyricsActivePaint.textSize * 1.25f * lyricsSpacingScale
        val hasArtist = artist.isNotBlank()
        val totalH = (titleLines.size * lineHeight) + (if (hasArtist) secTextSize * lyricsScale * 1.25f else 0f)

        var curY = centerY - (totalH / 2f) + (lyricsActivePaint.textSize * 0.75f)
        for (line in titleLines) {
            canvas.drawText(line, lyricsX + lyricsOffsetX, curY, lyricsActivePaint)
            curY += lineHeight
        }

        if (hasArtist) {
            lyricsNextLinePaint.textSize = secTextSize * lyricsScale
            lyricsNextLinePaint.alpha = (cardAlpha * 0.75f).toInt().coerceIn(0, 255)
            val artistLines = wrapText(artist, availW, lyricsNextLinePaint, maxLines = 1)
            for (aLine in artistLines) {
                canvas.drawText(aLine, lyricsX + lyricsOffsetX, curY + (height * 0.015f), lyricsNextLinePaint)
            }
        }
    }

    private fun wrapTimedWords(
        words: List<TimedWord>,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int = 6
    ): List<List<TimedWord>> {
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<MutableList<TimedWord>>()
        var currentLine = mutableListOf<TimedWord>()
        var currentLineWidth = 0f
        val spaceWidth = paint.measureText(" ")

        for (i in words.indices) {
            val word = words[i]
            val wordWidth = paint.measureText(word.text)
            val trailingWidth = if (word.hasTrailingSpace) spaceWidth else 0f

            if (currentLine.isEmpty() || currentLineWidth + wordWidth <= maxWidth) {
                currentLine.add(word)
                currentLineWidth += wordWidth + trailingWidth
            } else {
                if (lines.size < maxLines - 1) {
                    lines.add(currentLine)
                    currentLine = mutableListOf(word)
                    currentLineWidth = wordWidth + trailingWidth
                } else {
                    // On the final allowed line, keep adding remaining words so nothing gets cut off;
                    // computeEffectiveTextSize will automatically scale the font down to fit!
                    currentLine.add(word)
                    currentLineWidth += wordWidth + trailingWidth
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint, maxLines: Int = 1): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (i in words.indices) {
            val word = words[i]
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    if (lines.size >= maxLines) {
                        val lastIdx = lines.lastIndex
                        var elLine = lines[lastIdx]
                        while (paint.measureText("$elLine…") > maxWidth && elLine.isNotEmpty()) {
                            elLine = elLine.dropLast(1).trimEnd()
                        }
                        lines[lastIdx] = if (elLine.length < lines[lastIdx].length) "$elLine…" else lines[lastIdx]
                        return lines
                    }
                    currentLine = StringBuilder(word)
                } else {
                    lines.add(word)
                    if (lines.size >= maxLines) return lines
                    currentLine = StringBuilder()
                }
            }
        }

        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine.toString())
        }

        return lines
    }
}
