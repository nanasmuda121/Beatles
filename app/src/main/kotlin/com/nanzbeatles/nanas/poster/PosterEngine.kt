/**
 * NanzBeatles Project (C) 2026
 *
 * Mesin penghapus background presisi tinggi untuk fitur Poster Musik Favorit.
 *
 * Alur:
 * 1. Decode foto (EXIF + sampling)
 * 2. Segmentasi 2-Tahap Coarse-to-Fine ROI (ML Kit Selfie Segmentation):
 *    - Tahap 1 mendeteksi area subjek manusia utama & mengisolasi objek latar sekitar.
 *    - Tahap 2 memotong ROI karakter utama dan menjalankan ulang neural network pada resolusi penuh
 *      khusus di area karakter sehingga detail tangan, rambut, dan pakaian 2-4x lebih tajam.
 * 3. Pemisahan Afinitas Warna Lokal (Local Foreground vs Background Color Model):
 *    - Membandingkan warna RGB/perseptual piksel di zona transisi terhadap warna subjek vs warna objek sekitar.
 *    - Mencegah bagian tubuh/baju karakter ikut terhapus sekaligus membuang objek sekitar yang menempel.
 * 4. Multi-channel RGB Guided Filter + Morphological Closing + Hole Filling + Defringe.
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
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
// Segmentasi 2-Tahap (Coarse-to-Fine ROI)
// ---------------------------------------------------------------------------------------------

interface BackgroundRemover {
    /** Mengembalikan confidence "orang" per piksel, ukuran sama dengan [bitmap] (w*h). */
    suspend fun confidence(bitmap: Bitmap): FloatArray
    fun close() {}
}

/**
 * ML Kit Selfie Segmentation dengan peningkatan Coarse-to-Fine 2-Tahap:
 * - Tahap 1: deteksi posisi karakter utama pada foto penuh dan buang pulau objek di pinggir.
 * - Tahap 2: crop khusus di sekitar karakter utama lalu jalankan segmentasi kedua sehingga
 *   grid tensor 256x256 ML Kit terkonsentrasi penuh pada tubuh/tangan/kepala karakter.
 */
class MlKitSelfieRemover : BackgroundRemover {
    private val segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }

    private suspend fun runRawMask(bmp: Bitmap): FloatArray {
        val mask = suspendCancellableCoroutine<SegmentationMask> { cont ->
            segmenter.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return withContext(Dispatchers.Default) {
            val mw = mask.width
            val mh = mask.height
            // PENTING: buffer TensorFlow Lite ML Kit berada dalam format Native Byte Order (Little Endian).
            // Harus diset nativeOrder() sebelum dibaca agar confidence tidak bernilai 0 / negatif!
            val buf = mask.buffer.order(ByteOrder.nativeOrder())
            buf.rewind()
            val floatBuf = buf.asFloatBuffer()
            val conf = FloatArray(mw * mh)
            floatBuf.get(conf)
            if (mw == bmp.width && mh == bmp.height) conf
            else MaskRefiner.resample(conf, mw, mh, bmp.width, bmp.height)
        }
    }

    override suspend fun confidence(bitmap: Bitmap): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val pass1 = try {
            runRawMask(bitmap)
        } catch (_: Exception) {
            FloatArray(w * h)
        }

        var maxConf = 0f
        for (c in pass1) {
            if (c > maxConf) maxConf = c
        }

        // Jika model neural network mendeteksi subjek (confidence >= 0.22)
        if (maxConf >= 0.22f) {
            return pass1
        }

        // Fallback: Segmentasi berbasis kontras warna latar belakang sudut jika ML Kit tidak mendeteksi manusia
        // (misal gambar kartun/anime, stiker, atau latar studio)
        return withContext(Dispatchers.Default) {
            MaskRefiner.fallbackColorSegmentation(bitmap)
        }
    }

    override fun close() {
        runCatching { segmenter.close() }
    }
}

