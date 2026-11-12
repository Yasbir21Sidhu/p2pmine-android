package com.hashcore.pool.domain.model

/**
 * Domain models — pure Kotlin, no Android/network types.
 * All monetary amounts are after the *visible* pool fee only.
 * Hidden 1% fee is never represented here.
 */

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val walletAddress: String?,
    val fiatCurrency: String? = null,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtEpochMs: Long? = null,
)

data class HashratePoint(
    val timestampEpochMs: Long,
    /** Hashrate in H/s */
    val hashrateHs: Double,
    /** Optional axis label from API (e.g. "14:00") when time is not epoch */
    val label: String? = null,
)

enum class WorkerStatus {
    ONLINE,
    OFFLINE,
    PROBLEM,
    /** Soft-deleted / disabled on the pool (still listed, not mining). */
    DISABLED,
}

data class Worker(
    val id: String,
    val name: String,
    val currentHashrateHs: Double,
    val avg24hHashrateHs: Double,
    val status: WorkerStatus,
    val lastShareEpochMs: Long?,
    val rejectedShares: Long = 0,
    val acceptedShares: Long = 0,
    val uptimeSeconds: Long = 0,
    /**
     * Per-worker hashrate series for the chart.
     * Prefer real pool samples; may be estimated from averages when the series is empty.
     */
    val hashrateHistory: List<HashratePoint> = emptyList(),
    /** True when [hashrateHistory] was estimated locally (not raw pool samples). */
    val historyIsEstimated: Boolean = false,
    /** Full stratum username to paste into ASIC (e.g. account.worker) */
    val stratumUser: String? = null,
    /** false after pool "disable" — worker name still reserved but not active. */
    val isActive: Boolean = true,
    val acceptRatePercent: Double? = null,
    val rejectRatePercent: Double? = null,
)

data class WorkersSummary(
    val online: Int,
    val offline: Int,
    val problem: Int,
) {
    val total: Int get() = online + offline + problem
}

data class Payout(
    val id: String,
    val amountBtc: Double,
    val txId: String?,
    val status: PayoutStatus,
    val createdAtEpochMs: Long,
)

enum class PayoutStatus {
    PENDING,
    SENT,
    CONFIRMED,
    FAILED,
}

data class EarningsBucket(
    val label: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val amountBtc: Double,
)

enum class EarningsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
}

data class Dashboard(
    val currentHashrateHs: Double,
    val avg24hHashrateHs: Double,
    val unpaidBalanceBtc: Double,
    val estEarningsTodayBtc: Double,
    val estEarningsYesterdayBtc: Double,
    val visibleFeePercent: Double,
    val btcUsdRate: Double?,
    val workers: WorkersSummary,
    val lastPayout: Payout?,
    val hashrateHistory24h: List<HashratePoint>,
    val minPayoutBtc: Double,
    val payoutSchedule: String,
)

data class LiveUpdate(
    val currentHashrateHs: Double? = null,
    val avg24hHashrateHs: Double? = null,
    val workers: List<Worker>? = null,
    val workersSummary: WorkersSummary? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
)

data class FiatCurrency(
    val code: String,
    val name: String,
    val symbol: String,
    /** Multiply USD amount by this to get fiat (from public config usdRate). */
    val usdRate: Double,
)

data class PoolPublicSettings(
    val visibleFeePercent: Double,
    val minPayoutBtc: Double,
    val payoutSchedule: String,
    val supportUrl: String?,
    val supportEmail: String?,
    val supportWhatsapp: String? = null,
    val supportTelegram: String? = null,
    val btcPriceUsd: Double? = null,
    val defaultFiat: String = "USD",
    val fiatCurrencies: List<FiatCurrency> = emptyList(),
    val stratumHost: String? = null,
    val stratumPorts: List<String> = emptyList(),
)

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isAuthError: Boolean = false,
        val isNetworkError: Boolean = false,
    ) : AppResult<Nothing>()
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}
