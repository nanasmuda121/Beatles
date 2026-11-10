/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.lyricvideo

import android.media.MediaFormat
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Pure-Kotlin FastStart ISO-BMFF (.mp4 / .m4a) container muxer for H.264 (avc1) video
 * and AAC-LC (mp4a) audio.
 *
 * Avoids Android's native `MPEG4Writer` (`android.media.MediaMuxer`), eliminating
 * `E/MPEG4Writer: Stop() called but track is not started or stopped` logcat errors
 * and producing web/social-ready FastStart MP4 files (`[ftyp][moov][mdat]`).
 */
object Mp4ContainerWriter {

    private const val MOVIE_TIMESCALE = 1000L
    private const val VIDEO_TIMESCALE = 90000L

    data class SampleEntryMeta(
        val offsetInMdat: Long,
        val size: Int,
        val ptsUs: Long,
        val isSync: Boolean
    )

    data class AudioPacket(
        val data: ByteArray,
        val ptsUs: Long
    )

    /**
     * Writes an audio-only `.m4a` (ISO-BMFF `mp4a`) file from a list of raw AAC access units.
     */
    fun writeAudioOnlyM4a(
        outputFile: File,
        sampleRate: Int,
        channelCount: Int,
        csd0: ByteArray?,
        packets: List<AudioPacket>
    ): Boolean {
        if (packets.size < 2) return false
        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.writing.tmp")
        try {
            val asc = if (csd0 != null && csd0.size >= 2) {
                csd0
            } else {
                buildDefaultAacLcAsc(sampleRate, channelCount)
            }

            val sampleMetas = ArrayList<SampleEntryMeta>(packets.size)
            var currentOffset = 0L
            var lastPtsUs = -1L
            for (pkt in packets) {
                if (pkt.data.isEmpty()) continue
                var pts = maxOf(0L, pkt.ptsUs)
                if (pts <= lastPtsUs) {
                    pts = lastPtsUs + ((1_000_000L * 1024L) / sampleRate.coerceAtLeast(8000))
                }
                lastPtsUs = pts
                sampleMetas.add(
                    SampleEntryMeta(
                        offsetInMdat = currentOffset,
                        size = pkt.data.size,
                        ptsUs = pts,
                        isSync = true
                    )
                )
                currentOffset += pkt.data.size
            }

            if (sampleMetas.size < 2 || currentOffset <= 0L) return false

            val ftypBox = buildFtypBox(isAudioOnly = true)
            val dummyMoov = buildMoovBox(
                videoTrack = null,
                audioTrack = AudioTrackConfig(
                    trackId = 1,
                    sampleRate = sampleRate.coerceAtLeast(8000),
                    channelCount = channelCount.coerceIn(1, 8),
                    asc = asc,
                    samples = sampleMetas
                ),
                mdatDataStartOffset = 0L
            )
            val mdatDataStartOffset = ftypBox.size.toLong() + dummyMoov.size.toLong() + 8L
            val finalMoov = buildMoovBox(
                videoTrack = null,
                audioTrack = AudioTrackConfig(
                    trackId = 1,
                    sampleRate = sampleRate.coerceAtLeast(8000),
                    channelCount = channelCount.coerceIn(1, 8),
                    asc = asc,
                    samples = sampleMetas
                ),
                mdatDataStartOffset = mdatDataStartOffset
            )

            FileOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos, 64 * 1024).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.write(ftypBox)
                        dos.write(finalMoov)
                        dos.writeInt((8L + currentOffset).toInt())
                        dos.writeBytes("mdat")
                        for (pkt in packets) {
                            if (pkt.data.isNotEmpty()) {
                                dos.write(pkt.data)
                            }
                        }
                        dos.flush()
                    }
                }
            }

            if (outputFile.exists()) outputFile.delete()
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
            return outputFile.exists() && outputFile.length() > 512L
        } catch (e: Exception) {
            tempFile.delete()
            return false
        }
    }

    /**
     * Streaming session that writes encoded H.264 video frames + AAC audio frames to a temporary
     * `mdat` payload file on disk, and finalizes them into a FastStart `.mp4` (`[ftyp][moov][mdat]`).
     */
    class Mp4MuxSession(
        private val outputFile: File,
        private val videoWidth: Int,
        private val videoHeight: Int
    ) {
        private val mdatTempFile = File(
            outputFile.parentFile ?: outputFile.absoluteFile.parentFile,
            "mdat_${System.nanoTime()}.raw"
        )
        private var mdatOut: DataOutputStream? = null
        private var currentMdatOffset: Long = 0L

        private var spsBytes: ByteArray? = null
        private var ppsBytes: ByteArray? = null
        private var audioSampleRate: Int = 44100
        private var audioChannelCount: Int = 2
        private var audioAscBytes: ByteArray? = null

        private val videoSamples = ArrayList<SampleEntryMeta>(1024)
        private val audioSamples = ArrayList<SampleEntryMeta>(2048)
        private var lastVideoPtsUs = -1L
        private var lastAudioPtsUs = -1L

        init {
            outputFile.parentFile?.mkdirs()
            mdatOut = DataOutputStream(BufferedOutputStream(FileOutputStream(mdatTempFile), 128 * 1024))
        }

        fun setVideoFormat(format: MediaFormat) {
            format.getByteBuffer("csd-0")?.let { parseAndStoreSpsPps(copyByteBuffer(it)) }
            format.getByteBuffer("csd-1")?.let { parseAndStoreSpsPps(copyByteBuffer(it)) }
        }

        fun setAudioFormat(format: MediaFormat) {
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
            }
            format.getByteBuffer("csd-0")?.let { buf ->
                val bytes = copyByteBuffer(buf)
                if (bytes.size >= 2) {
                    audioAscBytes = bytes
                }
            }
        }

        fun feedVideoCodecConfig(data: ByteArray) {
            parseAndStoreSpsPps(data)
        }

        /**
         * Converts an H.264 frame from Annex-B (or length-prefixed) to 4-byte length-prefixed AVCC
         * and appends it to the `mdat` stream.
         */
        fun writeVideoSample(rawBytes: ByteArray, presentationTimeUs: Long, isKeyFrameFlag: Boolean) {
            val out = mdatOut ?: return
            if (rawBytes.isEmpty()) return

            val nals = extractNalUnits(rawBytes)
            if (nals.isEmpty()) return

            var hasIdr = isKeyFrameFlag
            var avccFrameSize = 0
            val payloadNals = ArrayList<ByteArray>(nals.size)

            for (nal in nals) {
                if (nal.isEmpty()) continue
                val nalType = nal[0].toInt() and 0x1F
                when (nalType) {
                    7 -> if (spsBytes == null) spsBytes = nal
                    8 -> if (ppsBytes == null) ppsBytes = nal
                    5 -> {
                        hasIdr = true
                        payloadNals.add(nal)
                        avccFrameSize += 4 + nal.size
                    }
                    else -> {
                        payloadNals.add(nal)
                        avccFrameSize += 4 + nal.size
                    }
                }
            }

            if (payloadNals.isEmpty() || avccFrameSize <= 0) return

            // Ensure the very first video sample is marked as sync
            val isSync = hasIdr || videoSamples.isEmpty()
            var pts = maxOf(0L, presentationTimeUs)
            if (pts <= lastVideoPtsUs) {
                pts = lastVideoPtsUs + 1000L
            }
            lastVideoPtsUs = pts

            val sampleOffset = currentMdatOffset
            for (nal in payloadNals) {
                out.writeInt(nal.size)
                out.write(nal)
            }
            currentMdatOffset += avccFrameSize

            videoSamples.add(
                SampleEntryMeta(
                    offsetInMdat = sampleOffset,
                    size = avccFrameSize,
                    ptsUs = pts,
                    isSync = isSync
                )
            )
        }

        fun writeAudioSample(rawBytes: ByteArray, presentationTimeUs: Long) {
            val out = mdatOut ?: return
            if (rawBytes.isEmpty()) return

            var pts = maxOf(0L, presentationTimeUs)
            if (pts <= lastAudioPtsUs) {
                pts = lastAudioPtsUs + ((1_000_000L * 1024L) / audioSampleRate.coerceAtLeast(8000))
            }
            lastAudioPtsUs = pts

            val sampleOffset = currentMdatOffset
            out.write(rawBytes)
            currentMdatOffset += rawBytes.size

            audioSamples.add(
                SampleEntryMeta(
                    offsetInMdat = sampleOffset,
                    size = rawBytes.size,
                    ptsUs = pts,
                    isSync = true
                )
            )
        }

        val videoSampleCount: Int get() = videoSamples.size
        val audioSampleCount: Int get() = audioSamples.size

        fun finish(): Boolean {
            try {
                mdatOut?.flush()
                mdatOut?.close()
                mdatOut = null

                if (videoSamples.isEmpty() || currentMdatOffset <= 0L) {
                    mdatTempFile.delete()
                    return false
                }

                val sps = spsBytes ?: byteArrayOf(0x67, 0x42, 0x80.toByte(), 0x1F, 0xDA.toByte(), 0x01, 0x40, 0x16, 0xEC.toByte(), 0x04, 0x40, 0x00, 0x00, 0x03, 0x00, 0x40, 0x00, 0x00, 0x0F, 0x03, 0xC5.toByte(), 0x8B.toByte(), 0x80.toByte())
                val pps = ppsBytes ?: byteArrayOf(0x68, 0xCE.toByte(), 0x3C, 0x80.toByte())

                val videoConfig = VideoTrackConfig(
                    trackId = 1,
                    width = videoWidth,
                    height = videoHeight,
                    sps = sps,
                    pps = pps,
                    samples = videoSamples
                )

                val audioConfig = if (audioSamples.size >= 2) {
                    val asc = audioAscBytes ?: buildDefaultAacLcAsc(audioSampleRate, audioChannelCount)
                    AudioTrackConfig(
                        trackId = 2,
                        sampleRate = audioSampleRate,
                        channelCount = audioChannelCount,
                        asc = asc,
                        samples = audioSamples
                    )
                } else null

                val ftypBox = buildFtypBox(isAudioOnly = false)
                val dummyMoov = buildMoovBox(
                    videoTrack = videoConfig,
                    audioTrack = audioConfig,
                    mdatDataStartOffset = 0L
                )
                val mdatDataStartOffset = ftypBox.size.toLong() + dummyMoov.size.toLong() + 8L
                val finalMoov = buildMoovBox(
                    videoTrack = videoConfig,
                    audioTrack = audioConfig,
                    mdatDataStartOffset = mdatDataStartOffset
                )

                val tempOutFile = File(outputFile.parentFile, "${outputFile.name}.mux.tmp")
                FileOutputStream(tempOutFile).use { fos ->
                    BufferedOutputStream(fos, 128 * 1024).use { bos ->
                        DataOutputStream(bos).use { dos ->
                            dos.write(ftypBox)
                            dos.write(finalMoov)
                            dos.writeInt((8L + currentMdatOffset).toInt())
                            dos.writeBytes("mdat")
                            FileInputStream(mdatTempFile).use { fis ->
                                BufferedInputStream(fis, 128 * 1024).use { bis ->
                                    val buf = ByteArray(64 * 1024)
                                    var read: Int
                                    while (bis.read(buf).also { read = it } != -1) {
                                        dos.write(buf, 0, read)
                                    }
                                }
                            }
                            dos.flush()
                        }
                    }
                }

                mdatTempFile.delete()
                if (outputFile.exists()) outputFile.delete()
                if (!tempOutFile.renameTo(outputFile)) {
                    tempOutFile.copyTo(outputFile, overwrite = true)
                    tempOutFile.delete()
                }
                return outputFile.exists() && outputFile.length() > 1024L
            } catch (e: Exception) {
                release()
                return false
            }
        }

        fun release() {
            try { mdatOut?.close() } catch (_: Exception) {}
            mdatOut = null
            try { mdatTempFile.delete() } catch (_: Exception) {}
        }

        private fun parseAndStoreSpsPps(data: ByteArray) {
            for (nal in extractNalUnits(data)) {
                if (nal.isEmpty()) continue
                when (nal[0].toInt() and 0x1F) {
                    7 -> spsBytes = nal
                    8 -> ppsBytes = nal
                }
            }
        }
    }

    private data class VideoTrackConfig(
        val trackId: Int,
        val width: Int,
        val height: Int,
        val sps: ByteArray,
        val pps: ByteArray,
        val samples: List<SampleEntryMeta>
    )

    private data class AudioTrackConfig(
        val trackId: Int,
        val sampleRate: Int,
        val channelCount: Int,
        val asc: ByteArray,
        val samples: List<SampleEntryMeta>
    )

    fun copyByteBuffer(buffer: ByteBuffer): ByteArray {
        val dup = buffer.duplicate()
        val out = ByteArray(dup.remaining())
        dup.get(out)
        return out
    }

    fun extractFormatCsd0(format: MediaFormat): ByteArray? {
        val buf = format.getByteBuffer("csd-0") ?: return null
        return copyByteBuffer(buf).takeIf { it.isNotEmpty() }
    }

    /**
     * Builds a standard 2-byte ISO/IEC 14496-3 AudioSpecificConfig for AAC-LC (`audioObjectType = 2`).
     */
    fun buildDefaultAacLcAsc(sampleRate: Int, channelCount: Int): ByteArray {
        val freqIndex = when (sampleRate) {
            96000 -> 0
            88200 -> 1
            64000 -> 2
            48000 -> 3
            44100 -> 4
            32000 -> 5
            24000 -> 6
            22050 -> 7
            16000 -> 8
            12000 -> 9
            11025 -> 10
            8000 -> 11
            else -> 4
        }
        val ch = channelCount.coerceIn(1, 7)
        val b0 = ((2 shl 3) or (freqIndex shr 1)) and 0xFF
        val b1 = (((freqIndex and 1) shl 7) or (ch shl 3)) and 0xFF
        return byteArrayOf(b0.toByte(), b1.toByte())
    }

    /**
     * Splits an H.264 Annex-B byte array (with `00 00 00 01` or `00 00 01` start codes)
     * into individual raw NAL units (without start codes).
     */
    fun extractNalUnits(data: ByteArray): List<ByteArray> {
        if (data.isEmpty()) return emptyList()
        val startPositions = ArrayList<IntRange>(4)
        var i = 0
        val limit = data.size
        while (i <= limit - 3) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) {
                if (data[i + 2].toInt() == 1) {
                    startPositions.add(i..(i + 2))
                    i += 3
                    continue
                } else if (i + 3 < limit && data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1) {
                    startPositions.add(i..(i + 3))
                    i += 4
                    continue
                }
            }
            i++
        }

        if (startPositions.isEmpty()) {
            // Check if already 4-byte length-prefixed AVCC
            if (data.size > 4) {
                val len = ((data[0].toInt() and 0xFF) shl 24) or
                    ((data[1].toInt() and 0xFF) shl 16) or
                    ((data[2].toInt() and 0xFF) shl 8) or
                    (data[3].toInt() and 0xFF)
                if (len in 1..(data.size - 4)) {
                    val result = ArrayList<ByteArray>()
                    var pos = 0
                    while (pos + 4 <= data.size) {
                        val nalLen = ((data[pos].toInt() and 0xFF) shl 24) or
                            ((data[pos + 1].toInt() and 0xFF) shl 16) or
                            ((data[pos + 2].toInt() and 0xFF) shl 8) or
                            (data[pos + 3].toInt() and 0xFF)
                        pos += 4
                        if (nalLen <= 0 || pos + nalLen > data.size) break
                        result.add(data.copyOfRange(pos, pos + nalLen))
                        pos += nalLen
                    }
                    if (result.isNotEmpty()) return result
                }
            }
            return listOf(data)
        }

        val nals = ArrayList<ByteArray>(startPositions.size)
        for (idx in startPositions.indices) {
            val nalStart = startPositions[idx].last + 1
            val nalEnd = if (idx + 1 < startPositions.size) startPositions[idx + 1].first else data.size
            if (nalEnd > nalStart) {
                nals.add(data.copyOfRange(nalStart, nalEnd))
            }
        }
        return nals
    }

    private fun buildFtypBox(isAudioOnly: Boolean): ByteArray = box("ftyp") {
        if (isAudioOnly) {
            writeBytes("M4A ")
            writeInt(512)
            writeBytes("M4A ")
            writeBytes("isom")
            writeBytes("iso2")
            writeBytes("mp42")
        } else {
            writeBytes("isom")
            writeInt(512)
            writeBytes("isom")
            writeBytes("iso2")
            writeBytes("avc1")
            writeBytes("mp41")
        }
    }

    private fun buildMoovBox(
        videoTrack: VideoTrackConfig?,
        audioTrack: AudioTrackConfig?,
        mdatDataStartOffset: Long
    ): ByteArray {
        val videoTrakBytes = videoTrack?.let { buildVideoTrakBox(it, mdatDataStartOffset) }
        val audioTrakBytes = audioTrack?.let { buildAudioTrakBox(it, mdatDataStartOffset) }

        val videoDurMs = videoTrack?.let { computeTrackDurationMs(it.samples) } ?: 0L
        val audioDurMs = audioTrack?.let { computeTrackDurationMs(it.samples) } ?: 0L
        val movieDurationMs = maxOf(videoDurMs, audioDurMs, 100L)
        val nextTrackId = maxOf(videoTrack?.trackId ?: 0, audioTrack?.trackId ?: 0) + 1

        return box("moov") {
            write(buildMvhdBox(movieDurationMs, nextTrackId))
            if (videoTrakBytes != null) write(videoTrakBytes)
            if (audioTrakBytes != null) write(audioTrakBytes)
        }
    }

    private fun computeTrackDurationMs(samples: List<SampleEntryMeta>): Long {
        if (samples.isEmpty()) return 0L
        if (samples.size == 1) return 33L
        val firstUs = samples.first().ptsUs
        val lastUs = samples.last().ptsUs
        val avgDeltaUs = ((lastUs - firstUs) / (samples.size - 1).coerceAtLeast(1)).coerceAtLeast(1000L)
        return ((lastUs - firstUs + avgDeltaUs) / 1000L).coerceAtLeast(33L)
    }

    private fun buildMvhdBox(durationMs: Long, nextTrackId: Int): ByteArray = fullBox("mvhd", 0, 0) {
        writeInt(0) // creation_time
        writeInt(0) // modification_time
        writeInt(MOVIE_TIMESCALE.toInt())
        writeInt(durationMs.coerceAtLeast(1L).toInt())
        writeInt(0x00010000) // rate = 1.0
        writeShort(0x0100) // volume = 1.0
        writeShort(0) // reserved
        writeInt(0) // reserved
        writeInt(0) // reserved
        // Unity matrix
        writeInt(0x00010000); writeInt(0); writeInt(0)
        writeInt(0); writeInt(0x00010000); writeInt(0)
        writeInt(0); writeInt(0); writeInt(0x40000000)
        // pre_defined[6]
        repeat(6) { writeInt(0) }
        writeInt(nextTrackId)
    }

    private fun buildVideoTrakBox(config: VideoTrackConfig, mdatDataStartOffset: Long): ByteArray {
        val deltas = computeSampleDeltas(config.samples, VIDEO_TIMESCALE)
        val mediaDurationTicks = deltas.sumOf { it.toLong() }.coerceAtLeast(1L)
        val movieDurationMs = ((mediaDurationTicks * MOVIE_TIMESCALE) / VIDEO_TIMESCALE).coerceAtLeast(1L)

        return box("trak") {
            write(
                buildTkhdBox(
                    trackId = config.trackId,
                    durationMs = movieDurationMs,
                    width = config.width,
                    height = config.height,
                    isAudio = false
                )
            )
            write(
                box("mdia") {
                    write(buildMdhdBox(VIDEO_TIMESCALE, mediaDurationTicks))
                    write(buildHdlrBox("vide", "VideoHandler"))
                    write(
                        box("minf") {
                            write(buildVmhdBox())
                            write(buildDinfBox())
                            write(
                                buildStblBox(
                                    stsdBytes = buildVideoStsdBox(config.width, config.height, config.sps, config.pps),
                                    deltas = deltas,
                                    samples = config.samples,
                                    includeStss = true,
                                    mdatDataStartOffset = mdatDataStartOffset
                                )
                            )
                        }
                    )
                }
            )
        }
    }

    private fun buildAudioTrakBox(config: AudioTrackConfig, mdatDataStartOffset: Long): ByteArray {
        val timescale = config.sampleRate.toLong().coerceAtLeast(8000L)
        val deltas = computeSampleDeltas(config.samples, timescale)
        val mediaDurationTicks = deltas.sumOf { it.toLong() }.coerceAtLeast(1L)
        val movieDurationMs = ((mediaDurationTicks * MOVIE_TIMESCALE) / timescale).coerceAtLeast(1L)

        return box("trak") {
            write(
                buildTkhdBox(
                    trackId = config.trackId,
                    durationMs = movieDurationMs,
                    width = 0,
                    height = 0,
                    isAudio = true
                )
            )
            write(
                box("mdia") {
                    write(buildMdhdBox(timescale, mediaDurationTicks))
                    write(buildHdlrBox("soun", "SoundHandler"))
                    write(
                        box("minf") {
                            write(buildSmhdBox())
                            write(buildDinfBox())
                            write(
                                buildStblBox(
                                    stsdBytes = buildAudioStsdBox(config.sampleRate, config.channelCount, config.asc),
                                    deltas = deltas,
                                    samples = config.samples,
                                    includeStss = false,
                                    mdatDataStartOffset = mdatDataStartOffset
                                )
                            )
                        }
                    )
                }
            )
        }
    }

    private fun computeSampleDeltas(samples: List<SampleEntryMeta>, timescale: Long): IntArray {
        val n = samples.size
        if (n == 0) return IntArray(0)
        val defaultDelta = (timescale / 30L).toInt().coerceAtLeast(1024)
        if (n == 1) return intArrayOf(defaultDelta)

        val ticks = LongArray(n) { i ->
            (samples[i].ptsUs * timescale) / 1_000_000L
        }
        val deltas = IntArray(n)
        for (i in 0 until n - 1) {
            deltas[i] = (ticks[i + 1] - ticks[i]).toInt().coerceAtLeast(1)
        }
        deltas[n - 1] = deltas[n - 2].coerceAtLeast(1)
        return deltas
    }

    private fun buildTkhdBox(
        trackId: Int,
        durationMs: Long,
        width: Int,
        height: Int,
        isAudio: Boolean
    ): ByteArray = fullBox("tkhd", 0, 0x000003) {
        writeInt(0) // creation_time
        writeInt(0) // modification_time
        writeInt(trackId)
        writeInt(0) // reserved
        writeInt(durationMs.coerceAtLeast(1L).toInt())
        writeInt(0) // reserved
        writeInt(0) // reserved
        writeShort(0) // layer
        writeShort(if (isAudio) 1 else 0) // alternate_group
        writeShort(if (isAudio) 0x0100 else 0) // volume
        writeShort(0) // reserved
        // Unity matrix
        writeInt(0x00010000); writeInt(0); writeInt(0)
        writeInt(0); writeInt(0x00010000); writeInt(0)
        writeInt(0); writeInt(0); writeInt(0x40000000)
        writeInt(width shl 16)
        writeInt(height shl 16)
    }

    private fun buildMdhdBox(timescale: Long, durationTicks: Long): ByteArray = fullBox("mdhd", 0, 0) {
        writeInt(0) // creation_time
        writeInt(0) // modification_time
        writeInt(timescale.toInt())
        writeInt(durationTicks.coerceAtLeast(1L).toInt())
        writeShort(0x55C4) // language = "und"
        writeShort(0) // pre_defined
    }

    private fun buildHdlrBox(handlerType: String, name: String): ByteArray = fullBox("hdlr", 0, 0) {
        writeInt(0) // pre_defined
        writeBytes(handlerType.take(4).padEnd(4, ' '))
        writeInt(0) // reserved[0]
        writeInt(0) // reserved[1]
        writeInt(0) // reserved[2]
        write(name.toByteArray(Charsets.UTF_8))
        writeByte(0) // null terminator
    }

    private fun buildVmhdBox(): ByteArray = fullBox("vmhd", 0, 1) {
        writeShort(0) // graphicsmode
        writeShort(0) // opcolor[0]
        writeShort(0) // opcolor[1]
        writeShort(0) // opcolor[2]
    }

    private fun buildSmhdBox(): ByteArray = fullBox("smhd", 0, 0) {
        writeShort(0) // balance
        writeShort(0) // reserved
    }

    private fun buildDinfBox(): ByteArray = box("dinf") {
        write(
            fullBox("dref", 0, 0) {
                writeInt(1) // entry_count = 1
                write(fullBox("url ", 0, 1) {})
            }
        )
    }

    private fun buildVideoStsdBox(width: Int, height: Int, sps: ByteArray, pps: ByteArray): ByteArray =
        fullBox("stsd", 0, 0) {
            writeInt(1) // entry_count = 1
            write(
                box("avc1") {
                    write(ByteArray(6)) // reserved
                    writeShort(1) // data_reference_index = 1
                    writeShort(0) // pre_defined
                    writeShort(0) // reserved
                    writeInt(0); writeInt(0); writeInt(0) // pre_defined[3]
                    writeShort(width)
                    writeShort(height)
                    writeInt(0x00480000) // horizresolution = 72 dpi
                    writeInt(0x00480000) // vertresolution = 72 dpi
                    writeInt(0) // reserved
                    writeShort(1) // frame_count = 1
                    write(ByteArray(32)) // compressorname
                    writeShort(0x0018) // depth = 24
                    writeShort(0xFFFF) // pre_defined = -1
                    write(buildAvcCBox(sps, pps))
                }
            )
        }

    private fun buildAvcCBox(sps: ByteArray, pps: ByteArray): ByteArray = box("avcC") {
        val profileIdc = if (sps.size > 1) (sps[1].toInt() and 0xFF) else 66
        val profileCompat = if (sps.size > 2) (sps[2].toInt() and 0xFF) else 0x80
        val levelIdc = if (sps.size > 3) (sps[3].toInt() and 0xFF) else 31

        writeByte(1) // configurationVersion = 1
        writeByte(profileIdc)
        writeByte(profileCompat)
        writeByte(levelIdc)
        writeByte(0xFF) // 4-byte NAL unit length prefix
        writeByte(0xE1) // 1 SPS
        writeShort(sps.size)
        write(sps)
        writeByte(1) // 1 PPS
        writeShort(pps.size)
        write(pps)

        if (profileIdc == 100 || profileIdc == 110 || profileIdc == 122 || profileIdc == 244 ||
            profileIdc == 44 || profileIdc == 83 || profileIdc == 86 || profileIdc == 118 ||
            profileIdc == 128 || profileIdc == 138
        ) {
            writeByte(0xFC) // chroma_format_idc = 1 (4:2:0)
            writeByte(0xF8) // bit_depth_luma_minus8 = 0
            writeByte(0xF8) // bit_depth_chroma_minus8 = 0
            writeByte(0x00) // numOfSequenceParameterSetExt = 0
        }
    }

    private fun buildAudioStsdBox(sampleRate: Int, channelCount: Int, asc: ByteArray): ByteArray =
        fullBox("stsd", 0, 0) {
            writeInt(1) // entry_count = 1
            write(
                box("mp4a") {
                    write(ByteArray(6)) // reserved
                    writeShort(1) // data_reference_index = 1
                    writeInt(0) // reserved[0]
                    writeInt(0) // reserved[1]
                    writeShort(channelCount)
                    writeShort(16) // samplesize = 16
                    writeShort(0) // pre_defined
                    writeShort(0) // reserved
                    writeInt(sampleRate.coerceAtMost(65535) shl 16)
                    write(buildEsdsBox(asc))
                }
            )
        }

    private fun buildEsdsBox(asc: ByteArray): ByteArray = fullBox("esds", 0, 0) {
        val ascBytes = if (asc.isNotEmpty()) asc else byteArrayOf(0x12, 0x10)
        // ES_Descriptor (tag 0x03)
        writeByte(0x03)
        writeByte(23 + ascBytes.size)
        writeShort(1) // ES_ID = 1
        writeByte(0) // flags = 0
        // DecoderConfigDescriptor (tag 0x04)
        writeByte(0x04)
        writeByte(15 + ascBytes.size)
        writeByte(0x40) // objectTypeIndication = Audio ISO/IEC 14496-3 (AAC)
        writeByte(0x15) // streamType = AudioStream (0x05 << 2 | 1)
        writeByte(0); writeByte(0); writeByte(0) // bufferSizeDB
        writeInt(192000) // maxBitrate
        writeInt(192000) // avgBitrate
        // DecoderSpecificInfo (tag 0x05)
        writeByte(0x05)
        writeByte(ascBytes.size)
        write(ascBytes)
        // SLConfigDescriptor (tag 0x06)
        writeByte(0x06)
        writeByte(1)
        writeByte(0x02)
    }

    private fun buildStblBox(
        stsdBytes: ByteArray,
        deltas: IntArray,
        samples: List<SampleEntryMeta>,
        includeStss: Boolean,
        mdatDataStartOffset: Long
    ): ByteArray = box("stbl") {
        write(stsdBytes)

        // stts (run-length encoded sample deltas)
        val rleCounts = ArrayList<Int>()
        val rleDeltas = ArrayList<Int>()
        for (d in deltas) {
            if (rleDeltas.isNotEmpty() && rleDeltas.last() == d) {
                rleCounts[rleCounts.lastIndex] = rleCounts.last() + 1
            } else {
                rleCounts.add(1)
                rleDeltas.add(d)
            }
        }
        write(
            fullBox("stts", 0, 0) {
                writeInt(rleCounts.size)
                for (i in rleCounts.indices) {
                    writeInt(rleCounts[i])
                    writeInt(rleDeltas[i])
                }
            }
        )

        // stss (sync sample table for video keyframes)
        if (includeStss) {
            val syncIndices = ArrayList<Int>()
            for (i in samples.indices) {
                if (samples[i].isSync) {
                    syncIndices.add(i + 1) // 1-based sample number
                }
            }
            if (syncIndices.isEmpty() && samples.isNotEmpty()) {
                syncIndices.add(1)
            }
            write(
                fullBox("stss", 0, 0) {
                    writeInt(syncIndices.size)
                    for (idx in syncIndices) {
                        writeInt(idx)
                    }
                }
            )
        }

        // stsc (1 sample per chunk)
        write(
            fullBox("stsc", 0, 0) {
                writeInt(1) // entry_count = 1
                writeInt(1) // first_chunk = 1
                writeInt(1) // samples_per_chunk = 1
                writeInt(1) // sample_description_index = 1
            }
        )

        // stsz (individual sample sizes)
        write(
            fullBox("stsz", 0, 0) {
                writeInt(0) // sample_size = 0 (variable)
                writeInt(samples.size)
                for (s in samples) {
                    writeInt(s.size)
                }
            }
        )

        // stco (chunk offsets, 1 chunk per sample)
        write(
            fullBox("stco", 0, 0) {
                writeInt(samples.size)
                for (s in samples) {
                    writeInt((mdatDataStartOffset + s.offsetInMdat).toInt())
                }
            }
        )
    }

    private inline fun box(type: String, block: DataOutputStream.() -> Unit): ByteArray {
        val payload = ByteArrayOutputStream()
        DataOutputStream(payload).use { it.block() }
        val payloadBytes = payload.toByteArray()
        val out = ByteArrayOutputStream(8 + payloadBytes.size)
        DataOutputStream(out).use { dos ->
            dos.writeInt(8 + payloadBytes.size)
            dos.writeBytes(type.take(4).padEnd(4, ' '))
            dos.write(payloadBytes)
        }
        return out.toByteArray()
    }

    private inline fun fullBox(
        type: String,
        version: Int,
        flags: Int,
        block: DataOutputStream.() -> Unit
    ): ByteArray = box(type) {
        writeInt(((version and 0xFF) shl 24) or (flags and 0x00FFFFFF))
        block()
    }
}
