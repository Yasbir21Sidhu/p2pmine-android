package com.hashcore.pool.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hashcore.pool.R
import com.hashcore.pool.presentation.MainActivity
import com.hashcore.pool.util.BtcFormatter
import com.hashcore.pool.util.HashrateFormatter
import com.hashcore.pool.util.TimeFormatter

/**
 * Home-screen widget showing last-known hashrate / unpaid balance.
 * Data is written when the dashboard loads in-app (WidgetSnapshotStore).
 */
class PoolStatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PoolStatsWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("p2pmine_widget", Context.MODE_PRIVATE)
            val hashrate = prefs.getFloat("hashrate_hs", 0f).toDouble()
            val unpaid = prefs.getFloat("unpaid_btc", 0f).toDouble()
            val online = prefs.getInt("workers_online", 0)
            val total = prefs.getInt("workers_total", 0)
            val updated = prefs.getLong("updated_at", 0L)

            val views = RemoteViews(context.packageName, R.layout.widget_pool_stats)
            views.setTextViewText(R.id.widget_hashrate, HashrateFormatter.format(hashrate))
            views.setTextViewText(R.id.widget_unpaid, BtcFormatter.btc(unpaid, compact = true))
            views.setTextViewText(
                R.id.widget_workers,
                if (total > 0) "$online / $total online" else "Open app to sync",
            )
            views.setTextViewText(
                R.id.widget_updated,
                if (updated > 0) "Updated ${TimeFormatter.relative(updated)}" else "Not synced yet",
            )

            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            return views
        }
    }
}
