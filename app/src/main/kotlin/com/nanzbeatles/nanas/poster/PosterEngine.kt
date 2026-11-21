/**
 * NanzBeatles Project (C) 2026
 *
 * Mesin penghapus background untuk fitur Poster Musik Favorit.
 *
 * Alur: decode foto (EXIF + sampling) -> segmentasi (ML Kit, offline) -> penghalusan tepi
 * (guided filter berbasis foto asli) -> bersihkan noise -> defringe warna tepi -> koreksi manual (kuas).
 */

package com.nanzbeatles.nanas.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
// Decode foto
// ---------------------------------------------------------------------------------------------

object PosterImageLoader {
    /** Decode foto dari Uri, hormati orientasi EXIF, dan batasi sisi terpanjang ke [maxSide]. */
    suspend fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "poster_src_${System.nanoTime()}.bin")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Foto tidak bisa dibuka")
            input.use { ins -> tmp.outputStream().use { out -> ins.copyTo(out) } }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tmp.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IllegalStateException("Format foto tidak didukung")
            }

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bmp = BitmapFactory.decodeFile(tmp.absolutePath, opts)
                ?: throw IllegalStateException("Foto gagal dibaca (memori kurang?)")

            val longSide = max(bmp.width, bmp.height)
            if (longSide > maxSide) {
                val s = maxSide.toFloat() / longSide
                val scaled = Bitmap.createScaledBitmap(
                    bmp,
                    max(1, (bmp.width * s).toInt()),
                    max(1, (bmp.height * s).toInt()),
                    true
                )
                if (scaled !== bmp) bmp.recycle()
                bmp = scaled
            }

            val orientation = try {
                ExifInterface(tmp.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(-90f); matrix.postScale(-1f, 1f) }
                else -> Unit
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                if (rotated !== bmp) bmp.recycle()
                bmp = rotated
            }
            if (bmp.config != Bitmap.Config.ARGB_8888) {
                val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                if (copy != null) {
                    bmp.recycle()
                    bmp = copy
                }
            }
            bmp
        } finally {
            tmp.delete()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Segmentasi
// ---------------------------------------------------------------------------------------------

/** Engine segmentasi bisa diganti (mis. model matting ONNX/TFLite) selama menghasilkan confidence 0..1. */
interface BackgroundRemover {
    /** Mengembalikan confidence "orang" per piksel, ukuran sama dengan [bitmap] (w*h). */
    suspend fun confidence(bitmap: Bitmap): FloatArray
    fun close() {}
}

/** ML Kit Selfie Segmentation: model dibundel di APK, jalan offline. */
class MlKitSelfieRemover : BackgroundRemover {
    private val segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }

    override suspend fun confidence(bitmap: Bitmap): FloatArray {
        val mask = suspendCancellableCoroutine<SegmentationMask> { cont ->
            segmenter.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return withContext(Dispatchers.Default) {
            val mw = mask.width
            val mh = mask.height
            val buf = mask.buffer
            val conf = FloatArray(mw * mh)
            for (i in conf.indices) conf[i] = buf.getFloat(i * 4)
            if (mw == bitmap.width && mh == bitmap.height) conf
            else MaskRefiner.resample(conf, mw, mh, bitmap.width, bitmap.height)
        }
    }

    override fun close() {
        runCatching { segmenter.close() }
    }
}

// ---------------------------------------------------------------------------------------------
// Penghalusan mask
// ---------------------------------------------------------------------------------------------

object MaskRefiner {

    fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        if (e1 <= e0) return if (x >= e0) 1f else 0f
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Resample bilinear untuk array float 2D. */
    fun resample(src: FloatArray, sw: Int, sh: Int, dw: Int, dh: Int): FloatArray {
        val out = FloatArray(dw * dh)
        val xr = if (dw > 1) (sw - 1).toFloat() / (dw - 1) else 0f
        val yr = if (dh > 1) (sh - 1).toFloat() / (dh - 1) else 0f
        for (y in 0 until dh) {
            val fy = y * yr
            val y0 = fy.toInt()
            val y1 = min(y0 + 1, sh - 1)
            val wy = fy - y0
            for (x in 0 until dw) {
                val fx = x * xr
                val x0 = fx.toInt()
                val x1 = min(x0 + 1, sw - 1)
                val wx = fx - x0
                val a = src[y0 * sw + x0] * (1 - wx) + src[y0 * sw + x1] * wx
                val b = src[y1 * sw + x0] * (1 - wx) + src[y1 * sw + x1] * wx
                out[y * dw + x] = a * (1 - wy) + b * wy
            }
        }
        return out
    }

    /** Box filter (rata-rata jendela (2r+1)^2, tepi dinormalisasi), O(n). */
    fun boxFilter(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val tmp = FloatArray(w * h)
        // horizontal
        for (y in 0 until h) {
            val row = y * w
            var sum = 0f
            for (x in 0..min(r, w - 1)) sum += src[row + x]
            for (x in 0 until w) {
                if (x > 0) {
                    val add = x + r
                    val sub = x - r - 1
                    if (add < w) sum += src[row + add]
                    if (sub >= 0) sum -= src[row + sub]
                }
                val cnt = min(w - 1, x + r) - max(0, x - r) + 1
                tmp[row + x] = sum / cnt
            }
        }
        // vertikal (cache-friendly: jumlahkan per baris)
        val out = FloatArray(w * h)
        val col = FloatArray(w)
        for (y in 0..min(r, h - 1)) {
            val row = y * w
            for (x in 0 until w) col[x] += tmp[row + x]
        }
        for (y in 0 until h) {
            if (y > 0) {
                val add = y + r
                val sub = y - r - 1
                if (add < h) {
                    val row = add * w
                    for (x in 0 until w) col[x] += tmp[row + x]
                }
                if (sub >= 0) {
                    val row = sub * w
                    for (x in 0 until w) col[x] -= tmp[row + x]
                }
            }
            val cnt = min(h - 1, y + r) - max(0, y - r) + 1
            val inv = 1f / cnt
            val row = y * w
            for (x in 0 until w) out[row + x] = col[x] * inv
        }
        return out
    }

    /** Guided filter (He et al.): membuat tepi mask mengikuti tepi pada foto asli. */
    private fun guidedFilter(guide: FloatArray, p: FloatArray, w: Int, h: Int, r: Int, eps: Float): FloatArray {
        val n = w * h
        val meanI = boxFilter(guide, w, h, r)
        val meanP = boxFilter(p, w, h, r)
        val ii = FloatArray(n) { guide[it] * guide[it] }
        val ip = FloatArray(n) { guide[it] * p[it] }
        val corrI = boxFilter(ii, w, h, r)
        val corrIp = boxFilter(ip, w, h, r)
        val a = FloatArray(n)
        val b = FloatArray(n)
        for (i in 0 until n) {
            val varI = corrI[i] - meanI[i] * meanI[i]
            val covIp = corrIp[i] - meanI[i] * meanP[i]
            val ai = covIp / (varI + eps)
            a[i] = ai
            b[i] = meanP[i] - ai * meanI[i]
        }
        val meanA = boxFilter(a, w, h, r)
        val meanB = boxFilter(b, w, h, r)
        return FloatArray(n) { (meanA[it] * guide[it] + meanB[it]).coerceIn(0f, 1f) }
    }

    private class Components(val labels: IntArray, val sizes: IntArray, val touchesBorder: BooleanArray)

    private fun label(binary: BooleanArray, w: Int, h: Int): Components {
        val n = w * h
        val labels = IntArray(n)
        val queue = IntArray(n)
        val sizes = ArrayList<Int>().apply { add(0) }
        val border = ArrayList<Boolean>().apply { add(false) }
        var next = 0
        for (start in 0 until n) {
            if (!binary[start] || labels[start] != 0) continue
            next++
            var head = 0
            var tail = 0
            queue[tail++] = start
            labels[start] = next
            var size = 0
            var touches = false
            while (head < tail) {
                val idx = queue[head++]
                size++
                val x = idx % w
                val y = idx / w
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) touches = true
                if (x > 0) {
                    val j = idx - 1
                    if (binary[j] && labels[j] == 0) { labels[j] = next; queue[tail++] = j }
                }
                if (x < w - 1) {
                    val j = idx + 1
                    if (binary[j] && labels[j] == 0) { labels[j] = next; queue[tail++] = j }
                }
                if (y > 0) {
                    val j = idx - w
                    if (binary[j] && labels[j] == 0) { labels[j] = next; queue[tail++] = j }
                }
                if (y < h - 1) {
                    val j = idx + w
                    if (binary[j] && labels[j] == 0) { labels[j] = next; queue[tail++] = j }
                }
            }
            sizes.add(size)
            border.add(touches)
        }
        return Components(labels, sizes.toIntArray(), border.toBooleanArray())
    }

    /** Buang pulau-pulau kecil di background dan tutup lubang kecil di dalam subjek. */
    private fun cleanup(alpha: FloatArray, w: Int, h: Int) {
        val n = w * h
        val fg = BooleanArray(n) { alpha[it] > 0.5f }
        val fgComp = label(fg, w, h)
        var largest = 0
        for (i in 1 until fgComp.sizes.size) largest = max(largest, fgComp.sizes[i])
        if (largest == 0) return

        val keep = BooleanArray(fgComp.sizes.size)
        for (i in 1 until fgComp.sizes.size) keep[i] = fgComp.sizes[i] >= largest * 0.04f

        val kept = FloatArray(n) { if (keep[fgComp.labels[it]] && fgComp.labels[it] != 0) 1f else 0f }
        val near = boxFilter(kept, w, h, 7)
        for (i in 0 until n) {
            if (near[i] <= 0.0001f) alpha[i] = 0f
        }

        // lubang kecil (background yang terkurung subjek)
        val bg = BooleanArray(n) { alpha[it] <= 0.5f }
        val bgComp = label(bg, w, h)
        val maxHole = (n * 0.0008f).toInt()
        for (i in 0 until n) {
            val l = bgComp.labels[i]
            if (l != 0 && !bgComp.touchesBorder[l] && bgComp.sizes[l] <= maxHole) alpha[i] = 1f
        }
    }

    /** Hasil: alpha dasar 0..1 (belum dikurva), ukuran w*h. */
    fun refine(pixels: IntArray, confidence: FloatArray, w: Int, h: Int): FloatArray {
        val n = w * h
        val p = FloatArray(n) { smoothstep(0.15f, 0.85f, confidence[it]) }
        val gray = FloatArray(n) {
            val c = pixels[it]
            (0.299f * ((c shr 16) and 255) + 0.587f * ((c shr 8) and 255) + 0.114f * (c and 255)) / 255f
        }
        val r = (max(w, h) / 180).coerceIn(4, 10)
        val q = guidedFilter(gray, p, w, h, r, 1e-3f)
        cleanup(q, w, h)
        return q
    }

    /** Netralkan warna background yang "bocor" di piksel tepi semi-transparan. */
    fun defringe(pixels: IntArray, alpha: FloatArray, w: Int, h: Int): IntArray {
        val n = w * h
        val solid = FloatArray(n) { if (alpha[it] >= 0.97f) 1f else 0f }
        val rad = 3
        val sumW = boxFilter(solid, w, h, rad)
        fun channel(shift: Int): FloatArray {
            val c = FloatArray(n) { solid[it] * ((pixels[it] shr shift) and 255) }
            return boxFilter(c, w, h, rad)
        }
        val sr = channel(16)
        val sg = channel(8)
        val sb = channel(0)
        val out = pixels.copyOf()
        for (i in 0 until n) {
            val a = alpha[i]
            if (a > 0.02f && a < 0.97f && sumW[i] > 0.02f) {
                val t = (1f - a) * 0.85f
                val c = pixels[i]
                val r0 = (c shr 16) and 255
                val g0 = (c shr 8) and 255
                val b0 = c and 255
                val nr = (r0 + (sr[i] / sumW[i] - r0) * t).toInt().coerceIn(0, 255)
                val ng = (g0 + (sg[i] / sumW[i] - g0) * t).toInt().coerceIn(0, 255)
                val nb = (b0 + (sb[i] / sumW[i] - b0) * t).toInt().coerceIn(0, 255)
                out[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return out
    }
}

// ---------------------------------------------------------------------------------------------
// Editor cutout (kuas hapus/pulihkan, undo/redo, slider tepi)
// ---------------------------------------------------------------------------------------------

class CutoutEditor private constructor(
    val width: Int,
    val height: Int,
    private val src: IntArray,
    private val base: FloatArray,
) {
    /** Bitmap hasil (ARGB, transparan di background). Diubah in-place saat mengedit. */
    val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    var edgeShift = 0f
        private set
    var softness = 0.25f
        private set

    private val n = width * height
    private val manualVal = ByteArray(n)
    private val manualW = ByteArray(n)

    private class Snapshot(val v: ByteArray, val w: ByteArray)

    private val undoStack = ArrayList<Snapshot>()
    private val redoStack = ArrayList<Snapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    init {
        rebuildAll()
    }

    private fun curve(b: Float): Float {
        val c = 0.5f - edgeShift
        val hw = 0.08f + 0.4f * softness
        return MaskRefiner.smoothstep(c - hw, c + hw, b)
    }

    private fun alphaAt(i: Int): Float {
        val a = curve(base[i])
        val w = (manualW[i].toInt() and 255) / 255f
        if (w == 0f) return a
        val v = (manualVal[i].toInt() and 255) / 255f
        return a * (1 - w) + v * w
    }

    private fun argbAt(i: Int): Int {
        val a8 = (alphaAt(i) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (a8 shl 24) or (src[i] and 0xFFFFFF)
    }

    fun rebuildAll() {
        val out = IntArray(n)
        for (i in 0 until n) out[i] = argbAt(i)
        bitmap.setPixels(out, 0, width, 0, 0, width, height)
    }

    fun setEdge(shift: Float, soft: Float) {
        edgeShift = shift.coerceIn(-0.3f, 0.3f)
        softness = soft.coerceIn(0f, 1f)
        rebuildAll()
    }

    private fun snapshot() = Snapshot(manualVal.copyOf(), manualW.copyOf())

    /** Panggil di awal tiap goresan supaya bisa di-undo. */
    fun beginStroke() {
        undoStack.add(snapshot())
        if (undoStack.size > 8) undoStack.removeAt(0)
        redoStack.clear()
    }

    /** Goresan kuas. [erase]=true menghapus, false memulihkan. Koordinat dalam piksel gambar. */
    fun brush(cx: Float, cy: Float, radius: Float, hardness: Float, erase: Boolean) {
        val x0 = max(0, floor(cx - radius).toInt())
        val x1 = min(width - 1, ceil(cx + radius).toInt())
        val y0 = max(0, floor(cy - radius).toInt())
        val y1 = min(height - 1, ceil(cy + radius).toInt())
        if (x1 < x0 || y1 < y0) return
        val inner = radius * hardness.coerceIn(0f, 0.95f)
        val target = if (erase) 0f else 1f
        val rw = x1 - x0 + 1
        val rh = y1 - y0 + 1
        val buf = IntArray(rw * rh)
        for (y in y0..y1) {
            for (x in x0..x1) {
                val i = y * width + x
                val d = hypot(x - cx, y - cy)
                if (d <= radius) {
                    val s = if (d <= inner) 1f else 1f - (d - inner) / (radius - inner + 1e-3f)
                    if (s > 0f) {
                        val cur = alphaAt(i)
                        val nv = cur * (1 - s) + target * s
                        manualVal[i] = (nv * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                        manualW[i] = 255.toByte()
                    }
                }
                buf[(y - y0) * rw + (x - x0)] = argbAt(i)
            }
        }
        bitmap.setPixels(buf, 0, rw, x0, y0, rw, rh)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(snapshot())
        val s = undoStack.removeAt(undoStack.size - 1)
        s.v.copyInto(manualVal)
        s.w.copyInto(manualW)
        rebuildAll()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(snapshot())
        val s = redoStack.removeAt(redoStack.size - 1)
        s.v.copyInto(manualVal)
        s.w.copyInto(manualW)
        rebuildAll()
    }

    /** Buang semua koreksi manual dan kembalikan slider ke default. */
    fun resetToAuto() {
        beginStroke()
        manualVal.fill(0)
        manualW.fill(0)
        edgeShift = 0f
        softness = 0.25f
        rebuildAll()
    }

    /** Hasil akhir yang dipangkas ke area subjek (alpha > ~2%). */
    fun finalCropped(): Bitmap {
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (alphaAt(y * width + x) > 0.02f) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val pad = 2
        val l = max(0, minX - pad)
        val t = max(0, minY - pad)
        val r = min(width - 1, maxX + pad)
        val b = min(height - 1, maxY + pad)
        return Bitmap.createBitmap(bitmap, l, t, r - l + 1, b - t + 1)
    }

    companion object {
        /** Proses lengkap di background thread: segmentasi + penghalusan + defringe. */
        suspend fun create(original: Bitmap, remover: BackgroundRemover): CutoutEditor {
            val conf = remover.confidence(original)
            return withContext(Dispatchers.Default) {
                val w = original.width
                val h = original.height
                val pixels = IntArray(w * h)
                original.getPixels(pixels, 0, w, 0, 0, w, h)
                val base = MaskRefiner.refine(pixels, conf, w, h)
                val clean = MaskRefiner.defringe(pixels, base, w, h)
                CutoutEditor(w, h, clean, base)
            }
        }
    }
}
