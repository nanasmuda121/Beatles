/**
 * NanzBeatles Project (C) 2026
 *
 * Layar Poster Musik Favorit (Mode 1): pilih mode -> lagu -> foto -> hapus background/koreksi
 * -> background -> editor bebas -> simpan/bagikan.
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.nanzbeatles.nanas.poster

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.R
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private fun stepTitle(step: PosterStep) = when (step) {
    PosterStep.MODE -> "Poster Musik Favorit"
    PosterStep.SONGS -> "Pilih lagu"
    PosterStep.PHOTOS -> "Foto kamu"
    PosterStep.CUTOUT -> "Hapus background"
    PosterStep.BACKGROUND -> "Background"
    PosterStep.EDITOR -> "Edit poster"
}

@Composable
fun PosterScreen(
    navController: NavController,
    viewModel: PosterViewModel = hiltViewModel(),
) {
    val vm = viewModel
    BackHandler {
        if (!vm.back()) navController.navigateUp()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stepTitle(vm.step)) },
            navigationIcon = {
                IconButton(onClick = { if (!vm.back()) navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = "Kembali")
                }
            },
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
        ) {
            when (vm.step) {
                PosterStep.MODE -> ModeStep(vm)
                PosterStep.SONGS -> SongsStep(vm)
                PosterStep.PHOTOS -> PhotosStep(vm)
                PosterStep.CUTOUT -> CutoutStep(vm)
                PosterStep.BACKGROUND -> BackgroundStep(vm)
                PosterStep.EDITOR -> EditorStep(vm)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 1. Pilih mode
// ---------------------------------------------------------------------------------------------

@Composable
private fun ModeStep(vm: PosterViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Buat poster lagu favoritmu, lalu simpan atau bagikan sebagai gambar.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Card(
            onClick = { vm.selectMode(PosterMode.COLLAGE) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Mode 1: Kartu + Kolase", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "3 kartu pemutar musik bergaya zig-zag, foto kamu (background dihapus) menimpa kartu, " +
                        "di atas kolase foto buram. Semua elemen bebas diatur.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Card(
            onClick = { vm.selectMode(PosterMode.PHOTO3D) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Mode 2: Poster Foto 3D", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Fotomu jadi background penuh, 1 sampai 4 kartu lagu vertikal melayang miring 3D di sekelilingmu. " +
                        "Kemiringan kiri/kanan, atas/bawah, dan sudut tiap kartu bisa diatur.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 2. Pilih lagu
// ---------------------------------------------------------------------------------------------

@Composable
private fun SongsStep(vm: PosterViewModel) {
    val songs by vm.allSongs.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(songs, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) songs
        else songs.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Cari lagu atau artis") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (vm.minSongs == vm.maxSongs) "Pilih ${vm.maxSongs} lagu berbeda (${vm.selectedSongs.size}/${vm.maxSongs})"
            else "Pilih ${vm.minSongs} sampai ${vm.maxSongs} lagu (${vm.selectedSongs.size}/${vm.maxSongs})",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (songs.isEmpty()) {
            Text(
                "Belum ada lagu di library. Putar atau simpan beberapa lagu dulu, lalu kembali ke sini.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { song ->
                val checked = vm.selectedSongs.any { it.id == song.id }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.toggleSong(song) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(
                            song.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Checkbox(checked = checked, onCheckedChange = { vm.toggleSong(song) })
                }
            }
        }
        Button(
            onClick = { vm.confirmSongs() },
            enabled = vm.selectedSongs.size in vm.minSongs..vm.maxSongs && !vm.cardsLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            if (vm.cardsLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Lanjut")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 3. Foto orang
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotosStep(vm: PosterViewModel) {
    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PosterMetrics.MAX_PHOTOS)
    ) { uris -> vm.addPhotos(uris) }
    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.addPhotos(listOf(uri)) }
    val single = vm.maxPhotos == 1
    fun launchPicker() {
        val req = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (single) singlePicker.launch(req) else multiPicker.launch(req)
    }

    val loading = vm.photos.any { it.status == SlotStatus.LOADING }
    val ready = vm.readyPhotos.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (single) "Pilih 1 foto utama. Foto ini jadi background penuh, dan dirimu dipisahkan dari background " +
                "supaya bisa berdiri di depan kartu lagu. Pilih foto dengan orang yang terlihat jelas."
            else "Tambahkan minimal 1 foto (maksimal ${PosterMetrics.MAX_PHOTOS}). " +
                "Foto dengan orang yang terlihat jelas dan pencahayaan cukup memberi hasil hapus background terbaik.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.photos.forEach { slot ->
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = slot.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    when (slot.status) {
                        SlotStatus.LOADING -> Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = Color.White) }
                        SlotStatus.ERROR -> Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xCC000000))
                                .clickable { vm.retry(slot) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Coba lagi", tint = Color.White)
                        }
                        SlotStatus.DONE -> Unit
                    }
                    IconButton(
                        onClick = { vm.removePhoto(slot) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = Color.White)
                    }
                }
            }
            if (vm.photos.size < vm.maxPhotos) {
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                        .clickable { launchPicker() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Tambah foto")
                }
            }
        }
        vm.photos.firstOrNull { it.error != null }?.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (loading) {
            Text("Memproses foto... ini bisa beberapa detik.", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                vm.currentPhoto = 0
                vm.goTo(PosterStep.CUTOUT)
            },
            enabled = ready >= 1 && !loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Lanjut") }
    }
}

// ---------------------------------------------------------------------------------------------
// 4. Hapus background + koreksi manual
// ---------------------------------------------------------------------------------------------

@Composable
private fun CutoutStep(vm: PosterViewModel) {
    val ready = vm.readyPhotos
    if (ready.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Belum ada foto yang siap. Kembali dan tambahkan foto.")
        }
        return
    }
    val index = vm.currentPhoto.coerceIn(0, ready.lastIndex)
    val slot = ready[index]
    val editor = slot.editor

    var erase by remember(slot.id) { mutableStateOf(true) }
    var brushDp by remember(slot.id) { mutableFloatStateOf(28f) }
    var hardness by remember(slot.id) { mutableFloatStateOf(0.7f) }
    var edge by remember(slot.id) { mutableFloatStateOf(editor?.edgeShift ?: 0f) }
    var soft by remember(slot.id) { mutableFloatStateOf(editor?.softness ?: 0.25f) }
    var revision by remember(slot.id) { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        if (ready.size > 1) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ready.forEachIndexed { i, _ ->
                    FilterChip(
                        selected = i == index,
                        onClick = { vm.currentPhoto = i },
                        label = { Text("Foto ${i + 1}") },
                    )
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            val original = slot.original
            if (original != null) {
                CutoutBrushCanvas(
                    editor = editor,
                    original = original,
                    showOriginal = slot.keepOriginal || editor == null,
                    erase = erase,
                    brushDp = brushDp,
                    hardness = hardness,
                    revision = revision,
                    onChanged = { revision++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            Modifier
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState())
                .height(200.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = slot.keepOriginal,
                    onCheckedChange = { if (editor != null) slot.keepOriginal = it },
                    enabled = editor != null,
                )
                Spacer(Modifier.width(8.dp))
                Text("Pakai foto asli (tanpa hapus background)", style = MaterialTheme.typography.bodyMedium)
            }
            slot.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (editor != null && !slot.keepOriginal) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = erase, onClick = { erase = true }, label = { Text("Hapus") })
                    FilterChip(selected = !erase, onClick = { erase = false }, label = { Text("Pulihkan") })
                    IconButton(
                        onClick = { editor.undo(); revision++ },
                        enabled = revision >= 0 && editor.canUndo,
                    ) { Icon(Icons.Rounded.Undo, contentDescription = "Undo") }
                    IconButton(
                        onClick = { editor.redo(); revision++ },
                        enabled = revision >= 0 && editor.canRedo,
                    ) { Icon(Icons.Rounded.Redo, contentDescription = "Redo") }
                    IconButton(
                        onClick = {
                            editor.resetToAuto()
                            edge = 0f
                            soft = 0.25f
                            revision++
                        }
                    ) { Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset ke hasil otomatis") }
                }
                SliderRow("Ukuran kuas", brushDp, 8f..80f) { brushDp = it }
                SliderRow("Kekerasan kuas", hardness, 0f..1f) { hardness = it }
                SliderRow(
                    label = "Tepi (kecilkan / besarkan)",
                    value = edge,
                    range = -0.3f..0.3f,
                    onChange = { edge = it },
                    onFinished = { editor.setEdge(edge, soft); revision++ },
                )
                SliderRow(
                    label = "Perhalus tepi",
                    value = soft,
                    range = 0f..1f,
                    onChange = { soft = it },
                    onFinished = { editor.setEdge(edge, soft); revision++ },
                )
                Text(
                    "Satu jari: kuas. Dua jari: zoom dan geser gambar.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Button(
            onClick = { vm.finishCutouts() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) { Text("Lanjut") }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: (() -> Unit)? = null,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            onValueChangeFinished = onFinished,
        )
    }
}

/** Kanvas kuas: latar kotak-kotak, satu jari melukis, dua jari zoom/geser. */
@Composable
private fun CutoutBrushCanvas(
    editor: CutoutEditor?,
    original: Bitmap,
    showOriginal: Boolean,
    erase: Boolean,
    brushDp: Float,
    hardness: Float,
    revision: Int,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val brushPx = with(density) { brushDp.dp.toPx() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var cursor by remember { mutableStateOf<Offset?>(null) }
    val imgW = editor?.width ?: original.width
    val imgH = editor?.height ?: original.height
    val canPaint = editor != null && !showOriginal

    Canvas(
        modifier = modifier
            .pointerInput(editor, canPaint, erase, brushPx, hardness) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val viewW = size.width.toFloat()
                    val viewH = size.height.toFloat()
                    val fit = min(viewW / imgW, viewH / imgH)
                    val center = Offset(viewW / 2f, viewH / 2f)

                    fun toImage(p: Offset): Offset {
                        val s = fit * zoom
                        val tl = Offset(center.x - imgW * s / 2f + pan.x, center.y - imgH * s / 2f + pan.y)
                        return Offset((p.x - tl.x) / s, (p.y - tl.y) / s)
                    }

                    fun paintAt(p: Offset) {
                        val ed = editor ?: return
                        val ip = toImage(p)
                        val r = (brushPx / 2f) / (fit * zoom)
                        ed.brush(ip.x, ip.y, r, hardness, erase)
                    }

                    var multi = false
                    var started = false
                    var last = down.position
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            multi = true
                            cursor = null
                            val zc = event.calculateZoom()
                            val pc = event.calculatePan()
                            val cc = event.calculateCentroid()
                            val newZoom = (zoom * zc).coerceIn(1f, 8f)
                            val ratio = newZoom / zoom
                            val v = cc - center
                            pan = if (newZoom <= 1.01f) Offset.Zero
                            else Offset(
                                v.x * (1 - ratio) + pan.x * ratio + pc.x,
                                v.y * (1 - ratio) + pan.y * ratio + pc.y,
                            )
                            zoom = newZoom
                            event.changes.forEach { it.consume() }
                        } else if (!multi && pressed.size == 1 && canPaint) {
                            val c = pressed[0]
                            if (!started) {
                                editor?.beginStroke()
                                started = true
                                paintAt(last)
                            }
                            val rScreen = brushPx / 2f
                            val dist = hypot(c.position.x - last.x, c.position.y - last.y)
                            val step = max(1f, rScreen * 0.35f)
                            val n = max(1, ceil(dist / step).toInt())
                            for (k in 1..n) {
                                val t = k / n.toFloat()
                                paintAt(Offset(last.x + (c.position.x - last.x) * t, last.y + (c.position.y - last.y) * t))
                            }
                            last = c.position
                            cursor = c.position
                            onChanged()
                            c.consume()
                        }
                    } while (event.changes.any { it.pressed })
                    cursor = null
                    if (started) onChanged()
                }
            }
    ) {
        @Suppress("UNUSED_VARIABLE") val rev = revision
        val fit = min(size.width / imgW, size.height / imgH)
        val s = fit * zoom
        val dw = imgW * s
        val dh = imgH * s
        val tl = Offset(size.width / 2f - dw / 2f + pan.x, size.height / 2f - dh / 2f + pan.y)

        // latar kotak-kotak
        drawRect(Color(0xFFBDBDBD))
        val tile = 12.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) tile else 0f
            while (x < size.width) {
                drawRect(Color(0xFFE6E6E6), Offset(x, y), Size(tile, tile))
                x += tile * 2f
            }
            y += tile
            row++
        }

        val bmp = if (showOriginal || editor == null) original else editor.bitmap
        drawImage(
            image = bmp.asImageBitmap(),
            dstOffset = IntOffset(tl.x.toInt(), tl.y.toInt()),
            dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)),
        )

        cursor?.let {
            drawCircle(Color.White, radius = brushPx / 2f, center = it, style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color.Black, radius = brushPx / 2f + 2.dp.toPx(), center = it, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 5. Background
// ---------------------------------------------------------------------------------------------

@Composable
private fun BackgroundStep(vm: PosterViewModel) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PosterMetrics.MAX_BACKGROUNDS)
    ) { uris -> vm.addBackgrounds(uris) }
    var blur by remember { mutableFloatStateOf(vm.bgBlur) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = vm.bgMode == BackgroundMode.PHOTO,
                onClick = { vm.bgMode = BackgroundMode.PHOTO },
                label = { Text("Foto") },
            )
            FilterChip(
                selected = vm.bgMode == BackgroundMode.SOLID,
                onClick = { vm.bgMode = BackgroundMode.SOLID },
                label = { Text("Warna polos") },
            )
            FilterChip(
                selected = vm.bgMode == BackgroundMode.GRADIENT,
                onClick = { vm.bgMode = BackgroundMode.GRADIENT },
                label = { Text("Gradasi") },
            )
        }

        when (vm.bgMode) {
            BackgroundMode.PHOTO -> {
                Text(
                    "Pilih 1 sampai ${PosterMetrics.MAX_BACKGROUNDS} foto. Jika lebih dari satu, tampil sebagai " +
                        "kolase bertingkat dan tiap foto bisa digeser di editor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    vm.bgBlurred.forEachIndexed { i, bmp ->
                        Box(
                            Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            IconButton(
                                onClick = { vm.removeBackgroundPhoto(i) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp),
                            ) { Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = Color.White) }
                        }
                    }
                    if (vm.bgCount < PosterMetrics.MAX_BACKGROUNDS) {
                        Box(
                            Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                                .clickable {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Add, contentDescription = "Tambah background") }
                    }
                }
                Text("Tingkat blur", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = blur,
                    onValueChange = { blur = it },
                    onValueChangeFinished = { vm.applyBlur(blur) },
                )
                if (vm.bgLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Memproses background...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            BackgroundMode.SOLID -> Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SOLID_BG_PRESETS.forEach { c ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                if (vm.solidColor == c) 3.dp else 1.dp,
                                if (vm.solidColor == c) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            )
                            .clickable { vm.solidColor = c }
                    )
                }
            }
            BackgroundMode.GRADIENT -> Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GRADIENT_BG_PRESETS.forEachIndexed { i, (a, b) ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(a), Color(b))))
                            .border(
                                if (vm.gradientIndex == i) 3.dp else 1.dp,
                                if (vm.gradientIndex == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            )
                            .clickable { vm.gradientIndex = i }
                    )
                }
            }
        }

        Button(
            onClick = { vm.enterEditor() },
            enabled = vm.canContinueFromBackground() && !vm.bgLoading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Lanjut ke editor") }
    }
}