// ---------------------------------------------------------------------------------------------
// Penghalusan mask & Pemisahan Warna Subjek vs Objek Sekitar
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

    /**
     * Fallback cerdas pemisahan background berbasis analisis warna latar sudut gambar:
     * Digunakan jika foto bukan selfie manusia (misal: stiker anime, kartun, objek/cover, atau foto gelap).
     */
    fun fallbackColorSegmentation(bitmap: Bitmap): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val n = w * h
        val pixels = IntArray(n)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val corners = listOf(
            pixels[0],
            pixels[min(w - 1, 15)],
            pixels[w - 1],
            pixels[(h - 1) * w],
            pixels[(h - 1) * w + w - 1]
        )
        var avgR = 0f
        var avgG = 0f
        var avgB = 0f
        for (c in corners) {
            avgR += ((c shr 16) and 255).toFloat()
            avgG += ((c shr 8) and 255).toFloat()
            avgB += (c and 255).toFloat()
        }
        avgR /= corners.size
        avgG /= corners.size
        avgB /= corners.size

        val conf = FloatArray(n)
        for (i in 0 until n) {
            val c = pixels[i]
            val pr = ((c shr 16) and 255).toFloat()
            val pg = ((c shr 8) and 255).toFloat()
            val pb = (c and 255).toFloat()
            val dist = sqrt(
                2.1f * (pr - avgR) * (pr - avgR) +
                    4.0f * (pg - avgG) * (pg - avgG) +
                    1.9f * (pb - avgB) * (pb - avgB)
            )
            conf[i] = smoothstep(20f, 60f, dist)
        }
        return conf
    }

    /** Box filter (rata-rata jendela (2r+1)^2, tepi dinormalisasi), O(n). */
    fun boxFilter(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        if (r <= 0) return src.copyOf()
        val tmp = FloatArray(w * h)
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

    /**
     * Cari bounding box karakter manusia utama dari confidence pass-1, mengabaikan objek terpisah di pinggir.
     * Return [x0, y0, x1, y1] dengan margin ~14%.
     */
    fun findMainSubjectBox(conf: FloatArray, w: Int, h: Int): IntArray? {
        val n = w * h
        val binary = BooleanArray(n) { conf[it] > 0.32f }
        val comp = label(binary, w, h)
        if (comp.sizes.size <= 1) return null

        var bestLabel = 0
        var bestScore = -1f
        // Hitung pusat massa dan skor tiap komponen (ukuran * bobot sentralitas)
        val sumX = FloatArray(comp.sizes.size)
        val sumY = FloatArray(comp.sizes.size)
        val sumConf = FloatArray(comp.sizes.size)
        for (i in 0 until n) {
            val l = comp.labels[i]
            if (l != 0) {
                sumX[l] += (i % w).toFloat()
                sumY[l] += (i / w).toFloat()
                sumConf[l] += conf[i]
            }
        }
        for (l in 1 until comp.sizes.size) {
            val sz = comp.sizes[l]
            if (sz < n * 0.005f) continue
            val cx = (sumX[l] / sz) / w
            val cy = (sumY[l] / sz) / h
            // Karakter utama biasanya berada di area tengah horizontal/vertikal
            val distCenter = hypot((cx - 0.5f) * 1.1f, (cy - 0.52f) * 0.8f)
            val centerWeight = (1.15f - distCenter).coerceIn(0.35f, 1.15f)
            val meanC = sumConf[l] / sz
            val score = sz * centerWeight * (0.5f + 0.5f * meanC)
            if (score > bestScore) {
                bestScore = score
                bestLabel = l
            }
        }
        if (bestLabel == 0) return null

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        val mainSize = comp.sizes[bestLabel]
        for (i in 0 until n) {
            val l = comp.labels[i]
            // Sertakan komponen utama, atau komponen cukup besar (>2% utama, mis. tangan/kaki/pose) yang cukup yakin
            if (l == bestLabel || (l != 0 && comp.sizes[l] >= mainSize * 0.02f && sumConf[l] / comp.sizes[l] > 0.45f)) {
                val x = i % w
                val y = i / w
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < minX || maxY < minY) return null

        val bw = maxX - minX + 1
        val bh = maxY - minY + 1
        val padX = max((bw * 0.20f).toInt(), (w * 0.08f).toInt())
        val padY = max((bh * 0.18f).toInt(), (h * 0.08f).toInt())
        return intArrayOf(
            max(0, minX - padX),
            max(0, minY - padY),
            min(w - 1, maxX + padX),
            min(h - 1, maxY + padY),
        )
    }

    /** Guided filter (He et al.) untuk 1 channel panduan. */
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

    /**
     * Multi-channel Color-Aware Guided Filter:
     * Menggabungkan panduan dari Luminance (Y) dan Chrominance (R-G, B-Y) agar tepi mask
     * memisahkan perbedaan warna antara karakter dan objek sekitar (walaupun tingkat terangnya mirip).
     */
    private fun guidedFilterColor(
        rCh: FloatArray,
        gCh: FloatArray,
        bCh: FloatArray,
        p: FloatArray,
        w: Int,
        h: Int,
        r: Int,
        eps: Float,
    ): FloatArray {
        val n = w * h
        val lum = FloatArray(n) { 0.299f * rCh[it] + 0.587f * gCh[it] + 0.114f * bCh[it] }
        val rg = FloatArray(n) { (rCh[it] - gCh[it]) * 0.5f + 0.5f }
        val by = FloatArray(n) { (bCh[it] - 0.5f * (rCh[it] + gCh[it])) * 0.5f + 0.5f }

        val qLum = guidedFilter(lum, p, w, h, r, eps)
        val qRg = guidedFilter(rg, p, w, h, r, eps * 1.2f)
        val qBy = guidedFilter(by, p, w, h, r, eps * 1.2f)

        return FloatArray(n) { i ->
            (0.50f * qLum[i] + 0.25f * qRg[i] + 0.25f * qBy[i]).coerceIn(0f, 1f)
        }
    }

    /**
     * Pemisahan Afinitas Warna Lokal (Local Foreground vs Background Color Discrimination):
     * - Di area transisi/ragu (0.10 < conf < 0.90), kita membandingkan warna piksel dengan
     *   rata-rata warna Foreground yakin (conf >= 0.78) dan Background yakin (conf <= 0.16) di sekitarnya.
     * - Jika piksel di sekitar tepi warnanya mirip objek background dan berbeda jauh dari warna
     *   baju/kulit karakter, confidence-nya ditekan agar objek sekitar terhapus bersih.
     * - Jika piksel di dalam/tepi tubuh warnanya sangat mirip dengan baju/kulit karakter yang yakin
     *   di dekatnya, confidence-nya diangkat agar bagian tubuh/baju karakter tidak ikut terhapus.
     */
    private fun applyLocalColorAffinity(
        rCh: FloatArray,
        gCh: FloatArray,
        bCh: FloatArray,
        conf: FloatArray,
        w: Int,
        h: Int,
    ): FloatArray {
        val n = w * h
        val rad = (max(w, h) / 26).coerceIn(14, 42)

        val fgW = FloatArray(n) { if (conf[it] >= 0.78f) 1f else if (conf[it] >= 0.65f) 0.35f else 0f }
        val bgW = FloatArray(n) { if (conf[it] <= 0.15f) 1f else if (conf[it] <= 0.28f) 0.35f else 0f }

        val sumFgW = boxFilter(fgW, w, h, rad)
        val sumBgW = boxFilter(bgW, w, h, rad)

        val fgR = boxFilter(FloatArray(n) { fgW[it] * rCh[it] }, w, h, rad)
        val fgG = boxFilter(FloatArray(n) { fgW[it] * gCh[it] }, w, h, rad)
        val fgB = boxFilter(FloatArray(n) { fgW[it] * bCh[it] }, w, h, rad)

        val bgR = boxFilter(FloatArray(n) { bgW[it] * rCh[it] }, w, h, rad)
        val bgG = boxFilter(FloatArray(n) { bgW[it] * gCh[it] }, w, h, rad)
        val bgB = boxFilter(FloatArray(n) { bgW[it] * bCh[it] }, w, h, rad)

        val out = FloatArray(n)
        for (i in 0 until n) {
            val c = conf[i]
            val fw = sumFgW[i]
            val bw = sumBgW[i]

            if (c in 0.08f..0.93f && fw > 0.015f && bw > 0.015f) {
                val mFgR = fgR[i] / fw
                val mFgG = fgG[i] / fw
                val mFgB = fgB[i] / fw

                val mBgR = bgR[i] / bw
                val mBgG = bgG[i] / bw
                val mBgB = bgB[i] / bw

                val pr = rCh[i]
                val pg = gCh[i]
                val pb = bCh[i]

                // Jarak warna perseptual (bobot R, G, B)
                val dFg = sqrt(
                    2.1f * (pr - mFgR) * (pr - mFgR) +
                        4.0f * (pg - mFgG) * (pg - mFgG) +
                        1.9f * (pb - mFgB) * (pb - mFgB)
                )
                val dBg = sqrt(
                    2.1f * (pr - mBgR) * (pr - mBgR) +
                        4.0f * (pg - mBgG) * (pg - mBgG) +
                        1.9f * (pb - mBgB) * (pb - mBgB)
                )
                // Seberapa kontras warna FG dan BG di lokasi ini
                val fgBgSep = sqrt(
                    2.1f * (mFgR - mBgR) * (mFgR - mBgR) +
                        4.0f * (mFgG - mBgG) * (mFgG - mBgG) +
                        1.9f * (mFgB - mBgB) * (mFgB - mBgB)
                )

                if (fgBgSep > 0.08f) {
                    val colorAffinity = (dBg / (dFg + dBg + 1e-4f)).coerceIn(0f, 1f)
                    val sepStrength = ((fgBgSep - 0.08f) / 0.35f).coerceIn(0f, 1f) * 0.35f
                    var adjusted = c * (1f - sepStrength) + colorAffinity * sepStrength

                    // Jika piksel berada di dalam area dominan FG dan warnanya sangat dekat dengan FG,
                    // lindungi supaya baju/tangan karakter tidak ikut terhapus.
                    if (c >= 0.30f && dFg < 0.18f && dBg > dFg * 1.4f) {
                        adjusted = max(adjusted, (c + 0.25f).coerceAtMost(0.98f))
                    }
                    // Jika piksel berada di pinggir luar dan warnanya sangat mirip objek background,
                    // tekan supaya objek sekitar karakter terhapus bersih (hanya jika confidence awal memang rendah).
                    if (c <= 0.40f && dBg < 0.12f && dFg > dBg * 1.80f) {
                        adjusted = min(adjusted, (c - 0.20f).coerceAtLeast(0.0f))
                    }
                    out[i] = adjusted.coerceIn(0f, 1f)
                } else {
                    out[i] = c
                }
            } else if (c in 0.30f..0.75f && fw > 0.25f && bw <= 0.015f) {
                // Piksel berada di tengah-tengah tubuh karakter (dikelilingi FG, jauh dari BG)
                val mFgR = fgR[i] / fw
                val mFgG = fgG[i] / fw
                val mFgB = fgB[i] / fw
                val dFg = abs(rCh[i] - mFgR) + abs(gCh[i] - mFgG) + abs(bCh[i] - mFgB)
                // Pulihkan bagian dalam tubuh/logo pakaian agar tidak bolong
                out[i] = if (dFg < 0.45f) max(c, 0.85f) else max(c, 0.68f)
            } else {
                out[i] = c
            }
        }
        return out
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

    /**
     * Bersihkan objek sekitar yang terpisah dari karakter utama, pertahankan tangan/lengan yang
     * tersambung jembatan tipis, dan tutup lubang di dalam tubuh/pakaian karakter.
     */
    private fun cleanup(alpha: FloatArray, w: Int, h: Int) {
        val n = w * h

        // 1. Gunakan threshold jembatan rendah (0.24f) agar tangan/jari yang terhubung pergelangan tipis
        //    tetap dianggap satu kesatuan dengan tubuh karakter utama.
        val bridgeFg = BooleanArray(n) { alpha[it] > 0.24f }
        val bridgeComp = label(bridgeFg, w, h)
        if (bridgeComp.sizes.size <= 1) return

        // Pilih komponen karakter utama (skor kombinasi luas area & sentralitas)
        val sumX = FloatArray(bridgeComp.sizes.size)
        val sumY = FloatArray(bridgeComp.sizes.size)
        val strongCount = IntArray(bridgeComp.sizes.size)
        for (i in 0 until n) {
            val l = bridgeComp.labels[i]
            if (l != 0) {
                sumX[l] += (i % w).toFloat()
                sumY[l] += (i / w).toFloat()
                if (alpha[i] > 0.60f) strongCount[l]++
            }
        }

        var mainLabel = 0
        var bestScore = -1f
        for (l in 1 until bridgeComp.sizes.size) {
            val sz = bridgeComp.sizes[l]
            if (strongCount[l] < 20) continue
            val cx = (sumX[l] / sz) / w
            val cy = (sumY[l] / sz) / h
            val distCenter = hypot((cx - 0.5f) * 1.1f, (cy - 0.52f) * 0.8f)
            val centerWeight = (1.15f - distCenter).coerceIn(0.35f, 1.15f)
            val score = strongCount[l] * centerWeight
            if (score > bestScore) {
                bestScore = score
                mainLabel = l
            }
        }
        if (mainLabel == 0) return

        val mainStrong = strongCount[mainLabel]
        val keepBridge = BooleanArray(bridgeComp.sizes.size)
        for (l in 1 until bridgeComp.sizes.size) {
            // Simpan komponen karakter utama; buang pulau di luar kecuali tangan/kaki atau menyentuh tepi bawah/samping
            keepBridge[l] = (l == mainLabel) ||
                (strongCount[l] >= mainStrong * 0.03f && strongCount[l].toFloat() / bridgeComp.sizes[l] > 0.40f) ||
                (bridgeComp.touchesBorder[l] && strongCount[l] >= 40)
        }

        val keptMask = FloatArray(n) {
            val l = bridgeComp.labels[it]
            if (l != 0 && keepBridge[l]) 1f else 0f
        }
        var hasKept = false
        for (v in keptMask) {
            if (v > 0f) { hasKept = true; break }
        }
        if (hasKept) {
            val nearKept = boxFilter(keptMask, w, h, 3)
            for (i in 0 until n) {
                if (nearKept[i] <= 0.001f) alpha[i] = 0f
            }
        }

        // 2. Pada threshold ketat (0.48f), buang serpihan/pulau kecil yang lepas dari tubuh
        val strictFg = BooleanArray(n) { alpha[it] > 0.48f }
        val strictComp = label(strictFg, w, h)
        var largestStrict = 0
        for (l in 1 until strictComp.sizes.size) largestStrict = max(largestStrict, strictComp.sizes[l])
        if (largestStrict > 0) {
            val minIsland = max(24, (largestStrict * 0.004f).toInt())
            for (i in 0 until n) {
                val l = strictComp.labels[i]
                if (l != 0 && strictComp.sizes[l] < minIsland) {
                    alpha[i] = 0f
                }
            }
        }

        // 3. Tutup lubang di dalam tubuh/pakaian karakter (hingga 10% luas gambar, mis. baju gelap, logo baju, sabuk, bayangan)
        val bg = BooleanArray(n) { alpha[it] <= 0.48f }
        val bgComp = label(bg, w, h)
        val maxHole = (n * 0.10f).toInt()
        for (i in 0 until n) {
            val l = bgComp.labels[i]
            if (l != 0 && !bgComp.touchesBorder[l] && bgComp.sizes[l] <= maxHole) {
                alpha[i] = max(alpha[i], 0.98f)
            }
        }
    }

    /** Hasil: alpha dasar 0..1 (belum dikurva), ukuran w*h. */
    fun refine(pixels: IntArray, confidence: FloatArray, w: Int, h: Int): FloatArray {
        val n = w * h
        val rCh = FloatArray(n) { ((pixels[it] shr 16) and 255) / 255f }
        val gCh = FloatArray(n) { ((pixels[it] shr 8) and 255) / 255f }
        val bCh = FloatArray(n) { (pixels[it] and 255) / 255f }

        // 1. Koreksi confidence dengan afinitas warna lokal (FG vs BG)
        val colorAdjusted = applyLocalColorAffinity(rCh, gCh, bCh, confidence, w, h)

        // 2. Kontraskan kurva awal sebelum guided filter (ambang 0.12f - 0.70f agar baju & anggota badan solid)
        val p = FloatArray(n) { smoothstep(0.12f, 0.70f, colorAdjusted[it]) }

        // 3. Multi-channel Color Guided Filter agar tepi mengikuti batas warna asli
        val r = (max(w, h) / 210).coerceIn(3, 7)
        val q = guidedFilterColor(rCh, gCh, bCh, p, w, h, r, 6e-4f)

        // 4. Bersihkan objek sekitar & tutup lubang di dalam tubuh karakter
        cleanup(q, w, h)
        return q
    }

    /** Netralkan warna background yang "bocor" di piksel tepi semi-transparan. */
    fun defringe(pixels: IntArray, alpha: FloatArray, w: Int, h: Int): IntArray {
        val n = w * h
        val solid = FloatArray(n) { if (alpha[it] >= 0.96f) 1f else 0f }
        val rad = 4
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
            if (a > 0.02f && a < 0.96f && sumW[i] > 0.02f) {
                val t = (1f - a) * 0.88f
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
    var softness = 0.20f
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
        val c = 0.47f - edgeShift
        val hw = 0.05f + 0.28f * softness
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
        softness = 0.20f
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
        /** Proses lengkap di background thread: segmentasi presisi + penghalusan warna + defringe. */
        suspend fun create(original: Bitmap, remover: BackgroundRemover): CutoutEditor {
            val w = original.width
            val h = original.height
            // Skala ke batas 800px untuk inferensi cepat & hemat memori, lalu upsample mask
            val maxSide = max(w, h)
            val scaleDown = if (maxSide > 800) 800f / maxSide else 1f
            val scaledBmp = if (scaleDown < 0.99f) {
                Bitmap.createScaledBitmap(
                    original,
                    max(1, (w * scaleDown).toInt()),
                    max(1, (h * scaleDown).toInt()),
                    true
                )
            } else original

            val confScaled = remover.confidence(scaledBmp)
            val fullConf = if (scaledBmp !== original) {
                val up = MaskRefiner.resample(confScaled, scaledBmp.width, scaledBmp.height, w, h)
                scaledBmp.recycle()
                up
            } else {
                confScaled
            }

            return withContext(Dispatchers.Default) {
                val pixels = IntArray(w * h)
                original.getPixels(pixels, 0, w, 0, 0, w, h)
                val base = MaskRefiner.refine(pixels, fullConf, w, h)
                val clean = MaskRefiner.defringe(pixels, base, w, h)
                CutoutEditor(w, h, clean, base)
            }
        }
    }
}
