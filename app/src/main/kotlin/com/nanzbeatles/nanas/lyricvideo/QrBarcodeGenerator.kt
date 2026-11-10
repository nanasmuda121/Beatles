/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyricvideo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Pure-Kotlin standard-compliant ISO/IEC 18004 QR Code & Code-128B Barcode generator.
 * Encodes temporary file share URLs into crisp, scannable QR and 1D Barcode Bitmaps
 * with zero external library overhead.
 */
object QrBarcodeGenerator {

    private data class QrVersionSpec(
        val version: Int,
        val size: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignCenters: IntArray
    )

    // ISO/IEC 18004 Level L specifications for Versions 1..6 (supports URLs up to 134 bytes)
    private val VERSION_SPECS = listOf(
        QrVersionSpec(1, 21, 19, 7, 1, intArrayOf()),
        QrVersionSpec(2, 25, 34, 10, 1, intArrayOf(6, 18)),
        QrVersionSpec(3, 29, 55, 15, 1, intArrayOf(6, 22)),
        QrVersionSpec(4, 33, 80, 20, 1, intArrayOf(6, 26)),
        QrVersionSpec(5, 37, 108, 26, 1, intArrayOf(6, 30)),
        QrVersionSpec(6, 41, 136, 18, 2, intArrayOf(6, 34))
    )

