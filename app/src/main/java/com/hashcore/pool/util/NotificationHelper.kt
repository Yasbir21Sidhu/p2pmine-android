package com.hashcore.pool.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hashcore.pool.domain.repository.MinerAlert
import com.hashcore.pool.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local notification surface for important pool events.
 * FCM can call the same channels once google-services is wired.
 *
 * Channels:
 * - worker_alerts: offline / high reject
 * - payout_alerts: payout sent / confirmed
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_WORKERS = "worker_alerts"
        const val CHANNEL_PAYOUTS = "payout_alerts"
        const val GROUP_WORKERS = "p2pmine_workers"
        const val GROUP_PAYOUTS = "p2pmine_payouts"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WORKERS,
                "Worker alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Worker offline and high reject rate alerts"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PAYOUTS,
                "Payout alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Payout requested, sent, and confirmed"
            },
        )
    }

    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notifyAlert(alert: MinerAlert, mutedUntilEpochMs: Long = 0L) {
        if (mutedUntilEpochMs > System.currentTimeMillis()) return
        ensureChannels()
        val isPayout = alert.type.contains("payout", ignoreCase = true)
        val channel = if (isPayout) CHANNEL_PAYOUTS else CHANNEL_WORKERS
        val group = if (isPayout) GROUP_PAYOUTS else GROUP_WORKERS
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = when {
            alert.type.contains("offline", true) -> "Worker offline"
            alert.type.contains("reject", true) -> "High reject rate"
            alert.type.contains("payout", true) -> "Payout update"
            else -> "P2PMine alert"
        }
        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setGroup(group)
            .build()

        val summary = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("P2PMine")
            .setContentText(if (isPayout) "Payout updates" else "Worker alerts")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(if (isPayout) "Payouts" else "Workers"),
            )
            .setGroup(group)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        try {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(alert.id.hashCode(), notif)
            nm.notify(if (isPayout) GROUP_PAYOUTS.hashCode() else GROUP_WORKERS.hashCode(), summary)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }
}
