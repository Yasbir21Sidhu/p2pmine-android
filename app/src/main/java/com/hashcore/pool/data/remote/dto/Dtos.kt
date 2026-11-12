package com.hashcore.pool.data.remote.dto

import com.hashcore.pool.domain.model.AuthTokens
import com.hashcore.pool.domain.model.Dashboard
import com.hashcore.pool.domain.model.EarningsBucket
import com.hashcore.pool.domain.model.HashratePoint
import com.hashcore.pool.domain.model.Payout
import com.hashcore.pool.domain.model.PayoutStatus
import com.hashcore.pool.domain.model.PoolPublicSettings
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.model.Worker
import com.hashcore.pool.domain.model.WorkerStatus
import com.hashcore.pool.domain.model.WorkersSummary
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthLoginRequestDto(
    val identifier: String,
    val password: String? = null,
)

@JsonClass(generateAdapter = true)
data class TwoFaVerifyRequestDto(
    val tempToken: String,
    val code: String,
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    val token: String? = null,
    val user: UserDto? = null,
    @Json(name = "requires2fa") val requires2fa: Boolean? = null,
    @Json(name = "requires_2fa") val requires2faSnake: Boolean? = null,
    val tempToken: String? = null,
    @Json(name = "temp_token") val tempTokenSnake: String? = null,
    val message: String? = null,
    val error: String? = null,
) {
    fun needs2fa(): Boolean = requires2fa == true || requires2faSnake == true
    fun resolvedTempToken(): String? = tempToken ?: tempTokenSnake
}

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String? = null,
    val email: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "display_name") val displayNameSnake: String? = null,
    @Json(name = "btcAddress") val btcAddress: String? = null,
    @Json(name = "btc_address") val btcAddressSnake: String? = null,
    val wallet: String? = null,
    val role: String? = null,
    @Json(name = "fiatCurrency") val fiatCurrency: String? = null,
) {
    fun toDomain() = User(
        id = id ?: email ?: wallet ?: "user",
        email = email ?: wallet.orEmpty(),
        displayName = displayName ?: displayNameSnake,
        walletAddress = btcAddress ?: btcAddressSnake ?: wallet,
        fiatCurrency = fiatCurrency,
    )
}

