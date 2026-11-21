/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.poster

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.innertube.utils.completed
import com.nanzbeatles.nanas.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

sealed interface PosterSaveResult {
    data class Success(val label: String) : PosterSaveResult
    data object NeedsPermission : PosterSaveResult
    data class Failed(val message: String) : PosterSaveResult
}

@HiltViewModel
class PosterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    // ------------------------------------------------------------------ navigasi wizard
    var step by mutableStateOf(PosterStep.MODE)
        private set

    fun goTo(target: PosterStep) {
        step = target
    }

    /** Kembali satu langkah. Return false jika sudah di langkah pertama (layar harus ditutup). */
    fun back(): Boolean {
        val prev = when (step) {
            PosterStep.MODE -> return false
            PosterStep.SONGS -> PosterStep.MODE
            PosterStep.PHOTOS -> PosterStep.SONGS
            PosterStep.CUTOUT -> PosterStep.PHOTOS
            PosterStep.BACKGROUND -> PosterStep.CUTOUT
            PosterStep.EDITOR ->
                if (mode == PosterMode.PHOTO3D) PosterStep.CUTOUT else PosterStep.BACKGROUND
        }
        step = prev
        return true
    }

    // ------------------------------------------------------------------ mode
    var mode by mutableStateOf(PosterMode.COLLAGE)
        private set
    var sparkle by mutableStateOf(true)

    val minSongs: Int get() = if (mode == PosterMode.PHOTO3D) 1 else PosterMetrics.SONG_COUNT
    val maxSongs: Int get() = if (mode == PosterMode.PHOTO3D) PosterMetrics.MAX_V_SONGS else PosterMetrics.SONG_COUNT
    val maxPhotos: Int get() = if (mode == PosterMode.PHOTO3D) 1 else PosterMetrics.MAX_PHOTOS

    /** Pilih mode. Ganti mode = mulai dari awal supaya data dua mode tidak bercampur. */
    fun selectMode(value: PosterMode) {
        if (value != mode) {
            mode = value
            selectedSongs.clear()
            cards = emptyList()
            photos.clear()
            currentPhoto = 0
            elements = emptyList()
            selectedId = null
            undoStack.clear()
            redoStack.clear()
            layoutSignature = ""
            historyVersion++
        }
        // Mode 2 dikunci ke aspek potrait 9:16 seperti referensi; Mode 1 default ke proporsi poster referensi Mode 1
        aspect = if (value == PosterMode.PHOTO3D) PosterAspect.STORY else PosterAspect.COLLAGE
        loadPlaylistSongsIfNeeded()
        step = PosterStep.SONGS
    }

    /** Background yang dikirim ke kanvas: foto utama (Mode 2) atau kolase blur (Mode 1). */
    val canvasBackgrounds: List<Bitmap>
        get() = if (mode == PosterMode.PHOTO3D) listOfNotNull(readyPhotos.firstOrNull()?.original) else bgBlurred

    val effectiveBgMode: BackgroundMode
        get() = if (mode == PosterMode.PHOTO3D) BackgroundMode.PHOTO else bgMode

    // ------------------------------------------------------------------ lagu dari daftar putar (playlist)
    var playlistName by mutableStateOf(PosterSession.playlistName)
        private set
    var playlistLoading by mutableStateOf(false)
        private set

    private val _playlistSongs = MutableStateFlow<List<PosterSong>>(PosterSession.songs)
    val playlistSongs: StateFlow<List<PosterSong>> = _playlistSongs.asStateFlow()

    private var loadedSessionVersion = -1

    init {
        loadPlaylistSongsIfNeeded()
    }

    fun loadPlaylistSongsIfNeeded() {
        val ver = PosterSession.sessionVersion
        if (ver == loadedSessionVersion && _playlistSongs.value.isNotEmpty()) return
        loadedSessionVersion = ver
        playlistName = PosterSession.playlistName.ifBlank { "Daftar Putar" }

        val initial = PosterSession.songs
        if (initial.isNotEmpty()) {
            _playlistSongs.value = initial
            return
        }

        val pid = PosterSession.playlistId
        val bid = PosterSession.browseId
        playlistLoading = true
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                var list = emptyList<PosterSong>()
                if (!pid.isNullOrBlank()) {
                    val dbSongs = database.playlistSongs(pid).firstOrNull().orEmpty()
                    list = dbSongs
                        .filter { !it.song.song.isVideo }
                        .map { it.song.toPosterSong() }
                }
                if (list.isEmpty() && !bid.isNullOrBlank()) {
                    val ytSongs = YouTube.playlist(bid.removePrefix("VL")).completed().getOrNull()?.songs.orEmpty()
                    list = ytSongs.map { it.toPosterSong() }
                }
                if (list.isEmpty()) {
                    val all = database.allSongs().firstOrNull().orEmpty()
                    list = all.filter { !it.song.isVideo }.map { it.toPosterSong() }
                }
                list
            }
            _playlistSongs.value = loaded
            playlistLoading = false
        }
    }

    val selectedSongs = mutableStateListOf<PosterSong>()

    fun toggleSong(song: PosterSong) {
        val existing = selectedSongs.indexOfFirst { it.id == song.id }
        if (existing >= 0) {
            selectedSongs.removeAt(existing)
        } else if (selectedSongs.size < maxSongs) {
            selectedSongs.add(song)
        }
    }

    var cards by mutableStateOf<List<PosterCardData>>(emptyList())
        private set
    var cardsLoading by mutableStateOf(false)
        private set

    /** Muat cover + warna kartu untuk lagu terpilih dari daftar putar, lalu lanjut ke langkah foto. */
    fun confirmSongs() {
        if (selectedSongs.size !in minSongs..maxSongs) return
        val songs = selectedSongs.toList()
        cardsLoading = true
        viewModelScope.launch {
            val defaults = if (mode == PosterMode.PHOTO3D) floatArrayOf(0.38f, 0.44f, 0.68f, 0.30f)
            else floatArrayOf(0.08f, 0.06f, 0.82f)
            val vertical = mode == PosterMode.PHOTO3D
            val built = withContext(Dispatchers.IO) {
                songs.mapIndexed { i, s ->
                    val cover = loadCover(s.thumbnailUrl)
                    PosterCardData(
                        song = s,
                        cover = cover,
                        autoColor = if (vertical) 0xFF23252A.toInt() else PosterColors.autoCardColor(cover),
                        progress = defaults[i % defaults.size],
                    )
                }
            }
            cards = built
            cardsLoading = false
            step = PosterStep.PHOTOS
        }
    }

    private suspend fun loadCover(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(512)
                .allowHardware(false)
                .build()
            loader.execute(request).image?.toBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun updateCardProgress(index: Int, progress: Float) {
        cards = cards.mapIndexed { i, c -> if (i == index) c.copy(progress = progress.coerceIn(0f, 1f)) else c }
    }

    fun updateCardColor(index: Int, color: Int?) {
        cards = cards.mapIndexed { i, c -> if (i == index) c.copy(color = color) else c }
    }

    // ------------------------------------------------------------------ foto orang + cutout
    val photos = mutableStateListOf<PhotoSlot>()
    var currentPhoto by mutableIntStateOf(0)
    var photoValidationMessage by mutableStateOf<String?>(null)
        private set

    private val remover: BackgroundRemover = MlKitSelfieRemover()
    private val processMutex = Mutex()

    fun addPhotos(uris: List<Uri>) {
        photoValidationMessage = null
        val room = maxPhotos - photos.size
        uris.take(room.coerceAtLeast(0)).forEach { uri ->
            val slot = PhotoSlot(UUID.randomUUID().toString(), uri)
            photos.add(slot)
            process(slot)
        }
    }

    fun removePhoto(slot: PhotoSlot) {
        photos.remove(slot)
        if (currentPhoto >= photos.size) currentPhoto = (photos.size - 1).coerceAtLeast(0)
    }

    fun retry(slot: PhotoSlot) = process(slot)

    private fun process(slot: PhotoSlot) {
        slot.status = SlotStatus.LOADING
        slot.error = null
        viewModelScope.launch {
            processMutex.withLock {
                try {
                    val original = PosterImageLoader.decode(context, slot.uri, 1280)
                    // Mode 2 hanya tersedia untuk gambar potrait (vertikal) sesuai permintaan user
                    if (mode == PosterMode.PHOTO3D && original.width >= original.height) {
                        original.recycle()
                        photos.remove(slot)
                        photoValidationMessage = "Template Mode 2 hanya tersedia untuk gambar potrait (vertikal). Silakan pilih foto potrait."
                        return@withLock
                    }
                    slot.original = original
                    try {
                        slot.editor = CutoutEditor.create(original, remover)
                    } catch (e: Exception) {
                        // Segmentasi gagal: foto tetap bisa dipakai apa adanya.
                        slot.editor = null
                        slot.keepOriginal = true
                        slot.error = "Hapus background gagal, foto dipakai apa adanya."
                    }
                    slot.status = SlotStatus.DONE
                } catch (e: OutOfMemoryError) {
                    slot.status = SlotStatus.ERROR
                    slot.error = "Memori tidak cukup untuk foto ini. Coba foto yang lebih kecil."
                } catch (e: Exception) {
                    slot.status = SlotStatus.ERROR
                    slot.error = e.message ?: "Foto gagal diproses"
                }
            }
        }
    }

    /** Pangkas cutout dan siapkan bitmap akhir tiap foto, lalu lanjut ke background. */
    fun finishCutouts() {
        viewModelScope.launch {
            val full = mode == PosterMode.PHOTO3D
            withContext(Dispatchers.Default) {
                photos.forEach { slot ->
                    val editor = slot.editor
                    slot.finalBitmap = when {
                        slot.status != SlotStatus.DONE -> null
                        slot.keepOriginal || editor == null ->
                            if (full) null else slot.original
                        // Mode 2: ukuran penuh (tidak dipangkas) supaya pas menimpa foto aslinya
                        full -> editor.bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        else -> editor.finalCropped()
                    }
                }
            }
            if (full) enterEditor() else step = PosterStep.BACKGROUND
        }
    }

    val readyPhotos: List<PhotoSlot> get() = photos.filter { it.status == SlotStatus.DONE }

    // ------------------------------------------------------------------ background
    var bgMode by mutableStateOf(BackgroundMode.PHOTO)
    var bgBlur by mutableFloatStateOf(0.72f)
    var solidColor by mutableIntStateOf(SOLID_BG_PRESETS[0])
    var gradientIndex by mutableIntStateOf(0)

    private val bgRaw = ArrayList<Bitmap>()
    var bgBlurred by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var bgLoading by mutableStateOf(false)
        private set
    var bgCount by mutableIntStateOf(0)
        private set
    private var blurJob: Job? = null

    fun addBackgrounds(uris: List<Uri>) {
        val room = PosterMetrics.MAX_BACKGROUNDS - bgRaw.size
        if (room <= 0 || uris.isEmpty()) return
        bgLoading = true
        viewModelScope.launch {
            uris.take(room).forEach { uri ->
                try {
                    bgRaw.add(PosterImageLoader.decode(context, uri, 1280))
                } catch (_: Exception) {
                    // lewati foto yang gagal dibaca
                }
            }
            bgCount = bgRaw.size
            rebuildBlur()
        }
    }

    fun removeBackgroundPhoto(index: Int) {
        if (index !in bgRaw.indices) return
        bgRaw.removeAt(index)
        bgCount = bgRaw.size
        bgLoading = true
        viewModelScope.launch { rebuildBlur() }
    }

    fun applyBlur(level: Float) {
        bgBlur = level
        bgLoading = true
        viewModelScope.launch { rebuildBlur() }
    }

    private suspend fun rebuildBlur() {
        blurJob?.cancel()
        val job = viewModelScope.launch {
            val level = bgBlur
            val result = withContext(Dispatchers.Default) { bgRaw.map { PosterBlur.blur(it, level) } }
            bgBlurred = result
            bgLoading = false
        }
        blurJob = job
        job.join()
    }

    fun canContinueFromBackground(): Boolean =
        bgMode != BackgroundMode.PHOTO || bgRaw.isNotEmpty()

    fun enterEditor() {
        if (mode == PosterMode.PHOTO3D) {
            aspect = PosterAspect.STORY
        }
        val sig = "$mode|$bgMode|${bgBlurred.size}|${readyPhotos.size}|${cards.joinToString(",") { it.song.id }}"
        if (elements.isEmpty() || sig != layoutSignature) {
            buildDefaultLayout()
            layoutSignature = sig
        }
        step = PosterStep.EDITOR
    }

    // ------------------------------------------------------------------ editor: elemen & layer
    var aspect by mutableStateOf(PosterAspect.COLLAGE)
    var elements by mutableStateOf<List<PosterElement>>(emptyList())
        private set
    var selectedId by mutableStateOf<String?>(null)
        private set

    private val undoStack = ArrayList<List<PosterElement>>()
    private val redoStack = ArrayList<List<PosterElement>>()
    var historyVersion by mutableIntStateOf(0)
        private set
    private var layoutSignature = ""
    private var lastTransformMs = 0L

    val canUndo: Boolean get() = historyVersion >= 0 && undoStack.isNotEmpty()
    val canRedo: Boolean get() = historyVersion >= 0 && redoStack.isNotEmpty()

    val selected: PosterElement? get() = elements.firstOrNull { it.id == selectedId }

    private fun pushHistory() {
        undoStack.add(elements)
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
        historyVersion++
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(elements)
        elements = undoStack.removeAt(undoStack.size - 1)
        if (selected == null) selectedId = null
        historyVersion++
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(elements)
        elements = redoStack.removeAt(redoStack.size - 1)
        if (selected == null) selectedId = null
        historyVersion++
    }

    fun select(id: String?) {
        selectedId = id
    }

    /** Aspek (tinggi/lebar) bitmap cutout untuk foto ke-[index]; 1.5 jika tidak ada. */
    fun cutoutAspect(index: Int): Float {
        val b = readyPhotos.getOrNull(index)?.finalBitmap ?: return 1.5f
        return b.height.toFloat() / b.width.toFloat()
    }

    fun cutoutBitmap(index: Int): Bitmap? = readyPhotos.getOrNull(index)?.finalBitmap

    /** Daftar bitmap cutout per indeks foto (Mode 2: ukuran penuh atau null). */
    val cutoutBitmaps: List<Bitmap?> get() = readyPhotos.map { it.finalBitmap }

    /** Tata letak default zig-zag Mode 1 (100% mirip referensi Mode 1). */
    fun buildDefaultLayout() {
        if (mode == PosterMode.PHOTO3D) {
            buildPhoto3dLayout()
            return
        }
        val list = ArrayList<PosterElement>()
        val ratio = aspect.ratio // lebar/tinggi
        val canvasH = 1f / ratio // tinggi poster dalam satuan lebar
        if (bgMode == BackgroundMode.PHOTO && bgBlurred.isNotEmpty()) {
            val n = bgBlurred.size
            for (i in 0 until n) {
                list.add(
                    PosterElement(
                        id = UUID.randomUUID().toString(),
                        kind = ElementKind.BACKGROUND,
                        refIndex = i,
                        cx = 0.5f,
                        cy = (i + 0.5f) / n,
                        bgCount = n,
                    )
                )
            }
        }
        val cardH = PosterMetrics.CARD_W * PosterMetrics.CARD_ASPECT
        // Koordinat zig-zag persis sesuai referensi Mode 1: kanan-atas, kiri-tengah, kanan-bawah
        val cardSlots = listOf(0.54f to 0.217f, 0.35f to 0.515f, 0.54f to 0.815f)
        val cutCount = readyPhotos.size
        cards.forEachIndexed { i, _ ->
            val (cx, cy) = cardSlots[i % cardSlots.size]
            list.add(
                PosterElement(
                    id = UUID.randomUUID().toString(),
                    kind = ElementKind.CARD,
                    refIndex = i,
                    cx = cx,
                    cy = cy,
                )
            )
            if (i < cutCount) {
                val probe = PosterElement(
                    id = "probe", kind = ElementKind.CUTOUT, refIndex = i, cx = 0f, cy = 0f
                )
                val (cutW, cutH) = baseSize(probe, 1f, canvasH)
                // Dasar cutout rata dengan tepi bawah kartu, kepala menonjol keluar dari atas kartu
                val cardBottom = cy * canvasH + cardH / 2f
                val cutCenterY = (cardBottom - cutH / 2f) / canvasH
                val lo = cutW / 2f
                val hi = 1f - cutW / 2f
                val rawX = cx + 0.118f
                val cutCenterX = if (lo <= hi) rawX.coerceIn(lo, hi) else 0.5f
                list.add(
                    PosterElement(
                        id = UUID.randomUUID().toString(),
                        kind = ElementKind.CUTOUT,
                        refIndex = i,
                        cx = cutCenterX,
                        cy = cutCenterY,
                    )
                )
            }
        }
        elements = list
        selectedId = null
        undoStack.clear()
        redoStack.clear()
        historyVersion++
    }

    /**
     * Mode 2: foto potrait penuh sebagai background, 4 kartu vertikal miring perspektif 3D menghadap
     * ke subjek di tengah, dan cutout subjek berdiri di depan kartu (100% mirip referensi Mode 2).
     */
    private fun buildPhoto3dLayout() {
        aspect = PosterAspect.STORY
        val list = ArrayList<PosterElement>()
        val hasPhoto = readyPhotos.firstOrNull()?.original != null
        if (hasPhoto) {
            list.add(
                PosterElement(
                    id = UUID.randomUUID().toString(),
                    kind = ElementKind.BACKGROUND,
                    refIndex = 0,
                    cx = 0.5f,
                    cy = 0.5f,
                    group = "photo",
                    fill = true,
                )
            )
        }
        // [cx, cy, rotationZ, rotY (perspektif kiri/kanan menghadap tengah), rotX, sparkleStyle]
        val slots = listOf(
            floatArrayOf(0.21f, 0.24f, -5f, 16f, -3f, 0f),   // Kiri-atas (Night Changes)
            floatArrayOf(0.77f, 0.25f, 5f, -16f, -3f, 2f),   // Kanan-atas (Rewrite The Stars)
            floatArrayOf(0.205f, 0.70f, 4f, 18f, 4f, 1f),    // Kiri-bawah (Rude)
            floatArrayOf(0.82f, 0.66f, -4f, -18f, 4f, 0f),   // Kanan-bawah (Mr. Loverman)
        )
        cards.forEachIndexed { i, _ ->
            val s = slots[i % slots.size]
            list.add(
                PosterElement(
                    id = UUID.randomUUID().toString(),
                    kind = ElementKind.CARD,
                    refIndex = i,
                    cx = s[0],
                    cy = s[1],
                    rotation = s[2],
                    rotY = s[3],
                    rotX = s[4],
                    sparkleStyle = s[5].toInt(),
                )
            )
        }
        if (readyPhotos.firstOrNull()?.finalBitmap != null) {
            list.add(
                PosterElement(
                    id = UUID.randomUUID().toString(),
                    kind = ElementKind.CUTOUT,
                    refIndex = 0,
                    cx = 0.5f,
                    cy = 0.5f,
                    group = "photo",
                    fill = true,
                )
            )
        }
        elements = list
        selectedId = null
        undoStack.clear()
        redoStack.clear()
        historyVersion++
    }

    fun resetLayout() {
        val keep = undoStack.toList()
        val before = elements
        buildDefaultLayout()
        undoStack.addAll(keep)
        undoStack.add(before)
        historyVersion++
    }

    fun changeAspect(value: PosterAspect) {
        if (mode == PosterMode.PHOTO3D) {
            aspect = PosterAspect.STORY
            return
        }
        aspect = value
    }

    private fun targetIds(): Set<String> {
        val sel = selected ?: return emptySet()
        val g = sel.group
        return if (g == null) setOf(sel.id) else elements.filter { it.group == g }.map { it.id }.toSet()
    }

    fun transformSelected(dxFrac: Float, dyFrac: Float, zoom: Float, rotationDeg: Float) {
        val ids = targetIds()
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastTransformMs > 500L) pushHistory()
        lastTransformMs = now
        elements = elements.map {
            if (it.id !in ids) it else it.copy(
                cx = it.cx + dxFrac,
                cy = it.cy + dyFrac,
                scale = (it.scale * zoom).coerceIn(0.15f, 5f),
                rotation = it.rotation + rotationDeg,
            )
        }
    }

    fun setSelectedRotation(deg: Float) = editSelectedDebounced { it.copy(rotation = deg) }
    fun setSelectedScale(s: Float) = editSelectedDebounced { it.copy(scale = s.coerceIn(0.15f, 5f)) }
    fun setSelectedTiltX(deg: Float) = editSelectedDebounced(single = true) { it.copy(rotX = deg.coerceIn(-70f, 70f)) }
    fun setSelectedTiltY(deg: Float) = editSelectedDebounced(single = true) { it.copy(rotY = deg.coerceIn(-70f, 70f)) }
    fun flipSelected() = editSelected { it.copy(flipX = !it.flipX) }

    /** Untuk slider: riwayat undo hanya dicatat sekali per gerakan, bukan per tick. */
    private fun editSelectedDebounced(single: Boolean = false, block: (PosterElement) -> PosterElement) {
        val ids = if (single) setOfNotNull(selectedId) else targetIds()
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastTransformMs > 500L) pushHistory()
        lastTransformMs = now
        elements = elements.map { if (it.id in ids) block(it) else it }
    }

    private fun editSelected(block: (PosterElement) -> PosterElement) {
        val ids = targetIds()
        if (ids.isEmpty()) return
        pushHistory()
        elements = elements.map { if (it.id in ids) block(it) else it }
    }

    fun bringForward() = moveLayer(+1)
    fun sendBackward() = moveLayer(-1)

    private fun moveLayer(delta: Int) {
        val id = selectedId ?: return
        val idx = elements.indexOfFirst { it.id == id }
        val target = idx + delta
        if (idx < 0 || target !in elements.indices) return
        pushHistory()
        val mutable = elements.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(target, item)
        elements = mutable
    }

    fun deleteSelected() {
        val ids = targetIds()
        if (ids.isEmpty()) return
        pushHistory()
        elements = elements.filter { it.id !in ids }
        selectedId = null
    }

    /**
     * Cari elemen yang disentuh di posisi ([px],[py]) pada kanvas berukuran [cw] x [ch] piksel.
     * Area transparan cutout diabaikan agar elemen di belakangnya bisa dipilih.
     */
    fun hitTest(px: Float, py: Float, cw: Float, ch: Float): String? {
        for (el in elements.asReversed()) {
            val (bw, bh) = baseSize(el, cw, ch)
            val dx = px - el.cx * cw
            val dy = py - el.cy * ch
            val rad = Math.toRadians(el.rotation.toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            var lx = (dx * c + dy * s) / el.scale
            val ly = (-dx * s + dy * c) / el.scale
            if (el.flipX) lx = -lx
            if (lx < -bw / 2f || lx > bw / 2f || ly < -bh / 2f || ly > bh / 2f) continue
            if (el.kind == ElementKind.CUTOUT) {
                val bmp = cutoutBitmap(el.refIndex) ?: continue
                val bx: Int
                val by: Int
                if (el.fill) {
                    val sc = max(bw / bmp.width, bh / bmp.height)
                    bx = (((lx + bw / 2f) - (bw - bmp.width * sc) / 2f) / sc).toInt().coerceIn(0, bmp.width - 1)
                    by = (((ly + bh / 2f) - (bh - bmp.height * sc) / 2f) / sc).toInt().coerceIn(0, bmp.height - 1)
                } else {
                    bx = ((lx + bw / 2f) / bw * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                    by = ((ly + bh / 2f) / bh * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                }
                val alpha = (bmp.getPixel(bx, by) ushr 24) and 255
                if (alpha < 16) continue
            }
            return el.id
        }
        return null
    }

    /** Ukuran dasar elemen (sebelum scale) dalam piksel kanvas. */
    fun baseSize(el: PosterElement, cw: Float, ch: Float): Pair<Float, Float> = when (el.kind) {
        ElementKind.CARD -> {
            if (mode == PosterMode.PHOTO3D) {
                val w = cw * PosterMetrics.V_CARD_W
                w to w * PosterMetrics.V_CARD_ASPECT
            } else {
                val w = cw * PosterMetrics.CARD_W
                w to w * PosterMetrics.CARD_ASPECT
            }
        }
        ElementKind.CUTOUT -> if (el.fill) {
            cw to ch
        } else {
            var h = cw * PosterMetrics.CUTOUT_H
            var w = h / cutoutAspect(el.refIndex)
            val maxW = cw * 0.72f
            if (w > maxW) {
                val f = maxW / w
                w *= f
                h *= f
            }
            w to h
        }
        ElementKind.BACKGROUND -> if (el.fill) cw to ch else cw to (ch / el.bgCount.coerceAtLeast(1))
        ElementKind.STICKER -> {
            val sz = cw * 0.28f
            sz to sz
        }
    }

    fun addSticker(index: Int) {
        recordHistory()
        val el = PosterElement(
            id = UUID.randomUUID().toString(),
            kind = ElementKind.STICKER,
            refIndex = index.coerceIn(0, STAR_STICKER_RES.lastIndex),
            cx = 0.5f,
            cy = 0.5f,
            scale = 1f,
        )
        elements = elements + el
        selectedId = el.id
        historyVersion++
    }

    // ------------------------------------------------------------------ export
    fun save(bitmap: Bitmap, onDone: (PosterSaveResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { writeToGallery(bitmap) }
            onDone(result)
        }
    }

    fun share(bitmap: Bitmap, onReady: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val dir = File(context.cacheDir, "images").apply { mkdirs() }
                    val file = File(dir, "poster_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
                } catch (_: Exception) {
                    null
                }
            }
            onReady(uri)
        }
    }

    private fun writeToGallery(bitmap: Bitmap): PosterSaveResult {
        val name = "Beatles_Poster_${System.currentTimeMillis()}"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Beatles")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return PosterSaveResult.Failed("Gagal membuat file di galeri")
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        resolver.delete(uri, null, null)
                        return PosterSaveResult.Failed("Gagal menulis gambar")
                    }
                } ?: return PosterSaveResult.Failed("Gagal membuka file tujuan")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                PosterSaveResult.Success("Pictures/Beatles")
            } else {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) return PosterSaveResult.NeedsPermission
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Beatles"
                ).apply { mkdirs() }
                val file = File(dir, "$name.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf("image/png"), null
                )
                PosterSaveResult.Success("Pictures/Beatles")
            }
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("ENOSPC", ignoreCase = true) || msg.contains("space", ignoreCase = true)) {
                PosterSaveResult.Failed("Penyimpanan penuh")
            } else {
                PosterSaveResult.Failed(msg.ifBlank { "Gagal menyimpan poster" })
            }
        }
    }

    override fun onCleared() {
        remover.close()
        super.onCleared()
    }
}