// ---------------------------------------------------------------------------------------------
// 6. Editor + export
// ---------------------------------------------------------------------------------------------

@Composable
private fun EditorStep(vm: PosterViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val bmp = pending
        pending = null
        if (granted && bmp != null) {
            vm.save(bmp) { result -> message = resultMessage(result); busy = false }
        } else {
            busy = false
            message = "Izin penyimpanan ditolak. Gunakan tombol Bagikan, atau izinkan penyimpanan di pengaturan."
        }
    }

    suspend fun capture(): Bitmap? = try {
        layer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    } catch (_: Exception) {
        null
    }

    fun doSave() {
        if (busy) return
        busy = true
        scope.launch {
            val bmp = capture()
            if (bmp == null) {
                busy = false
                message = "Gagal membuat gambar poster"
                return@launch
            }
            vm.save(bmp) { result ->
                if (result is PosterSaveResult.NeedsPermission) {
                    pending = bmp
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                } else {
                    message = resultMessage(result)
                    busy = false
                }
            }
        }
    }

    fun doShare() {
        if (busy) return
        busy = true
        scope.launch {
            val bmp = capture()
            if (bmp == null) {
                busy = false
                message = "Gagal membuat gambar poster"
                return@launch
            }
            vm.share(bmp) { uri ->
                busy = false
                if (uri == null) {
                    message = "Gagal menyiapkan gambar untuk dibagikan"
                } else {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Bagikan poster"))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val availW = maxWidth
            val availH = maxHeight
            val posterW = min(availW.value, availH.value * vm.aspect.ratio).dp
            PosterEditorSurface(
                vm = vm,
                captureLayer = layer,
                modifier = Modifier.width(posterW),
            )
        }
        EditorPanel(
            vm = vm,
            busy = busy,
            onSave = ::doSave,
            onShare = ::doShare,
        )
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            title = { Text("Poster") },
            text = { Text(message.orEmpty()) },
        )
    }
}

