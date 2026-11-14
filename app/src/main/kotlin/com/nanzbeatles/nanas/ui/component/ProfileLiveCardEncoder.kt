/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nanzbeatles.nanas.lyricvideo.Mp4ContainerWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * High-performance on-device encoder for Beatles Live Profile Cards.
 * Encodes a 720x1360 30fps H.264 video loop of the user's music journey stats,
 * and packages it into either a standalone social video story (MP4) or
 * a Google Camera-compliant Motion Photo (.jpg).
 */
object ProfileLiveCardEncoder {

    private const val TAG = "ProfileLiveCardEncoder"
    private const val MIME_TYPE_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val VIDEO_WIDTH = 720
    private const val VIDEO_HEIGHT = 1360
    private const val VIDEO_FPS = 30
    private const val VIDEO_DURATION_MS = 3000L
    private const val VIDEO_BITRATE = 3_500_000
    private const val TIMEOUT_US = 10_000L

    private fun fmtInt(v: Int): String =
        NumberFormat.getIntegerInstance(Locale("id", "ID")).format(v)

    /**
     * Encodes a 3-second animated live card video (720x1360 H.264 MP4).
     */
    suspend fun encodeLiveCardVideo(
        context: Context,
        data: ProfileShareCanvasData,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.Default) {
        val cacheDir = File(context.cacheDir, "live_profile_cards").apply { mkdirs() }
        val outputFile = File(cacheDir, "Beatles_LiveCard_${System.currentTimeMillis()}.mp4")
        val muxSession = Mp4ContainerWriter.Mp4MuxSession(
            outputFile = outputFile,
            videoWidth = VIDEO_WIDTH,
            videoHeight = VIDEO_HEIGHT
        )

        var encoder: MediaCodec? = null
        try {
            val totalFrames = ((VIDEO_DURATION_MS / 1000.0) * VIDEO_FPS).toInt().coerceAtLeast(30)
            val frameDurationUs = 1_000_000L / VIDEO_FPS

            val videoFormat = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val enc = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO)
            encoder = enc
            enc.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            enc.start()

            val argbPixels = IntArray(VIDEO_WIDTH * VIDEO_HEIGHT)
            val bufferInfo = MediaCodec.BufferInfo()
            var currentFrame = 0
            var isInputEOS = false
            var isOutputEOS = false
            var lastActivityNs = System.nanoTime()

            Timber.tag(TAG).d("Starting Live Profile Card encoding: $totalFrames frames")

            while (!isOutputEOS && coroutineContext.isActive) {
                if (System.nanoTime() - lastActivityNs > 25_000_000_000L) {
                    error("Encoder video live card tidak merespons...")
                }

                if (!isInputEOS) {
                    val inputBufferIndex = enc.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        lastActivityNs = System.nanoTime()
                        if (currentFrame >= totalFrames) {
                            enc.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                currentFrame * frameDurationUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputEOS = true
                        } else {
                            val timeMs = (currentFrame * 1000L) / VIDEO_FPS
                            val ptsUs = currentFrame * frameDurationUs

                            val frameBitmap = ProfileShareCanvasRenderer.renderBitmap(
                                context = context,
                                data = data,
                                timeMs = timeMs,
                                targetW = VIDEO_WIDTH,
                                targetH = VIDEO_HEIGHT
                            )
                            frameBitmap.getPixels(argbPixels, 0, VIDEO_WIDTH, 0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
                            if (frameBitmap != null && !frameBitmap.isRecycled) {
                                frameBitmap.recycle()
                            }

                            val inputImage = enc.getInputImage(inputBufferIndex)
                            if (inputImage != null) {
                                copyArgbToImageYuv(argbPixels, inputImage, VIDEO_WIDTH, VIDEO_HEIGHT)
                                enc.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    inputImage.planes[0].buffer.remaining(),
                                    ptsUs,
                                    0
                                )
                            } else {
                                val inputBuffer = enc.getInputBuffer(inputBufferIndex)
                                if (inputBuffer != null) {
                                    copyArgbToNv12(argbPixels, inputBuffer, VIDEO_WIDTH, VIDEO_HEIGHT)
                                    enc.queueInputBuffer(
                                        inputBufferIndex,
                                        0,
                                        VIDEO_WIDTH * VIDEO_HEIGHT * 3 / 2,
                                        ptsUs,
                                        0
                                    )
                                }
                            }

                            currentFrame++
                            val prog = (currentFrame.toFloat() / totalFrames).coerceIn(0f, 0.95f)
                            withContext(Dispatchers.Main) {
                                onProgress(prog)
                            }
                        }
                    }
                }

                val outputBufferIndex = enc.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    muxSession.setVideoFormat(enc.outputFormat)
                } else if (outputBufferIndex >= 0) {
                    lastActivityNs = System.nanoTime()
                    val outputBuffer = enc.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (isConfig) {
                            val configBytes = ByteArray(bufferInfo.size)
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.get(configBytes)
                            muxSession.feedVideoCodecConfig(configBytes)
                        } else {
                            val sampleBytes = ByteArray(bufferInfo.size)
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.get(sampleBytes)
                            val isKey = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                            muxSession.writeVideoSample(sampleBytes, bufferInfo.presentationTimeUs, isKey)
                        }
                    }
                    enc.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                    }
                }
            }

            runCatching { enc.stop() }
            runCatching { enc.release() }
            encoder = null

            val success = muxSession.finish()
            if (success && outputFile.exists() && outputFile.length() > 2048L) {
                withContext(Dispatchers.Main) { onProgress(1.0f) }
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error encoding live profile card")
            runCatching { encoder?.release() }
            outputFile.delete()
            null
        }
    }

    /**
     * Packages a full-res JPEG cover and the rendered MP4 into a Google Camera Motion Photo (.jpg).
     */
    suspend fun createMotionPhoto(
        context: Context,
        coverBitmap: Bitmap,
        mp4File: File,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val cacheDir = File(context.cacheDir, "motion_photos").apply { mkdirs() }
            val outputFile = File(cacheDir, "Beatles_MotionPhoto_${System.currentTimeMillis()}.jpg")

            val jpegBytes = ByteArrayOutputStream().use { baos ->
                coverBitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
                baos.toByteArray()
            }
            if (jpegBytes.size < 4 || (jpegBytes[0].toInt() and 0xFF) != 0xFF || (jpegBytes[1].toInt() and 0xFF) != 0xD8) {
                return@runCatching null
            }

            val mp4Length = mp4File.length()
            val xmpXml = "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
                "<rdf:Description rdf:about=\"\" " +
                "xmlns:GCamera=\"http://ns.google.com/photos/1.0/camera/\" " +
                "xmlns:Container=\"http://ns.google.com/photos/1.0/container/\" " +
                "xmlns:Item=\"http://ns.google.com/photos/1.0/container/item/\" " +
                "GCamera:MotionPhoto=\"1\" " +
                "GCamera:MotionPhotoVersion=\"1\" " +
                "GCamera:MotionPhotoPresentationTimestampUs=\"0\">" +
                "<Container:Directory>" +
                "<rdf:Seq>" +
                "<rdf:li rdf:parseType=\"Resource\">" +
                "<Item:Mime>image/jpeg</Item:Mime>" +
                "<Item:Semantic>Primary</Item:Semantic>" +
                "<Item:Length>0</Item:Length>" +
                "<Item:Padding>0</Item:Padding>" +
                "</rdf:li>" +
                "<rdf:li rdf:parseType=\"Resource\">" +
                "<Item:Mime>video/mp4</Item:Mime>" +
                "<Item:Semantic>MotionPhoto</Item:Semantic>" +
                "<Item:Length>$mp4Length</Item:Length>" +
                "<Item:Padding>0</Item:Padding>" +
                "</rdf:li>" +
                "</rdf:Seq>" +
                "</Container:Directory>" +
                "</rdf:Description>" +
                "</rdf:RDF>" +
                "</x:xmpmeta>"

            val xmpHeader = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)
            val xmpPayload = xmpXml.toByteArray(Charsets.UTF_8)
            val app1PayloadLength = xmpHeader.size + xmpPayload.size + 2
            val app1Marker = byteArrayOf(
                0xFF.toByte(), 0xE1.toByte(),
                ((app1PayloadLength shr 8) and 0xFF).toByte(),
                (app1PayloadLength and 0xFF).toByte()
            )

            FileOutputStream(outputFile).use { fos ->
                fos.write(0xFF)
                fos.write(0xD8)
                fos.write(app1Marker)
                fos.write(xmpHeader)
                fos.write(xmpPayload)
                fos.write(jpegBytes, 2, jpegBytes.size - 2)

                FileInputStream(mp4File).use { fis ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    while (fis.read(buf).also { read = it } != -1) {
                        fos.write(buf, 0, read)
                    }
                }
                fos.flush()
            }

            if (outputFile.exists() && outputFile.length() > mp4Length) outputFile else null
        }.getOrNull()
    }

    /**
     * Shares the MP4 video to social media (Instagram Stories, WhatsApp Status, TikTok, etc.).
     */
    suspend fun shareLiveVideo(
        context: Context,
        videoFile: File,
        data: ProfileShareCanvasData,
    ) = withContext(Dispatchers.Main) {
        runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                videoFile
            )

            val levelSpec = data.journeyState.levelSpec
            val caption = buildString {
                appendLine("🎵 Foto Live Statistik Musik — ${data.displayName} (${data.displayHandle})")
                appendLine("🏆 Level ${levelSpec.level}: ${levelSpec.title}")
                append("🔥 Streak: ${data.streakState.currentStreak} Hari • 🎧 Total: ${fmtInt(data.journeyState.totalHours)} Jam • Diputar di Beatles")
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newRawUri("Foto Live Statistik Profil", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(sendIntent, "Bagikan Foto Live / Video Story")
            )
        }.onFailure {
            Toast.makeText(context, "Gagal membagikan foto live: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves a generated media file to the device's public Gallery (Pictures or Movies).
     */
    suspend fun saveFileToGallery(
        context: Context,
        file: File,
        mimeType: String,
        displayName: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val isVideo = mimeType.startsWith("video/")
            val targetUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val targetDir = if (isVideo) Environment.DIRECTORY_MOVIES + "/Beatles" else Environment.DIRECTORY_PICTURES + "/Beatles"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(targetUri, values) ?: return@runCatching false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                    out.flush()
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                )
                val outFolder = File(publicDir, "Beatles").apply { mkdirs() }
                val targetFile = File(outFolder, displayName)
                file.copyTo(targetFile, overwrite = true)
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = Uri.fromFile(targetFile)
                }
                context.sendBroadcast(intent)
                true
            }
        }.getOrDefault(false)
    }

    private fun copyArgbToImageYuv(argb: IntArray, image: Image, width: Int, height: Int) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        for (y in 0 until height) {
            val yOffset = y * yRowStride
            val argbRowOffset = y * width
            for (x in 0 until width) {
                val c = argb[argbRowOffset + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuffer.put(yOffset + x, yVal.coerceIn(0, 255).toByte())
            }
        }

        for (y in 0 until height / 2) {
            val uRowOffset = y * uRowStride
            val vRowOffset = y * vRowStride
            val argbRowOffset = (y * 2) * width
            for (x in 0 until width / 2) {
                val c = argb[argbRowOffset + (x * 2)]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                uBuffer.put(uRowOffset + (x * uPixelStride), uVal.coerceIn(0, 255).toByte())
                vBuffer.put(vRowOffset + (x * vPixelStride), vVal.coerceIn(0, 255).toByte())
            }
        }
    }

    private fun copyArgbToNv12(argb: IntArray, buffer: ByteBuffer, width: Int, height: Int) {
        buffer.clear()
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val c = argb[rowOffset + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                buffer.put(yIndex++, yVal.coerceIn(0, 255).toByte())

                if (y % 2 == 0 && x % 2 == 0) {
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    buffer.put(uvIndex++, uVal.coerceIn(0, 255).toByte())
                    buffer.put(uvIndex++, vVal.coerceIn(0, 255).toByte())
                }
            }
        }
    }
}
