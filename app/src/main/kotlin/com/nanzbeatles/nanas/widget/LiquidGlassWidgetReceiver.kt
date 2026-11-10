/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.nanzbeatles.nanas.MainActivity
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.playback.MusicService

class LiquidGlassWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_LIQUID_GLASS_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        } else {
            appWidgetIds.forEach { widgetId ->
                val views = buildDefaultRemoteViews(context)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_LIQUID_GLASS_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_LIQUID_GLASS_PLAY_PAUSE,
            ACTION_LIQUID_GLASS_LIKE,
            ACTION_LIQUID_GLASS_NEXT,
            ACTION_LIQUID_GLASS_PREVIOUS -> {
                val mappedAction = when (intent.action) {
                    ACTION_LIQUID_GLASS_PLAY_PAUSE -> MusicWidgetReceiver.ACTION_PLAY_PAUSE
                    ACTION_LIQUID_GLASS_LIKE -> MusicWidgetReceiver.ACTION_LIKE
                    ACTION_LIQUID_GLASS_NEXT -> MusicWidgetReceiver.ACTION_NEXT
                    ACTION_LIQUID_GLASS_PREVIOUS -> MusicWidgetReceiver.ACTION_PREVIOUS
                    else -> return
                }
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = mappedAction
                    putExtras(intent)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        const val ACTION_LIQUID_GLASS_PLAY_PAUSE = "com.nanzbeatles.nanas.widget.LIQUID_GLASS_PLAY_PAUSE"
        const val ACTION_LIQUID_GLASS_LIKE = "com.nanzbeatles.nanas.widget.LIQUID_GLASS_LIKE"
        const val ACTION_LIQUID_GLASS_NEXT = "com.nanzbeatles.nanas.widget.LIQUID_GLASS_NEXT"
        const val ACTION_LIQUID_GLASS_PREVIOUS = "com.nanzbeatles.nanas.widget.LIQUID_GLASS_PREVIOUS"
        const val ACTION_LIQUID_GLASS_UPDATE = "com.nanzbeatles.nanas.widget.LIQUID_GLASS_UPDATE"

        fun buildDefaultRemoteViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_liquid_glass)
            views.setTextViewText(R.id.widget_glass_song_title, context.getString(R.string.no_song_playing))
            views.setTextViewText(R.id.widget_glass_artist_name, context.getString(R.string.tap_to_open))
            views.setImageViewResource(R.id.widget_glass_play_pause, R.drawable.ic_widget_play)
            views.setImageViewResource(R.id.widget_glass_like_button, R.drawable.ic_widget_heart_outline_nav)
            views.setProgressBar(R.id.widget_glass_progress, 10000, 0, false)

            val openIntent = PendingIntent.getActivity(
                context,
                200,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_glass_album_art, openIntent)
            views.setOnClickPendingIntent(R.id.widget_glass_open_app, openIntent)
            views.setOnClickPendingIntent(R.id.widget_glass_play_pause, getActionIntent(context, ACTION_LIQUID_GLASS_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_glass_previous, getActionIntent(context, ACTION_LIQUID_GLASS_PREVIOUS))
            views.setOnClickPendingIntent(R.id.widget_glass_next, getActionIntent(context, ACTION_LIQUID_GLASS_NEXT))
            views.setOnClickPendingIntent(R.id.widget_glass_like_button, getActionIntent(context, ACTION_LIQUID_GLASS_LIKE))
            return views
        }

        private fun getActionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, LiquidGlassWidgetReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