private fun resultMessage(result: PosterSaveResult): String = when (result) {
    is PosterSaveResult.Success -> "Poster tersimpan di ${result.label}"
    PosterSaveResult.NeedsPermission -> "Izin penyimpanan diperlukan untuk menyimpan poster"
    is PosterSaveResult.Failed -> "Gagal menyimpan: ${result.message}"
}

@Composable
private fun EditorPanel(
    vm: PosterViewModel,
    busy: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val sel = vm.selected
    Column(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { vm.undo() }, enabled = vm.canUndo) {
                Icon(Icons.Rounded.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = { vm.redo() }, enabled = vm.canRedo) {
                Icon(Icons.Rounded.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = { vm.resetLayout() }) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset tata letak")
            }
            PosterAspect.entries.forEach { a ->
                FilterChip(
                    selected = vm.aspect == a,
                    onClick = { vm.changeAspect(a) },
                    label = { Text(a.label) },
                )
            }
        }

        if (vm.mode == PosterMode.PHOTO3D) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = vm.sparkle, onCheckedChange = { vm.sparkle = it })
                Spacer(Modifier.width(8.dp))
                Text("Kilau di sudut kartu", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (sel == null) {
            Text(
                "Ketuk kartu, foto, atau background di poster untuk memilih. Satu jari menggeser, dua jari " +
                    "mencubit untuk zoom dan memutar.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            val kindLabel = when (sel.kind) {
                ElementKind.CARD -> "Kartu lagu"
                ElementKind.CUTOUT -> "Foto"
                ElementKind.BACKGROUND -> "Background"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(kindLabel, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.bringForward() }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Maju satu layer")
                }
                IconButton(onClick = { vm.sendBackward() }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Mundur satu layer")
                }
                if (sel.kind == ElementKind.CUTOUT) {
                    IconButton(onClick = { vm.flipSelected() }) {
                        Icon(Icons.Rounded.Flip, contentDescription = "Balik horizontal")
                    }
                }
                IconButton(onClick = { vm.deleteSelected() }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Hapus elemen")
                }
            }
            SliderRow("Ukuran", sel.scale, 0.15f..3f, onChange = { vm.setSelectedScale(it) })
            SliderRow("Sudut (putar)", sel.rotation.coerceIn(-180f, 180f), -180f..180f, onChange = { vm.setSelectedRotation(it) })
            if (sel.kind == ElementKind.CARD) {
                SliderRow("Miring kiri / kanan", sel.rotY.coerceIn(-70f, 70f), -70f..70f, onChange = { vm.setSelectedTiltY(it) })
                SliderRow("Miring atas / bawah", sel.rotX.coerceIn(-70f, 70f), -70f..70f, onChange = { vm.setSelectedTiltX(it) })
            }

            if (sel.kind == ElementKind.CARD) {
                val card = vm.cards.getOrNull(sel.refIndex)
                if (card != null) {
                    SliderRow("Posisi progress lagu", card.progress, 0f..1f, onChange = {
                        vm.updateCardProgress(sel.refIndex, it)
                    })
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = card.color == null,
                            onClick = { vm.updateCardColor(sel.refIndex, null) },
                            label = { Text("Otomatis") },
                        )
                        CARD_COLOR_PRESETS.forEach { c ->
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        if (card.color == c) 3.dp else 1.dp,
                                        if (card.color == c) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        CircleShape,
                                    )
                                    .clickable { vm.updateCardColor(sel.refIndex, c) }
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onShare, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Bagikan") }
            Button(onClick = onSave, enabled = !busy, modifier = Modifier.weight(1f)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Simpan")
            }
        }
    }
}