@JsonClass(generateAdapter = true)
data class MeResponseDto(
    val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class UpdateMeRequestDto(
    val displayName: String? = null,
    val btcAddress: String? = null,
    val fiatCurrency: String? = null,
    val password: String? = null,
)

@JsonClass(generateAdapter = true)
data class HashratePointDto(
    val t: FlexibleEpochMs? = null,
    /**
     * On P2PMine website this is usually a display label like "14:00".
     * Adapter also accepts epoch numbers / ISO strings.
     */
    val time: FlexibleTimeLabel? = null,
    val ts: FlexibleEpochMs? = null,
    @Json(name = "timestamp") val timestamp: FlexibleEpochMs? = null,
    val hashrateHs: Double? = null,
    val hs: Double? = null,
    val hashrate: Double? = null,
    val hashrateThs: Double? = null,
    val value: Double? = null,
    val y: Double? = null,
    val rate: Double? = null,
) {
    /**
     * @param index 0-based index in the series (for synthesizing X when only labels exist)
     * @param total series length
     * @param rangeHint "1h" | "24h" | "7d" | "30d" for spacing between points
     */
    fun toDomain(index: Int = 0, total: Int = 1, rangeHint: String = "24h"): HashratePoint? {
        val hsVal = hashrateHs
            ?: hs
            ?: hashrate
            ?: value
            ?: y
            ?: rate
            ?: hashrateThs?.let { it * 1e12 }
            ?: return null
        val label = time?.label?.takeIf { it.isNotBlank() }
        val tsVal = t?.value
            ?: ts?.value
            ?: timestamp?.value
            ?: time?.epochMs
            ?: synthesizeBucketTime(index, total, rangeHint)
        return HashratePoint(
            timestampEpochMs = tsVal,
            hashrateHs = hsVal,
            label = label,
        )
    }
}

fun List<HashratePointDto>.toDomainPoints(rangeHint: String = "24h"): List<HashratePoint> =
    mapIndexedNotNull { i, p -> p.toDomain(index = i, total = size, rangeHint = rangeHint) }

/** Evenly space points over the selected range ending at now. */
private fun synthesizeBucketTime(index: Int, total: Int, rangeHint: String): Long {
    val spanMs = when (rangeHint.lowercase()) {
        "1h" -> 60L * 60_000L
        "7d" -> 7L * 86_400_000L
        "30d" -> 30L * 86_400_000L
        else -> 24L * 86_400_000L // 24h default
    }
    val n = total.coerceAtLeast(1)
    val step = spanMs / n
    val now = System.currentTimeMillis()
    return now - (n - 1 - index).toLong() * step
}

@JsonClass(generateAdapter = true)
data class WorkerDto(
    val id: String? = null,
    val workerName: String? = null,
    val name: String? = null,
    val hashrateHs: Double? = null,
    val hashrate: Double? = null,
    val hashrateDisplay: String? = null,
    val avgHashrate1hDisplay: String? = null,
    val avgHashrate24hDisplay: String? = null,
    val avgHashrate1h: Double? = null,
    val avgHashrate24h: Double? = null,
    /** Live detail endpoint field names (preferred). */
    val avgHashrate1hHs: Double? = null,
    val avgHashrate24hHs: Double? = null,
    val avg24hHashrateHs: Double? = null,
    val peakHashrate24h: Double? = null,
    val peakHashrate24hHs: Double? = null,
    val peakHashrate24hDisplay: String? = null,
    val status: String? = null,
    val online: Boolean? = null,
    /** Website uses isActive; false after soft-delete/disable. */
    val isActive: Boolean? = null,
    val acceptRatePercent: Double? = null,
    val rejectRatePercent: Double? = null,
    val staleRatePercent: Double? = null,
    val difficulty: Double? = null,
    /** ISO-8601 or epoch ms — see [FlexibleEpochMs] */
    val lastShareAt: FlexibleEpochMs? = null,
    @Json(name = "last_share_at") val lastShareAtSnake: FlexibleEpochMs? = null,
    val lastShareAgo: String? = null,
    val rejectedShares: Long? = null,
    val sharesInvalid: Long? = null,
    val sharesValid: Long? = null,
    val sharesDelta24h: Long? = null,
    val uptimeSeconds: Long? = null,
    val stratumUser: String? = null,
    @Json(name = "stratum_user") val stratumUserSnake: String? = null,
    /** Present if API attaches series on the worker object. */
    val history: List<HashratePointDto>? = null,
    val history24h: List<HashratePointDto>? = null,
    val hashrateHistory: List<HashratePointDto>? = null,
)

/**
 * `GET /api/miner/workers/{id}?range=24h`
 * Returns nested `worker` plus top-level history / averages (and flattened worker fields).
 */
@JsonClass(generateAdapter = true)
data class WorkerDetailResponseDto(
    val worker: WorkerDto? = null,
    val history: List<HashratePointDto>? = null,
    val history24h: List<HashratePointDto>? = null,
    val hashrateHistory: List<HashratePointDto>? = null,
    val historyRange: String? = null,
    val range: String? = null,
    // Flattened worker fields (same shape as list/detail) when nested worker is absent.
    val id: String? = null,
    val workerName: String? = null,
    val name: String? = null,
    val hashrateHs: Double? = null,
    val hashrate: Double? = null,
    val avgHashrate1hHs: Double? = null,
    val avgHashrate24hHs: Double? = null,
    val avg24hHashrateHs: Double? = null,
    val avgHashrate1h: Double? = null,
    val avgHashrate24h: Double? = null,
    val peakHashrate24hHs: Double? = null,
    val peakHashrate24h: Double? = null,
    val status: String? = null,
    val online: Boolean? = null,
    val isActive: Boolean? = null,
    val acceptRatePercent: Double? = null,
    val rejectRatePercent: Double? = null,
    val lastShareAt: FlexibleEpochMs? = null,
    val rejectedShares: Long? = null,
    val sharesInvalid: Long? = null,
    val sharesValid: Long? = null,
    val sharesDelta24h: Long? = null,
    val uptimeSeconds: Long? = null,
    val stratumUser: String? = null,
) {
    fun resolvedRange(): String = historyRange ?: range ?: "24h"

    fun resolvedWorkerDto(): WorkerDto {
        worker?.let { return it }
        return WorkerDto(
            id = id,
            workerName = workerName,
            name = name,
            hashrateHs = hashrateHs,
            hashrate = hashrate,
            avgHashrate1hHs = avgHashrate1hHs,
            avgHashrate24hHs = avgHashrate24hHs,
            avg24hHashrateHs = avg24hHashrateHs,
            avgHashrate1h = avgHashrate1h,
            avgHashrate24h = avgHashrate24h,
            peakHashrate24hHs = peakHashrate24hHs,
            peakHashrate24h = peakHashrate24h,
            status = status,
            online = online,
            isActive = isActive,
            acceptRatePercent = acceptRatePercent,
            rejectRatePercent = rejectRatePercent,
            lastShareAt = lastShareAt,
            rejectedShares = rejectedShares ?: sharesInvalid,
            sharesValid = sharesValid,
            sharesDelta24h = sharesDelta24h,
            uptimeSeconds = uptimeSeconds,
            stratumUser = stratumUser,
            history = history,
            history24h = history24h,
            hashrateHistory = hashrateHistory,
        )
    }

    fun resolvedHistory(): List<HashratePointDto> =
        (history ?: history24h ?: hashrateHistory ?: worker?.history ?: worker?.history24h
            ?: worker?.hashrateHistory).orEmpty()
}

/** `GET /api/miner/workers/{id}/history` or `GET /api/miner/history?workerId=` */
@JsonClass(generateAdapter = true)
data class WorkerHistoryResponseDto(
    val range: String? = null,
    val workerId: String? = null,
    val history: List<HashratePointDto>? = null,
    val history24h: List<HashratePointDto>? = null,
    val hashrateHistory: List<HashratePointDto>? = null,
) {
    fun resolvedHistory(): List<HashratePointDto> =
        (history ?: history24h ?: hashrateHistory).orEmpty()
}

@JsonClass(generateAdapter = true)
data class WorkersResponseDto(
    val workers: List<WorkerDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PayoutDto(
    val id: String? = null,
    val amountBtc: Double? = null,
    @Json(name = "amount_btc") val amountBtcSnake: Double? = null,
    val txid: String? = null,
    @Json(name = "tx_id") val txIdSnake: String? = null,
    val status: String? = null,
    val createdAt: FlexibleEpochMs? = null,
    @Json(name = "created_at") val createdAtSnake: FlexibleEpochMs? = null,
    val createdAtIso: String? = null,
)

@JsonClass(generateAdapter = true)
data class PayoutRequestDto(
    val amountBtc: Double? = null,
)

@JsonClass(generateAdapter = true)
data class DashboardDto(
    val hashrateHs: Double? = null,
    val hashrateDisplay: String? = null,
    val avgHashrate24hDisplay: String? = null,
    val avg24hDisplay: String? = null,
    val unpaidBtc: Double? = null,
    val unpaidUsd: Double? = null,
    val unpaidFiat: Double? = null,
    val estDailyBtc: Double? = null,
    @Json(name = "est_today_btc") val estTodayBtc: Double? = null,
    @Json(name = "est_yesterday_btc") val estYesterdayBtc: Double? = null,
    val lifetimeEarnedBtc: Double? = null,
    val visibleFeePercent: Double? = null,
    val btcPriceUsd: Double? = null,
    val onlineWorkers: Int? = null,
    val offlineWorkers: Int? = null,
    val workerCount: Int? = null,
    val workers: List<WorkerDto>? = null,
    val history: List<HashratePointDto>? = null,
    val history24h: List<HashratePointDto>? = null,
    val hashrateHistory: List<HashratePointDto>? = null,
    val payouts: List<PayoutDto>? = null,
    val minPayoutBtc: Double? = null,
    val needForMinPayoutBtc: Double? = null,
    val hoursToMinPayout: Double? = null,
    val payoutSchedule: String? = null,
    val network: NetworkFeeDto? = null,
    val averages: AveragesDto? = null,
)

@JsonClass(generateAdapter = true)
data class NetworkFeeDto(
    val visibleFeePercent: Double? = null,
)

@JsonClass(generateAdapter = true)
data class AveragesDto(
    val avg1hDisplay: String? = null,
    val avg24hDisplay: String? = null,
    val avg7dDisplay: String? = null,
    val peakDisplay: String? = null,
    val sharesSubmitted1h: Long? = null,
    val sharesSubmitted24h: Long? = null,
)

@JsonClass(generateAdapter = true)
data class FiatCurrencyDto(
    val code: String,
    val name: String? = null,
    val symbol: String? = null,
    val usdRate: Double? = null,
)

@JsonClass(generateAdapter = true)
data class PoolSettingsDto(
    @Json(name = "pool_name") val poolName: String? = null,
    val tagline: String? = null,
    @Json(name = "visible_fee_percent") val visibleFeePercent: Double? = null,
    @Json(name = "min_payout_btc") val minPayoutBtc: Double? = null,
    @Json(name = "payout_schedule") val payoutSchedule: String? = null,
    @Json(name = "stratum_host") val stratumHost: String? = null,
    @Json(name = "stratum_ports") val stratumPorts: List<String>? = null,
    @Json(name = "stratum_protocol") val stratumProtocol: String? = null,
    @Json(name = "support_email") val supportEmail: String? = null,
    @Json(name = "support_whatsapp") val supportWhatsapp: String? = null,
    @Json(name = "support_telegram") val supportTelegram: String? = null,
    @Json(name = "support_url") val supportUrl: String? = null,
    @Json(name = "payout_eta_hours") val payoutEtaHours: Int? = null,
    @Json(name = "btc_price_usd") val btcPriceUsd: Double? = null,
    @Json(name = "default_fiat") val defaultFiat: String? = null,
    @Json(name = "fiat_currencies") val fiatCurrencies: List<FiatCurrencyDto>? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    val message: String? = null,
    val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class LiveUpdateDto(
    val type: String? = null,
    val data: DashboardDto? = null,
    val hashrateHs: Double? = null,
    val onlineWorkers: Int? = null,
    val offlineWorkers: Int? = null,
)

// ---- Mappers ----

fun AuthResponseDto.toTokens() = AuthTokens(
    accessToken = token.orEmpty(),
    refreshToken = token.orEmpty(), // single-token auth on P2PMine
    accessExpiresAtEpochMs = System.currentTimeMillis() + 12 * 60 * 60_000L,
)

/** Never match on null == null (that wrongly merged unrelated workers). */
fun WorkerDto.matchesIdOrName(key: String): Boolean {
    if (key.isBlank()) return false
    if (!id.isNullOrBlank() && id == key) return true
    if (!workerName.isNullOrBlank() && workerName.equals(key, ignoreCase = true)) return true
    if (!name.isNullOrBlank() && name.equals(key, ignoreCase = true)) return true
    return false
}

/** Same worker only when non-blank id or name matches. */
fun WorkerDto.sameWorkerAs(other: WorkerDto): Boolean {
    if (!id.isNullOrBlank() && !other.id.isNullOrBlank() && id == other.id) return true
    val a = workerName?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() }
    val b = other.workerName?.takeIf { it.isNotBlank() } ?: other.name?.takeIf { it.isNotBlank() }
    return a != null && b != null && a.equals(b, ignoreCase = true)
}

fun WorkerDto.toDomain(
    rangeHint: String = "24h",
    historyOverride: List<HashratePointDto>? = null,
    historyIsEstimated: Boolean = false,
): Worker {
    val disabled = isActive == false ||
        status.equals("disabled", true) ||
        status.equals("inactive", true)
    val st = when {
        disabled -> WorkerStatus.DISABLED
        online == true -> WorkerStatus.ONLINE
        online == false -> WorkerStatus.OFFLINE
        status.equals("online", true) || status.equals("active", true) -> WorkerStatus.ONLINE
        status.equals("problem", true) || status.equals("error", true) -> WorkerStatus.PROBLEM
        else -> WorkerStatus.OFFLINE
    }
    val current = if (disabled) 0.0 else (hashrateHs ?: hashrate ?: 0.0)
    val avg24 = if (disabled) {
        0.0
    } else {
        avgHashrate24hHs ?: avg24hHashrateHs ?: avgHashrate24h ?: 0.0
    }
    val rawHist = historyOverride
        ?: (history ?: history24h ?: hashrateHistory).orEmpty()
    val hist = rawHist.toDomainPoints(rangeHint)
    val stableId = id?.takeIf { it.isNotBlank() }
        ?: workerName?.takeIf { it.isNotBlank() }
        ?: name?.takeIf { it.isNotBlank() }
        ?: "unknown"
    return Worker(
        id = stableId,
        name = workerName ?: name ?: id ?: "worker",
        currentHashrateHs = current,
        avg24hHashrateHs = avg24,
        status = st,
        lastShareEpochMs = lastShareAt?.value ?: lastShareAtSnake?.value,
        rejectedShares = rejectedShares ?: sharesInvalid ?: 0,
        acceptedShares = sharesValid ?: 0,
        uptimeSeconds = uptimeSeconds ?: 0,
        hashrateHistory = hist,
        historyIsEstimated = historyIsEstimated && hist.isNotEmpty(),
        stratumUser = stratumUser ?: stratumUserSnake,
        isActive = !disabled,
        acceptRatePercent = acceptRatePercent,
        rejectRatePercent = rejectRatePercent,
    )
}

fun WorkerDetailResponseDto.toDomain(): Worker {
    val rangeHint = resolvedRange()
    return resolvedWorkerDto().toDomain(
        rangeHint = rangeHint,
        historyOverride = resolvedHistory(),
        historyIsEstimated = false,
    )
}

fun WorkerDto.avg1hHs(): Double? = avgHashrate1hHs ?: avgHashrate1h
fun WorkerDto.avg24hHs(): Double? = avgHashrate24hHs ?: avg24hHashrateHs ?: avgHashrate24h
fun WorkerDto.peak24hHs(): Double? = peakHashrate24hHs ?: peakHashrate24h

/**
 * Build a smooth 24-bucket series from sparse worker stats so detail charts are useful
 * when the API does not return a per-worker history array (current P2PMine behavior).
 */
fun synthesizeWorkerHistory(
    currentHs: Double,
    avg1hHs: Double?,
    avg24hHs: Double?,
    peakHs: Double?,
    rangeHint: String = "24h",
    online: Boolean = true,
    buckets: Int = 24,
): List<HashratePoint> {
    if (!online && currentHs <= 0 && (avg24hHs ?: 0.0) <= 0) {
        // Fully offline with no history — empty chart is honest
        return emptyList()
    }
    val base = when {
        avg24hHs != null && avg24hHs > 0 -> avg24hHs
        avg1hHs != null && avg1hHs > 0 -> avg1hHs
        currentHs > 0 -> currentHs
        else -> 0.0
    }
    if (base <= 0 && currentHs <= 0) return emptyList()

    val peak = (peakHs ?: base * 1.15).coerceAtLeast(base)
    val recent = avg1hHs ?: ((currentHs + base) / 2.0)
    val now = currentHs.takeIf { it > 0 } ?: recent
    val n = buckets.coerceAtLeast(8)
    val spanMs = when (rangeHint.lowercase()) {
        "1h" -> 60L * 60_000L
        "7d" -> 7L * 86_400_000L
        "30d" -> 30L * 86_400_000L
        else -> 24L * 86_400_000L
    }
    val step = spanMs / n
    val end = System.currentTimeMillis()

    return (0 until n).map { i ->
        val t = i.toDouble() / (n - 1).coerceAtLeast(1)
        // Blend: early → 24h avg, mid → peak-ish, late → 1h avg, last → current
        val v = when {
            t < 0.5 -> base + (peak - base) * (t / 0.5) * 0.35
            t < 0.85 -> base * 0.9 + recent * 0.1 + (peak - base) * 0.15 * (1 - (t - 0.5) / 0.35)
            else -> recent + (now - recent) * ((t - 0.85) / 0.15)
        }.coerceAtLeast(0.0)
        val epoch = end - (n - 1 - i).toLong() * step
        HashratePoint(
            timestampEpochMs = epoch,
            hashrateHs = v,
            label = null,
        )
    }
}

fun PayoutDto.toDomain(): Payout {
    val st = when (status?.lowercase()) {
        "pending", "requested", "queued" -> PayoutStatus.PENDING
        "sent", "broadcast", "processing" -> PayoutStatus.SENT
        "confirmed", "complete", "completed", "paid" -> PayoutStatus.CONFIRMED
        else -> PayoutStatus.PENDING
    }
    val createdMs = createdAt?.value
        ?: createdAtSnake?.value
        ?: EpochMsParser.parse(createdAtIso)
        ?: 0L
    return Payout(
        id = id ?: createdMs.toString(),
        amountBtc = amountBtc ?: amountBtcSnake ?: 0.0,
        txId = txid ?: txIdSnake,
        status = st,
        createdAtEpochMs = createdMs,
    )
}

fun DashboardDto.toDomain(fallbackFee: Double = 1.5, rangeHint: String = "24h"): Dashboard {
    val rawHist = (history ?: history24h ?: hashrateHistory).orEmpty()
    val hist = rawHist.mapIndexedNotNull { index, point ->
        point.toDomain(index = index, total = rawHist.size, rangeHint = rangeHint)
    }
    val workerList = workers.orEmpty().map { it.toDomain() }
    val online = onlineWorkers ?: workerList.count { it.status == WorkerStatus.ONLINE }
    val offline = offlineWorkers ?: workerList.count { it.status != WorkerStatus.ONLINE }
    val fee = network?.visibleFeePercent ?: visibleFeePercent ?: fallbackFee
    val lastPayout = payouts?.firstOrNull()?.toDomain()
    return Dashboard(
        currentHashrateHs = hashrateHs ?: 0.0,
        avg24hHashrateHs = averages?.let { 0.0 } ?: 0.0,
        unpaidBalanceBtc = unpaidBtc ?: 0.0,
        estEarningsTodayBtc = estDailyBtc ?: estTodayBtc ?: 0.0,
        estEarningsYesterdayBtc = estYesterdayBtc ?: 0.0,
        visibleFeePercent = fee,
        btcUsdRate = btcPriceUsd,
        workers = WorkersSummary(online = online, offline = offline, problem = 0),
        lastPayout = lastPayout,
        hashrateHistory24h = hist,
        minPayoutBtc = minPayoutBtc ?: 0.0001,
        payoutSchedule = payoutSchedule.orEmpty(),
    )
}

fun DashboardDto.payoutList(): List<Payout> = payouts.orEmpty().map { it.toDomain() }

fun PoolSettingsDto.toDomain() = PoolPublicSettings(
    visibleFeePercent = visibleFeePercent ?: 1.5,
    minPayoutBtc = minPayoutBtc ?: 0.0001,
    payoutSchedule = buildString {
        append(payout_schedule_display())
        if (payoutEtaHours != null) append(" · ~${payoutEtaHours}h processing")
        if (!stratumHost.isNullOrBlank()) {
            append("\nStratum: ")
            append(stratumProtocol ?: "stratum+tcp")
            append("://")
            append(stratumHost)
            stratumPorts?.takeIf { it.isNotEmpty() }?.let { append(":").append(it.joinToString(",")) }
        }
    },
    supportUrl = supportUrl ?: "https://p2pmine.com",
    supportEmail = supportEmail ?: "admin@p2pmine.com",
    supportWhatsapp = supportWhatsapp,
    supportTelegram = supportTelegram,
    btcPriceUsd = btcPriceUsd,
    defaultFiat = defaultFiat ?: "INR",
    fiatCurrencies = fiatCurrencies.orEmpty().map {
        com.hashcore.pool.domain.model.FiatCurrency(
            code = it.code,
            name = it.name ?: it.code,
            symbol = it.symbol ?: it.code,
            usdRate = it.usdRate ?: 1.0,
        )
    },
    stratumHost = stratumHost,
    stratumPorts = stratumPorts.orEmpty(),
)

private fun PoolSettingsDto.payout_schedule_display(): String =
    when (payoutSchedule?.lowercase()) {
        "daily" -> "Daily payouts"
        "weekly" -> "Weekly payouts"
        else -> payoutSchedule ?: "On request"
    }

/** Placeholder earnings buckets derived from lifetime if API has no period history. */
fun syntheticEarnings(lifetimeBtc: Double, days: Int): List<EarningsBucket> {
    if (lifetimeBtc <= 0) return emptyList()
    val now = System.currentTimeMillis()
    val perDay = lifetimeBtc / days.coerceAtLeast(1)
    return (0 until days).map { i ->
        val start = now - (days - 1 - i) * 86_400_000L
        EarningsBucket(
            label = "D${i + 1}",
            startEpochMs = start,
            endEpochMs = start + 86_400_000L,
            amountBtc = perDay * (0.85 + (i % 5) * 0.04),
        )
    }
}


@JsonClass(generateAdapter = true)
data class AlertsResponseDto(
    val alerts: List<AlertDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AlertDto(
    val id: String? = null,
    val type: String? = null,
    val message: String? = null,
    val title: String? = null,
    val createdAt: FlexibleEpochMs? = null,
    @Json(name = "created_at") val createdAtSnake: FlexibleEpochMs? = null,
    val dismissedAt: FlexibleEpochMs? = null,
    @Json(name = "dismissed_at") val dismissedAtSnake: FlexibleEpochMs? = null,
    val workerName: String? = null,
)
