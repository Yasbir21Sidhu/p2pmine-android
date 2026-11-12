package com.hashcore.pool.data.local.prefs

import android.content.Context
import androidx.core.content.edit
import com.hashcore.pool.domain.model.Dashboard
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Last-known dashboard stats for the home-screen widget (no network from widget process). */
@Singleton
class WidgetSnapshotStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("p2pmine_widget", Context.MODE_PRIVATE)

    fun save(dashboard: Dashboard) {
        prefs.edit {
            putFloat("hashrate_hs", dashboard.currentHashrateHs.toFloat())
            putFloat("avg24_hs", dashboard.avg24hHashrateHs.toFloat())
            putFloat("unpaid_btc", dashboard.unpaidBalanceBtc.toFloat())
            putInt("workers_online", dashboard.workers.online)
            putInt("workers_total", dashboard.workers.total)
            putLong("updated_at", System.currentTimeMillis())
        }
    }

    fun hashrateHs(): Double = prefs.getFloat("hashrate_hs", 0f).toDouble()
    fun avg24Hs(): Double = prefs.getFloat("avg24_hs", 0f).toDouble()
    fun unpaidBtc(): Double = prefs.getFloat("unpaid_btc", 0f).toDouble()
    fun workersOnline(): Int = prefs.getInt("workers_online", 0)
    fun workersTotal(): Int = prefs.getInt("workers_total", 0)
    fun updatedAt(): Long = prefs.getLong("updated_at", 0L)
}
