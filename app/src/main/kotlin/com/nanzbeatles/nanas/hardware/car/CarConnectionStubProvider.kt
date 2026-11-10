/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.hardware.car

import android.app.UiModeManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * In-process ContentProvider for `content://androidx.car.app.connection`.
 *
 * Media3's `AndroidAutoConnectionStateObserver` queries `content://androidx.car.app.connection`
 * for the `CarConnectionState` column whenever a `MediaSession` / `MediaLibrarySession` is created.
 * On devices or emulators without the Android Auto host projection provider installed,
 * `ActivityThread.acquireProvider` logs `E/ActivityThread: Failed to find provider info for androidx.car.app.connection`.
 *
 * Providing this unexported in-process provider satisfies the query cleanly and reports the current
 * car UI mode state (`1` when in car mode, `0` when not connected).
 */
class CarConnectionStubProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val columnName = projection?.firstOrNull() ?: COLUMN_CAR_CONNECTION_STATE
        val cursor = MatrixCursor(arrayOf(columnName), 1)
        val uiModeManager = context?.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isCarMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR
        cursor.addRow(arrayOf(if (isCarMode) CONNECTION_TYPE_NATIVE else CONNECTION_TYPE_NOT_CONNECTED))
        return cursor
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val COLUMN_CAR_CONNECTION_STATE = "CarConnectionState"
        private const val CONNECTION_TYPE_NOT_CONNECTED = 0
        private const val CONNECTION_TYPE_NATIVE = 1
    }
}