    /**
     * Generates a scannable ISO/IEC 18004 QR Code [Bitmap] for the given [text] URL.
     */
    fun generateQrCodeBitmap(
        text: String,
        pixelSize: Int = 512,
        quietZoneModules: Int = 3
    ): Bitmap {
        val payload = text.toByteArray(Charsets.UTF_8)
        val spec = VERSION_SPECS.firstOrNull { payload.size + 2 <= it.totalDataCodewords }
            ?: VERSION_SPECS.last()
        val safePayload = if (payload.size + 2 > spec.totalDataCodewords) {
            payload.copyOf(spec.totalDataCodewords - 2)
        } else {
            payload
        }

        // 1. Encode Byte Mode bitstream (0100 + 8-bit length + data bytes + terminator + pad bytes)
        val dataCodewords = encodeDataCodewords(safePayload, spec.totalDataCodewords)

        // 2. Compute Reed-Solomon Error Correction codewords and interleave if numBlocks > 1
        val finalCodewords = buildFinalMessage(dataCodewords, spec)

        // 3. Construct QR matrix with function patterns, data bits, Mask 0, and Format Info (Level L = 01, Mask = 000)
        val n = spec.size
        val modules = Array(n) { BooleanArray(n) }
        val isFunction = Array(n) { BooleanArray(n) }

        drawFunctionPatterns(modules, isFunction, spec)
        placeCodewords(modules, isFunction, finalCodewords, n)
        applyMask0(modules, isFunction, n)
        drawFormatBits(modules, isFunction, n, mask = 0) // Level L (01), Mask 0 (000)

        // 4. Render to Bitmap with quiet zone
        val totalModules = n + quietZoneModules * 2
        val bmp = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val darkPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        val modulePx = pixelSize.toFloat() / totalModules.toFloat()
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (modules[r][c]) {
                    val left = (c + quietZoneModules) * modulePx
                    val top = (r + quietZoneModules) * modulePx
                    canvas.drawRect(left, top, left + modulePx + 0.5f, top + modulePx + 0.5f, darkPaint)
                }
            }
        }
        return bmp
    }

    /**
     * Generates a scannable 1D Code-128B Barcode [Bitmap] for the given [text].
     */
    fun generateCode128Bitmap(
        text: String,
        widthPx: Int = 640,
        heightPx: Int = 140
    ): Bitmap {
        val patterns = CODE128_PATTERNS
        val codes = ArrayList<Int>(text.length + 3)
        codes.add(104) // Start Code B
        var checksum = 104
        var weight = 1
        for (ch in text) {
            val code = (ch.code - 32).coerceIn(0, 94)
            codes.add(code)
            checksum += code * weight
            weight++
        }
        codes.add(checksum % 103)
        codes.add(106) // Stop Code

        val bits = ArrayList<Boolean>(codes.size * 11 + 22)
        repeat(10) { bits.add(false) } // Quiet zone left
        for (c in codes) {
            val pat = patterns[c]
            var isBar = true
            for (digitChar in pat) {
                val count = digitChar - '0'
                repeat(count) { bits.add(isBar) }
                isBar = !isBar
            }
        }
        repeat(10) { bits.add(false) } // Quiet zone right

        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        val unitW = widthPx.toFloat() / bits.size.coerceAtLeast(1)
        val vPad = heightPx * 0.12f
        for (i in bits.indices) {
            if (bits[i]) {
                val left = i * unitW
                canvas.drawRect(left, vPad, left + unitW + 0.4f, heightPx - vPad, barPaint)
            }
        }
        return bmp
    }

    private fun encodeDataCodewords(payload: ByteArray, capacityBytes: Int): IntArray {
        val bitCap = capacityBytes * 8
        val bits = ArrayList<Int>(bitCap)

        fun appendBits(value: Int, len: Int) {
            for (i in len - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Mode indicator: Byte mode = 0100 (4 bits)
        appendBits(0b0100, 4)
        // Character count indicator (8 bits for versions 1..9)
        appendBits(payload.size, 8)
        // Payload bytes
        for (b in payload) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zero bits)
        val termBits = minOf(4, bitCap - bits.size)
        repeat(termBits) { bits.add(0) }
        // Pad to byte boundary
        while (bits.size % 8 != 0) {
            bits.add(0)
        }
        // Pad bytes 0xEC, 0x11 alternating
        var padToggle = true
        while (bits.size < bitCap) {
            appendBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val out = IntArray(capacityBytes)
        for (i in 0 until capacityBytes) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bits[i * 8 + b]
            }
            out[i] = byteVal
        }
        return out
    }

    private fun buildFinalMessage(data: IntArray, spec: QrVersionSpec): IntArray {
        if (spec.numBlocks == 1) {
            val ec = reedSolomonComputeRemainder(data, spec.ecCodewordsPerBlock)
            return data + ec
        }
        val blockDataLen = spec.totalDataCodewords / spec.numBlocks
        val dataBlocks = Array(spec.numBlocks) { b ->
            data.copyOfRange(b * blockDataLen, (b + 1) * blockDataLen)
        }
        val ecBlocks = Array(spec.numBlocks) { b ->
            reedSolomonComputeRemainder(dataBlocks[b], spec.ecCodewordsPerBlock)
        }
        val result = IntArray(spec.totalDataCodewords + spec.ecCodewordsPerBlock * spec.numBlocks)
        var idx = 0
        for (i in 0 until blockDataLen) {
            for (b in 0 until spec.numBlocks) {
                result[idx++] = dataBlocks[b][i]
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (b in 0 until spec.numBlocks) {
                result[idx++] = ecBlocks[b][i]
            }
        }
        return result
    }

    private fun reedSolomonComputeRemainder(data: IntArray, degree: Int): IntArray {
        // Compute generator polynomial in GF(256) with primitive polynomial 0x11D
        var gen = IntArray(degree)
        gen[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                gen[j] = gfMultiply(gen[j], root)
                if (j + 1 < degree) {
                    gen[j] = gen[j] xor gen[j + 1]
                }
            }
            root = gfMultiply(root, 0x02)
        }

        val result = IntArray(degree)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, degree - 1)
            result[degree - 1] = 0
            for (j in 0 until degree) {
                result[j] = result[j] xor gfMultiply(gen[j], factor)
            }
        }
        return result
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var z = 0
        var a = x
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * a)
        }
        return z
    }

    private fun drawFunctionPatterns(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        spec: QrVersionSpec
    ) {
        val n = spec.size

        fun setFunctionModule(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until n && c in 0 until n) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        fun drawFinderPattern(centerR: Int, centerC: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    val dark = (dist != 2 && dist != 4)
                    setFunctionModule(centerR + dy, centerC + dx, dark)
                }
            }
        }

        // 3 Finder patterns
        drawFinderPattern(3, 3)
        drawFinderPattern(3, n - 4)
        drawFinderPattern(n - 4, 3)

        // Timing patterns
        for (i in 0 until n) {
            if (!isFunction[6][i]) setFunctionModule(6, i, i % 2 == 0)
            if (!isFunction[i][6]) setFunctionModule(i, 6, i % 2 == 0)
        }

        // Alignment patterns (for Version >= 2)
        val align = spec.alignCenters
        for (i in align.indices) {
            for (j in align.indices) {
                if ((i == 0 && j == 0) || (i == 0 && j == align.lastIndex) || (i == align.lastIndex && j == 0)) {
                    continue
                }
                val cr = align[i]
                val cc = align[j]
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        setFunctionModule(cr + dy, cc + dx, dist != 1)
                    }
                }
            }
        }

        // Reserve Format Information areas & Dark Module
        for (i in 0..8) {
            if (i != 6) {
                if (!isFunction[8][i]) setFunctionModule(8, i, false)
                if (!isFunction[i][8]) setFunctionModule(i, 8, false)
            }
        }
        for (i in 0..7) {
            if (!isFunction[8][n - 1 - i]) setFunctionModule(8, n - 1 - i, false)
            if (!isFunction[n - 1 - i][8]) setFunctionModule(n - 1 - i, 8, false)
        }
        // Dark module
        setFunctionModule(n - 8, 8, true)
    }

    private fun placeCodewords(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        codewords: IntArray,
        n: Int
    ) {
        var bitIdx = 0
        val totalBits = codewords.size * 8
        var right = n - 1
        var upward = true

        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until n) {
                val r = if (upward) (n - 1 - vert) else vert
                for (j in 0..1) {
                    val c = right - j
                    if (!isFunction[r][c]) {
                        val dark = if (bitIdx < totalBits) {
                            ((codewords[bitIdx ushr 3] ushr (7 - (bitIdx and 7))) and 1) != 0
                        } else {
                            false
                        }
                        modules[r][c] = dark
                        bitIdx++
                    }
                }
            }
            upward = !upward
            right -= 2
        }
    }

    private fun applyMask0(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        n: Int
    ) {
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (!isFunction[r][c] && (r + c) % 2 == 0) {
                    modules[r][c] = !modules[r][c]
                }
            }
        }
    }

    private fun drawFormatBits(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        n: Int,
        mask: Int
    ) {
        // Level L format indicator = 01
        val data = (0b01 shl 3) or mask
        var rem = data
        for (i in 0 until 10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        fun getBit(idx: Int): Boolean = ((bits ushr idx) and 1) != 0

        // First copy around top-left finder (bit 0 at (0,8) .. bit 14 at (8,0))
        for (i in 0..5) modules[i][8] = getBit(i)
        modules[7][8] = getBit(6)
        modules[8][8] = getBit(7)
        modules[8][7] = getBit(8)
        for (i in 9..14) modules[8][14 - i] = getBit(i)

        // Second copy along top-right (bit 0..7) and bottom-left (bit 8..14)
        for (i in 0..7) modules[8][n - 1 - i] = getBit(i)
        for (i in 8..14) modules[n - 15 + i][8] = getBit(i)
        modules[n - 8][8] = true
    }

    // Standard Code-128 bar/space width patterns (codes 0..106)
    private val CODE128_PATTERNS = arrayOf(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    )
}
