package com.hashcore.pool.data.repository

import com.hashcore.pool.BuildConfig
import com.hashcore.pool.data.local.datastore.UserPreferences
import com.hashcore.pool.data.remote.DemoData
import com.hashcore.pool.data.remote.NetworkErrorMapper
import com.hashcore.pool.data.remote.api.HashCoreApi
import android.util.Log
import com.hashcore.pool.data.remote.dto.HashrateHistoryParser
import com.hashcore.pool.data.remote.dto.PayoutRequestDto
import com.hashcore.pool.data.remote.dto.WorkerDto
import com.hashcore.pool.data.remote.dto.avg1hHs
import com.hashcore.pool.data.remote.dto.avg24hHs
import com.hashcore.pool.data.remote.dto.matchesIdOrName
import com.hashcore.pool.data.remote.dto.payoutList
import com.hashcore.pool.data.remote.dto.peak24hHs
import com.hashcore.pool.data.remote.dto.sameWorkerAs
import com.hashcore.pool.data.remote.dto.synthesizeWorkerHistory
import com.hashcore.pool.data.remote.dto.syntheticEarnings
import com.hashcore.pool.data.remote.dto.toDomain
import com.hashcore.pool.data.remote.dto.toDomainPoints
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.Dashboard
import com.hashcore.pool.domain.model.EarningsBucket
import com.hashcore.pool.domain.model.EarningsPeriod
import com.hashcore.pool.domain.model.HashratePoint
import com.hashcore.pool.domain.model.Payout
import com.hashcore.pool.domain.model.PoolPublicSettings
import com.hashcore.pool.domain.model.Worker
import com.hashcore.pool.domain.repository.DashboardRepository
import com.hashcore.pool.domain.repository.EarningsRepository
import com.hashcore.pool.domain.repository.PayoutsRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.domain.repository.WorkersRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
) : DashboardRepository {
    override suspend fun getDashboard(range: String): AppResult<Dashboard> {
        if (BuildConfig.DEMO_MODE) {
            delay(350)
            return AppResult.Success(DemoData.dashboard())
        }
        val r = range.ifBlank { "24h" }
        return try {
            val fee = try {
                api.publicConfig().visibleFeePercent ?: 1.5
            } catch (_: Throwable) {
                1.5
            }
            AppResult.Success(api.dashboard(r).toDomain(fallbackFee = fee, rangeHint = r))
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }
}

@Singleton
class WorkersRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
) : WorkersRepository {
    override suspend fun getWorkers(): AppResult<List<Worker>> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(DemoData.workers())
        }
        return try {
            AppResult.Success(api.workers().workers.map { it.toDomain() })
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun createWorker(workerName: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(Unit)
        }
        val name = workerName.trim()
        if (name.isBlank()) return AppResult.Error("Worker name is required.")
        return try {
            api.createWorker(mapOf("workerName" to name))
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    /**
     * Pool soft-disables workers (website: "Disable this worker").
     * They remain in GET /workers with isActive=false — not hard-deleted.
     */
    override suspend fun deleteWorker(id: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(250)
            return AppResult.Success(Unit)
        }
        if (id.isBlank() || id == "unknown") {
            return AppResult.Error("Cannot disable this worker: missing server id.")
        }
        return try {
            api.deleteWorker(id)
            // Confirm disable took effect (API keeps the row).
            val after = try {
                api.workers().workers.find { it.matchesIdOrName(id) }
            } catch (_: Throwable) {
                null
            }
            if (after != null && after.isActive != false &&
                !after.status.equals("disabled", true) &&
                !after.status.equals("inactive", true)
            ) {
                return AppResult.Error(
                    "Worker is still active on the server. Try again or disable it on p2pmine.com.",
                )
            }
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun getWorker(id: String): AppResult<Worker> {
        if (BuildConfig.DEMO_MODE) {
            delay(250)
            val w = DemoData.workers().find { it.id == id }
            return if (w != null) AppResult.Success(w) else AppResult.Error("Worker not found.")
        }
        return try {
            val range = "24h"
            // Resolve stable server id + base worker row from list (name/id match).
            val listDto = try {
                api.workers().workers
            } catch (_: Throwable) {
                emptyList()
            }
            val listMatch = listDto.find { it.matchesIdOrName(id) }
            val serverId = listMatch?.id?.takeIf { it.isNotBlank() }
                ?: id.takeIf { it.isNotBlank() && it != "unknown" }
                ?: return AppResult.Error("Worker not found.")

            // 1) Typed worker detail (stats + history)
            var dto: WorkerDto? = null
            var history: List<HashratePoint> = emptyList()
            var estimated = false

            try {
                val detail = api.workerDetail(serverId, range)
                dto = detail.resolvedWorkerDto()
                history = detail.resolvedHistory().toDomainPoints(detail.resolvedRange())
                Log.d(TAG, "workerDetail ok id=$serverId hist=${history.size}")
            } catch (t: Throwable) {
                Log.w(TAG, "workerDetail typed failed id=$serverId: ${t.message}")
            }

            // 2) Raw body parse if typed failed or history empty
            if (dto == null || history.isEmpty()) {
                try {
                    val raw = api.workerDetailRaw(serverId, range).string()
                    if (dto == null) {
                        // Still try typed adapter on the string for worker fields
                        try {
                            val parsed = moshi.adapter(com.hashcore.pool.data.remote.dto.WorkerDetailResponseDto::class.java)
                                .fromJson(raw)
                            if (parsed != null) {
                                dto = parsed.resolvedWorkerDto()
                                if (history.isEmpty()) {
                                    history = parsed.resolvedHistory().toDomainPoints(parsed.resolvedRange())
                                }
                            }
                        } catch (t: Throwable) {
                            Log.w(TAG, "workerDetail re-parse failed: ${t.message}")
                        }
                    }
                    if (history.isEmpty()) {
                        history = HashrateHistoryParser.extractFromJsonObject(raw).toDomainPoints(range)
                        Log.d(TAG, "workerDetailRaw hist=${history.size}")
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "workerDetailRaw failed: ${t.message}")
                }
            }

            // 3) Dedicated history endpoints
            if (history.isEmpty()) {
                history = fetchWorkerHistorySeries(serverId, range)
            }

            // 4) Account history when this is the only worker (≈ per-worker)
            if (history.isEmpty() && listDto.size <= 1) {
                try {
                    val dash = api.dashboard(range)
                    history = (dash.history ?: dash.history24h ?: dash.hashrateHistory)
                        .orEmpty()
                        .toDomainPoints(range)
                    if (history.isNotEmpty()) {
                        Log.d(TAG, "using account dashboard history as single-worker chart (${history.size})")
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "dashboard fallback failed: ${t.message}")
                }
            }

            // Base DTO: detail → list match → error
            val baseDto = dto ?: listMatch
            if (baseDto == null) {
                return AppResult.Error("Worker not found.")
            }

            // Optional enrich from dashboard workers array (same worker only)
            val enriched = try {
                val dashWorkers = api.dashboard(range).workers.orEmpty()
                dashWorkers.find { it.sameWorkerAs(baseDto) } ?: baseDto
            } catch (_: Throwable) {
                baseDto
            }

            var worker = enriched.toDomain(
                rangeHint = range,
                historyOverride = null,
                historyIsEstimated = false,
            ).let { w ->
                if (history.isNotEmpty()) {
                    w.copy(hashrateHistory = history, historyIsEstimated = false)
                } else {
                    w
                }
            }

            // 5) Last resort: approximate curve from live averages so the chart is not blank
            //    for workers that are clearly hashing but have no sample series yet.
            if (worker.hashrateHistory.isEmpty()) {
                val approx = synthesizeWorkerHistory(
                    currentHs = worker.currentHashrateHs,
                    avg1hHs = enriched.avg1hHs(),
                    avg24hHs = worker.avg24hHashrateHs.takeIf { it > 0 } ?: enriched.avg24hHs(),
                    peakHs = enriched.peak24hHs(),
                    rangeHint = range,
                    online = worker.status == com.hashcore.pool.domain.model.WorkerStatus.ONLINE ||
                        worker.currentHashrateHs > 0,
                )
                if (approx.isNotEmpty()) {
                    worker = worker.copy(hashrateHistory = approx, historyIsEstimated = true)
                    estimated = true
                    Log.d(TAG, "using estimated history from averages (${approx.size})")
                }
            }

            AppResult.Success(worker.copy(historyIsEstimated = estimated || worker.historyIsEstimated))
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    private suspend fun fetchWorkerHistorySeries(
        serverId: String,
        range: String,
    ): List<HashratePoint> {
        // Typed history
        try {
            val hr = api.workerHistory(serverId, range)
            val pts = hr.resolvedHistory().toDomainPoints(hr.range ?: range)
            if (pts.isNotEmpty()) {
                Log.d(TAG, "workerHistory ok hist=${pts.size}")
                return pts
            }
        } catch (t: Throwable) {
            Log.w(TAG, "workerHistory typed failed: ${t.message}")
        }
        // Raw history
        try {
            val raw = api.workerHistoryRaw(serverId, range).string()
            val pts = HashrateHistoryParser.extractFromJsonObject(raw).toDomainPoints(range)
            if (pts.isNotEmpty()) {
                Log.d(TAG, "workerHistoryRaw hist=${pts.size}")
                return pts
            }
        } catch (t: Throwable) {
            Log.w(TAG, "workerHistoryRaw failed: ${t.message}")
        }
        // Account history filtered by workerId
        try {
            val hr = api.minerHistory(range = range, workerId = serverId)
            val pts = hr.resolvedHistory().toDomainPoints(hr.range ?: range)
            if (pts.isNotEmpty()) {
                Log.d(TAG, "minerHistory?workerId hist=${pts.size}")
                return pts
            }
        } catch (t: Throwable) {
            Log.w(TAG, "minerHistory typed failed: ${t.message}")
        }
        try {
            val raw = api.minerHistoryRaw(range = range, workerId = serverId).string()
            val pts = HashrateHistoryParser.extractFromJsonObject(raw).toDomainPoints(range)
            if (pts.isNotEmpty()) {
                Log.d(TAG, "minerHistoryRaw hist=${pts.size}")
                return pts
            }
        } catch (t: Throwable) {
            Log.w(TAG, "minerHistoryRaw failed: ${t.message}")
        }
        return emptyList()
    }

    companion object {
        private const val TAG = "P2PMineWorkers"
    }
}

@Singleton
class EarningsRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
) : EarningsRepository {
    override suspend fun getEarnings(period: EarningsPeriod): AppResult<List<EarningsBucket>> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(DemoData.earnings(period))
        }
        return try {
            val dash = api.dashboard("24h")
            val lifetime = dash.lifetimeEarnedBtc ?: 0.0
            val days = when (period) {
                EarningsPeriod.DAILY -> 14
                EarningsPeriod.WEEKLY -> 8 * 7
                EarningsPeriod.MONTHLY -> 6 * 30
            }
            val buckets = if (lifetime > 0) {
                syntheticEarnings(
                    lifetime,
                    when (period) {
                        EarningsPeriod.DAILY -> 14
                        EarningsPeriod.WEEKLY -> 8
                        EarningsPeriod.MONTHLY -> 6
                    },
                )
            } else {
                val daily = dash.estDailyBtc ?: 0.0
                syntheticEarnings(
                    daily * days,
                    when (period) {
                        EarningsPeriod.DAILY -> 14
                        EarningsPeriod.WEEKLY -> 8
                        EarningsPeriod.MONTHLY -> 6
                    },
                )
            }
            AppResult.Success(buckets)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }
}

@Singleton
class PayoutsRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
) : PayoutsRepository {
    override suspend fun getPayouts(): AppResult<List<Payout>> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(DemoData.payouts())
        }
        return try {
            AppResult.Success(api.dashboard().payoutList())
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun requestPayout(amountBtc: Double?): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(400)
            return AppResult.Success(Unit)
        }
        return try {
            val body = if (amountBtc == null || amountBtc <= 0) {
                PayoutRequestDto()
            } else {
                PayoutRequestDto(amountBtc = amountBtc)
            }
            api.requestPayout(body)
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
    private val prefs: UserPreferences,
) : SettingsRepository {
    override suspend fun getPublicSettings(): AppResult<PoolPublicSettings> {
        if (BuildConfig.DEMO_MODE) {
            delay(150)
            return AppResult.Success(DemoData.settings)
        }
        return try {
            AppResult.Success(api.publicConfig().toDomain())
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override val displayFiat: Flow<Boolean> = prefs.displayFiat
    override suspend fun setDisplayFiat(enabled: Boolean) = prefs.setDisplayFiat(enabled)

    override val fiatCurrency: Flow<String> = prefs.fiatCurrency
    override suspend fun setFiatCurrency(code: String) = prefs.setFiatCurrency(code)

    override val notificationsEnabled: Flow<Boolean> = prefs.notificationsEnabled
    override suspend fun setNotificationsEnabled(enabled: Boolean) =
        prefs.setNotificationsEnabled(enabled)

    override val biometricEnabled: Flow<Boolean> = prefs.biometricEnabled
    override suspend fun setBiometricEnabled(enabled: Boolean) =
        prefs.setBiometricEnabled(enabled)

    override val biometricTimeoutMinutes: Flow<Int> = prefs.biometricTimeoutMinutes
    override suspend fun setBiometricTimeoutMinutes(minutes: Int) =
        prefs.setBiometricTimeoutMinutes(minutes)

    override val amoledBlack: Flow<Boolean> = prefs.amoledBlack
    override suspend fun setAmoledBlack(enabled: Boolean) = prefs.setAmoledBlack(enabled)

    override val alertsMutedUntilEpochMs: Flow<Long> = prefs.alertsMutedUntilEpochMs
    override suspend fun setAlertsMutedUntil(epochMs: Long) = prefs.setAlertsMutedUntil(epochMs)

    override val firstShareCelebrated: Flow<Boolean> = prefs.firstShareCelebrated
    override suspend fun setFirstShareCelebrated(done: Boolean) =
        prefs.setFirstShareCelebrated(done)

    override val onboardingCompleted: Flow<Boolean> = prefs.onboardingCompleted
    override suspend fun setOnboardingCompleted(completed: Boolean) =
        prefs.setOnboardingCompleted(completed)
}
