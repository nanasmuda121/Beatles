/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.poster

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.palette.graphics.Palette
import kotlin.math.max
import kotlin.math.min

enum class PosterStep { MODE, SONGS, PHOTOS, CUTOUT, BACKGROUND, EDITOR }

enum class PosterMode { COLLAGE, PHOTO3D }

enum class PosterAspect(val label: String, val ratio: Float) {
    STORY("9:16", 9f / 16f),
    PORTRAIT("4:5", 4f / 5f),
    SQUARE("1:1", 1f),
}

enum class BackgroundMode { PHOTO, SOLID, GRADIENT }

enum class ElementKind { BACKGROUND, CARD, CUTOUT }

enum class SlotStatus { LOADING, DONE, ERROR }

@Immutable
data class PosterSong(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Int,
    val thumbnailUrl: String?,
)

/** Data satu kartu lagu di poster. [color] null = ikut warna dominan cover. */
@Immutable
data class PosterCardData(
    val song: PosterSong,
    val cover: Bitmap?,
    val autoColor: Int,
    val color: Int? = null,
    val progress: Float = 0.1f,
) {
    val effectiveColor: Int get() = color ?: autoColor
}

/**
 * Satu elemen yang bisa digeser/diputar/di-zoom. Posisi [cx]/[cy] adalah pecahan lebar/tinggi poster.
 * Urutan di list = urutan layer (indeks 0 paling belakang).
 */
@Immutable
data class PosterElement(
    val id: String,
    val kind: ElementKind,
    val refIndex: Int,
    val cx: Float,
    val cy: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipX: Boolean = false,
    /** Hanya dipakai kind BACKGROUND: jumlah pita background (tinggi pita = tinggi poster / bgCount). */
    val bgCount: Int = 1,
    /** Kemiringan 3D: rotX = atas/bawah, rotY = kiri/kanan (derajat). Dipakai kartu Mode 2. */
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    /** Elemen dengan group sama bergerak bersama (foto + cutout dirinya di Mode 2). */
    val group: String? = null,
    /** true = elemen menutupi seluruh poster (crop), dipakai foto utama dan cutout-nya di Mode 2. */
    val fill: Boolean = false,
)

object PosterMetrics {
    const val CARD_W = 0.60f          // lebar kartu = 60% lebar poster
    const val CARD_ASPECT = 0.56f     // tinggi kartu = 56% lebar kartu
    const val CUTOUT_H = 0.46f        // tinggi dasar cutout = 46% lebar poster
    const val V_CARD_W = 0.33f        // Mode 2: kartu vertikal, lebar 33% lebar poster
    const val V_CARD_ASPECT = 1.72f   // tinggi kartu = 1.72 x lebar kartu
    const val MAX_V_SONGS = 4
    const val MAX_PHOTOS = 3
    const val MAX_BACKGROUNDS = 3
    const val SONG_COUNT = 3
}

class PhotoSlot(val id: String, val uri: android.net.Uri) {
    var status by mutableStateOf(SlotStatus.LOADING)
    var error by mutableStateOf<String?>(null)
    var original: Bitmap? = null
    var editor by mutableStateOf<CutoutEditor?>(null)
    var keepOriginal by mutableStateOf(false)
    /** Hasil akhir yang dipakai di poster (sudah dipangkas). */
    var finalBitmap: Bitmap? = null
}

val CARD_COLOR_PRESETS: List<Int> = listOf(
    0xFF3A3A3A.toInt(), 0xFF5E2227.toInt(), 0xFF4E5428.toInt(), 0xFF1F3B5C.toInt(),
    0xFF3D2B56.toInt(), 0xFF1E4D3B.toInt(), 0xFF000000.toInt(), 0xFFEDEDED.toInt(),
)

val SOLID_BG_PRESETS: List<Int> = listOf(
    0xFF101217.toInt(), 0xFF1F3B5C.toInt(), 0xFF5E2227.toInt(), 0xFF2B2B2B.toInt(),
    0xFFE8DCC8.toInt(), 0xFF3D2B56.toInt(),
)

val GRADIENT_BG_PRESETS: List<Pair<Int, Int>> = listOf(
    0xFF2B1055.toInt() to 0xFF7597DE.toInt(),
    0xFF0F2027.toInt() to 0xFF2C5364.toInt(),
    0xFF8E2DE2.toInt() to 0xFF4A00E0.toInt(),
    0xFFFF512F.toInt() to 0xFFDD2476.toInt(),
    0xFF134E5E.toInt() to 0xFF71B280.toInt(),
)

object PosterColors {
    private const val FALLBACK = 0xFF3A3A3A.toInt()

    /** Warna kartu otomatis dari cover: diambil dari Palette lalu digelapkan agar teks putih terbaca. */
    fun autoCardColor(cover: Bitmap?): Int {
        if (cover == null) return FALLBACK
        return try {
            val p = Palette.from(cover).maximumColorCount(16).generate()
            val rgb = p.darkMutedSwatch?.rgb
                ?: p.mutedSwatch?.rgb
                ?: p.dominantSwatch?.rgb
                ?: return FALLBACK
            val hsv = FloatArray(3)
            Color.colorToHSV(rgb, hsv)
            hsv[1] = min(hsv[1], 0.75f)
            hsv[2] = max(0.20f, min(hsv[2], 0.36f))
            Color.HSVToColor(hsv)
        } catch (_: Exception) {
            FALLBACK
        }
    }
}

object PosterBlur {
    /**
     * Blur foto background. [level] 0..1. Hasil berukuran maks 480px sisi terpanjang
     * (cukup, karena tampil buram) kecuali level ~0 (mengikuti ukuran asli, maks 1280px).
     */
    fun blur(src: Bitmap, level: Float): Bitmap {
        val maxSide = if (level < 0.02f) 1280 else 480
        val s = min(1f, maxSide.toFloat() / max(src.width, src.height))
        val w = max(1, (src.width * s).toInt())
        val h = max(1, (src.height * s).toInt())
        val small = if (w == src.width && h == src.height) src else Bitmap.createScaledBitmap(src, w, h, true)
        if (level < 0.02f) return small

        val n = w * h
        val px = IntArray(n)
        small.getPixels(px, 0, w, 0, 0, w, h)
        val radius = (1 + level * 22f).toInt().coerceAtLeast(1)
        fun channel(shift: Int): FloatArray {
            var arr = FloatArray(n) { ((px[it] shr shift) and 255).toFloat() }
            repeat(3) { arr = MaskRefiner.boxFilter(arr, w, h, radius) }
            return arr
        }
        val r = channel(16)
        val g = channel(8)
        val b = channel(0)
        val out = IntArray(n) {
            (0xFF shl 24) or
                (r[it].toInt().coerceIn(0, 255) shl 16) or
                (g[it].toInt().coerceIn(0, 255) shl 8) or
                b[it].toInt().coerceIn(0, 255)
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        if (small !== src) small.recycle()
        return result
    }
}
