/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.nanzbeatles.nanas.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ExoDownloadService : DownloadService(
    NOTIFICATION_ID,
    1000L,
    CHANNEL_ID,
    R.string.downloading,
    0
) {
    @Inject
    lateinit var downloadUtil: DownloadUtil

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == REMOVE_ALL_PENDING_DOWNLOADS) {
            downloadManager.currentDownloads.forEach { download ->
                downloadManager.removeDownload(download.request.id)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getDownloadManager() = downloadUtil.downloadManager

    override fun getScheduler(): Scheduler? = runCatching {
        PlatformScheduler(this, JOB_ID)
    }.getOrNull()

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val message = when {
            downloads.size == 1 -> {
                val data = downloads[0].request.data
                if (data.isNotEmpty()) Util.fromUtf8Bytes(data) else getString(R.string.downloading)
            }
            downloads.isNotEmpty() -> {
                resources.getQuantityString(R.plurals.n_song, downloads.size, downloads.size)
            }
            else -> getString(R.string.downloading)
        }
        val baseNotification = downloadUtil.downloadNotificationHelper.buildProgressNotification(
            this,
            R.drawable.download,
            null,
            message,
            downloads,
            notMetRequirements
        )
        return Notification.Builder.recoverBuilder(this, baseNotification)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.close),
                    getString(android.R.string.cancel),
                    PendingIntent.getService(
                        this,
                        0,
                        Intent(this, ExoDownloadService::class.java).setAction(
                            REMOVE_ALL_PENDING_DOWNLOADS
                        ),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            ).build()
    }


    /**
     * This helper will outlive the lifespan of a single instance of [ExoDownloadService]
     */
    class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private var nextNotificationId: Int,
    ) : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_FAILED) {
                val notification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.error,
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "download"
        const val NOTIFICATION_ID = 1
        const val JOB_ID = 1
        const val REMOVE_ALL_PENDING_DOWNLOADS = "REMOVE_ALL_PENDING_DOWNLOADS"
    }
}
