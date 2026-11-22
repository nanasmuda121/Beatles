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
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.nanas.db.entities.Song
import kotlin.math.max
import kotlin.math.min

enum class PosterStep { MODE, SONGS, PHOTOS, CUTOUT, BACKGROUND, EDITOR }

enum class PosterMode { COLLAGE, PHOTO3D }

enum class PosterAspect(val label: String, val ratio: Float) {
    ORIGINAL("Asli Foto", 9f / 16f),
    STORY("9:16", 9f / 16f),
    RATIO_3_4("3:4", 3f / 4f),
    PORTRAIT("4:5", 4f / 5f),
    COLLAGE("Poster", 1080f / 1526f),
    SQUARE("1:1", 1f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    WIDE_16_9("16:9", 16f / 9f),
}

enum class BackgroundMode { PHOTO, SOLID, GRADIENT }

enum class ElementKind { BACKGROUND, CARD, CUTOUT, STICKER }

val STAR_STICKER_RES = listOf(
    com.nanzbeatles.nanas.R.drawable.sticker_star_01,
    com.nanzbeatles.nanas.R.drawable.sticker_star_02,
    com.nanzbeatles.nanas.R.drawable.sticker_star_03,
    com.nanzbeatles.nanas.R.drawable.sticker_star_04,
    com.nanzbeatles.nanas.R.drawable.sticker_star_05,
    com.nanzbeatles.nanas.R.drawable.sticker_star_06,
    com.nanzbeatles.nanas.R.drawable.sticker_star_07,
    com.nanzbeatles.nanas.R.drawable.sticker_star_08,
    com.nanzbeatles.nanas.R.drawable.sticker_star_09,
    com.nanzbeatles.nanas.R.drawable.sticker_star_10,
    com.nanzbeatles.nanas.R.drawable.sticker_star_11,
    com.nanzbeatles.nanas.R.drawable.sticker_star_12,
    com.nanzbeatles.nanas.R.drawable.sticker_star_13,
    com.nanzbeatles.nanas.R.drawable.sticker_star_14,
)

enum class SlotStatus { LOADING, DONE, ERROR }

@Immutable
data class PosterSong(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Int,
    val thumbnailUrl: String?,
)

fun Song.toPosterSong() = PosterSong(
    id = song.id,
    title = song.title,
    artist = artists.joinToString(", ") { it.name }.ifBlank { "Artis tidak diketahui" },
    durationSec = song.duration.takeIf { it > 0 } ?: 200,
    thumbnailUrl = song.thumbnailUrl,
)

fun SongItem.toPosterSong() = PosterSong(
    id = id,
    title = title,
    artist = artists.joinToString(", ") { it.name }.ifBlank { "Artis tidak diketahui" },
    durationSec = duration?.takeIf { it > 0 } ?: 200,
    thumbnailUrl = thumbnail,
)

/**
 * Menyimpan konteks daftar putar (playlist) yang dipilih user saat menekan titik 3 -> "Buat Poster".
 */
object PosterSession {
    var playlistName: String = ""
        private set
    var playlistId: String? = null
        private set
    var browseId: String? = null
        private set
    var songs: List<PosterSong> = emptyList()
        private set
    var sessionVersion: Int = 0
        private set

    var navigateCallback: (() -> Unit)? = null

    fun openFromPlaylist(
        name: String,
        songs: List<PosterSong>,
        playlistId: String? = null,
        browseId: String? = null,
    ) {
        this.playlistName = name
        this.songs = songs
        this.playlistId = playlistId
        this.browseId = browseId
        this.sessionVersion++
        navigateCallback?.invoke()
    }
}

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
    /** Posisi kilau kartu Mode 2: 0 = kanan-atas, 1 = kiri-atas, 2 = kanan-atas + kanan-bawah. */
    val sparkleStyle: Int = 0,
    /** Elemen dengan group sama bergerak bersama (null = bergerak terpisah secara independen). */
    val group: String? = null,
    /** true = elemen menutupi seluruh poster (crop). */
    val fill: Boolean = false,
)

object PosterMetrics {
    const val CARD_W = 0.63f          // Mode 1: lebar kartu = 63% lebar poster (sesuai referensi 680/1080)
    const val CARD_ASPECT = 0.66f     // Mode 1: tinggi kartu = 66% lebar kartu (sesuai referensi 460/680)
    const val CUTOUT_H = 0.45f        // Mode 1: tinggi dasar cutout
    const val V_CARD_W = 0.44f        // Mode 2: kartu vertikal, lebar 44% lebar poster (sesuai referensi)
    const val V_CARD_ASPECT = 1.52f   // Mode 2: tinggi kartu = 1.52 x lebar kartu agar kontrol pause/prev/next & volume utuh
    const val MAX_V_SONGS = Int.MAX_VALUE
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
    /** Hasil akhir cutout karakter yang sudah dipangkas rapat ke batas karakter. */
    var finalBitmap: Bitmap? = null
    /** Background bersih yang sudah dihapus karakternya (inpainted) agar background & karakter benar-benar terpisah. */
    var cleanBackground: Bitmap? = null
    /** Bounds karakter relatif terhadap foto asli: [cxFrac, cyFrac, wFrac, hFrac]. */
    var cutoutBoundsInOriginal: FloatArray? = null
}

val CARD_COLOR_PRESETS: List<Int> = listOf(
    0xFF3E3E3E.toInt(), 0xFF5A2023.toInt(), 0xFF4E5329.toInt(), 0xFF1F3B5C.toInt(),
    0xFF3D2B56.toInt(), 0xFF1E4D3B.toInt(), 0xFF23252A.toInt(), 0xFFEDEDED.toInt(),
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
    private const val FALLBACK = 0xFF3E3E3E.toInt()

    /** Warna kartu otomatis dari cover: diambil dari Palette lalu digelapkan agar teks putih terbaca seperti referensi. */
    fun autoCardColor(cover: Bitmap?): Int {
        if (cover == null) return FALLBACK
        return try {
            val p = Palette.from(cover).maximumColorCount(24).generate()
            val rgb = p.darkVibrantSwatch?.rgb
                ?: p.darkMutedSwatch?.rgb
                ?: p.vibrantSwatch?.rgb
                ?: p.mutedSwatch?.rgb
                ?: p.dominantSwatch?.rgb
                ?: return FALLBACK
            val hsv = FloatArray(3)
            Color.colorToHSV(rgb, hsv)
            hsv[1] = min(hsv[1], 0.72f)
            hsv[2] = max(0.22f, min(hsv[2], 0.36f))
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
        val radius = (1 + level * 26f).toInt().coerceAtLeast(1)
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
