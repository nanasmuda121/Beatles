/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import android.os.Process
import com.nanzbeatles.nanas.BuildConfig
import java.io.File

/**
 * Runtime integrity protection and encoded string vault for release builds.
 *
 * - Blocks active JDWP debugger attachment, native ptrace tracers, and apktool
 *   debuggable-flag repackaging in non-debug builds without impacting normal users.
 * - Decodes sensitive backend endpoints only in memory at runtime so static DEX
 *   string scans do not expose plaintext URLs.
 */
object SecurityGuard {

    private fun decode(data: IntArray, seed: Int): String {
        val bytes = ByteArray(data.size)
        for (i in data.indices) {
            val mask = (seed xor ((i * 37 + 19) and 0xFF)) and 0xFF
            bytes[i] = (data[i] xor mask).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    val updateJsonUrl: String by lazy {
        decode(
            intArrayOf(
                220, 235, 142, 85, 115, 81, 121, 158, 238, 166, 85, 35, 15, 58, 202,
                241, 177, 77, 127, 6, 53, 201, 133, 174, 66, 99, 23, 51, 204, 205,
                173, 70, 121, 80, 52, 228, 142, 170, 69, 124, 9, 195, 227, 154, 163,
                68, 117, 14, 207, 160, 136, 176, 81, 111, 42, 196, 255, 216, 191, 92,
                113, 45, 129, 255, 145, 173, 73, 12, 47, 197, 184, 155, 175, 104, 12
            ),
            0xA7
        )
    }

    val vocalRemoverApiBase: String by lazy {
        decode(
            intArrayOf(
                38, 17, 116, 175, 137, 171, 131, 100, 7, 77, 177, 217, 228, 196, 55,
                77, 87, 187, 134, 234, 216, 50, 121, 90, 191, 195, 235, 200, 47
            ),
            0x5D
        )
    }

    val vocalRemoverOrigin: String by lazy {
        decode(
            intArrayOf(
                16, 39, 66, 153, 191, 157, 181, 82, 34, 110, 131, 174, 210, 250, 92,
                56, 125, 144, 175, 218
            ),
            0x6B
        )
    }

    val vocalRemoverReferer: String by lazy {
        decode(
            intArrayOf(
                232, 223, 186, 97, 71, 101, 77, 170, 218, 150, 123, 86, 42, 2, 164,
                192, 133, 104, 87, 34, 75, 230, 182, 218, 110, 76, 37, 8, 224, 250,
                136, 120, 77, 36, 24, 212, 166
            ),
            0x93
        )
    }

    val bgRemoverPageUrl: String by lazy {
        decode(
            intArrayOf(
                190, 137, 236, 55, 17, 51, 27, 252, 137, 210, 55, 65, 99, 93, 179,
                141, 195, 36, 5, 112, 28, 186, 235, 206, 97, 7, 117, 82, 181, 247,
                201, 102, 20, 124, 91, 140, 229, 219, 59, 6, 112, 161
            ),
            0xC5
        )
    }

    val bgRemoverOrigin: String by lazy {
        decode(
            intArrayOf(
                154, 173, 200, 19, 53, 23, 63, 216, 173, 246, 19, 101, 71, 121, 151,
                169, 231, 0, 33, 84, 56, 158, 207, 234
            ),
            0xE1
        )
    }

    val bgRemoverHostSuffix: String by lazy {
        decode(
            intArrayOf(2, 110, 14, 210, 238, 150, 167, 68, 99, 113, 217, 250, 157),
            0x3F
        )
    }

    /**
     * Checks for active reverse-engineering tampering in release builds only:
     * 1. APK manifest patched with android:debuggable="true" on a release build.
     * 2. Active JDWP debugger attached to the process.
     * 3. Active ptrace tracer (TracerPid > 0) attached to the process.
     */
    fun verifyRuntimeIntegrity(context: Context) {
        if (BuildConfig.DEBUG) return

        val isDebuggableFlagSet =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val isDebuggerActive = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        val isTraced = hasActiveTracerPid()

        if (isDebuggableFlagSet || isDebuggerActive || isTraced) {
            Process.killProcess(Process.myPid())
        }
    }

    private fun hasActiveTracerPid(): Boolean = runCatching {
        val statusFile = File("/proc/self/status")
        if (!statusFile.canRead()) return@runCatching false
        statusFile.useLines { lines ->
            val tracerLine = lines.firstOrNull { it.startsWith("TracerPid:") }
                ?: return@useLines false
            val pid = tracerLine.substringAfter(":").trim().toIntOrNull() ?: 0
            pid > 0
        }
    }.getOrDefault(false)
}
