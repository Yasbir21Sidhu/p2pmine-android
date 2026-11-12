package com.hashcore.pool.data.remote

import com.hashcore.pool.domain.model.AuthTokens
import com.hashcore.pool.domain.model.Dashboard
import com.hashcore.pool.domain.model.EarningsBucket
import com.hashcore.pool.domain.model.EarningsPeriod
import com.hashcore.pool.domain.model.HashratePoint
import com.hashcore.pool.domain.model.Payout
import com.hashcore.pool.domain.model.PayoutStatus
import com.hashcore.pool.domain.model.PoolPublicSettings
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.model.Worker
import com.hashcore.pool.domain.model.WorkerStatus
import com.hashcore.pool.domain.model.WorkersSummary
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic-ish demo data for offline development (BuildConfig.DEMO_MODE).
 * Earnings are after pool fee only.
 */
object DemoData {
    const val VISIBLE_FEE = 1.5
    const val BTC_USD = 97_450.0

    val user = User(
        id = "demo-user-1",
        email = "miner@example.com",
        displayName = "Demo Miner",
        walletAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
    )

    val tokens = AuthTokens(
        accessToken = "demo-access-token",
        refreshToken = "demo-refresh-token",
        accessExpiresAtEpochMs = System.currentTimeMillis() + 15 * 60_000L,
    )

    fun history24h(baseHs: Double = 1.85e14): List<HashratePoint> {
        val now = System.currentTimeMillis()
        return (0 until 48).map { i ->
            val t = now - (47 - i) * 30 * 60_000L
            val wave = 1.0 + 0.08 * sin(i / 3.5) + Random(i).nextDouble(-0.03, 0.03)
            HashratePoint(t, baseHs * wave)
        }
    }

    fun workers(): List<Worker> {
        val now = System.currentTimeMillis()
        return listOf(
            Worker(
                id = "w1",
                name = "rack1-s19-01",
                currentHashrateHs = 9.5e13,
                avg24hHashrateHs = 9.4e13,
                status = WorkerStatus.ONLINE,
                lastShareEpochMs = now - 12_000,
                rejectedShares = 42,
                acceptedShares = 128_400,
                uptimeSeconds = 86400 * 12 + 3600 * 5,
                hashrateHistory = history24h(9.4e13),
            ),
            Worker(
                id = "w2",
                name = "rack1-s19-02",
                currentHashrateHs = 9.2e13,
                avg24hHashrateHs = 9.1e13,
                status = WorkerStatus.ONLINE,
                lastShareEpochMs = now - 8_000,
                rejectedShares = 18,
                acceptedShares = 120_100,
                uptimeSeconds = 86400 * 12,
                hashrateHistory = history24h(9.1e13),
            ),
            Worker(
                id = "w3",
                name = "garage-m50",
                currentHashrateHs = 0.0,
                avg24hHashrateHs = 4.2e13,
                status = WorkerStatus.OFFLINE,
                lastShareEpochMs = now - 3 * 3600_000L,
                rejectedShares = 90,
                acceptedShares = 55_000,
                uptimeSeconds = 0,
                hashrateHistory = history24h(4.2e13).mapIndexed { idx, p ->
                    if (idx > 40) p.copy(hashrateHs = 0.0) else p
                },
            ),
            Worker(
                id = "w4",
                name = "rack2-s21-01",
                currentHashrateHs = 1.1e13,
                avg24hHashrateHs = 1.8e14 * 0.4,
                status = WorkerStatus.PROBLEM,
                lastShareEpochMs = now - 25 * 60_000L,
                rejectedShares = 1_200,
                acceptedShares = 80_000,
                uptimeSeconds = 86400 * 2,
                hashrateHistory = history24h(7e13),
            ),
        )
    }

    fun dashboard(jitter: Double = 1.0): Dashboard {
        val list = workers()
        val online = list.count { it.status == WorkerStatus.ONLINE }
        val offline = list.count { it.status == WorkerStatus.OFFLINE }
        val problem = list.count { it.status == WorkerStatus.PROBLEM }
        val current = list.sumOf { it.currentHashrateHs } * jitter
        return Dashboard(
            currentHashrateHs = current,
            avg24hHashrateHs = list.sumOf { it.avg24hHashrateHs },
            unpaidBalanceBtc = 0.00218450,
            estEarningsTodayBtc = 0.00041200,
            estEarningsYesterdayBtc = 0.00039810,
            visibleFeePercent = VISIBLE_FEE,
            btcUsdRate = BTC_USD,
            workers = WorkersSummary(online, offline, problem),
            lastPayout = Payout(
                id = "p9",
                amountBtc = 0.00420000,
                txId = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456",
                status = PayoutStatus.CONFIRMED,
                createdAtEpochMs = System.currentTimeMillis() - 3 * 86400_000L,
            ),
            hashrateHistory24h = history24h(current.coerceAtLeast(1e13)),
            minPayoutBtc = 0.001,
            payoutSchedule = "Daily at 00:00 UTC when balance ≥ minimum",
        )
    }

    fun earnings(period: EarningsPeriod): List<EarningsBucket> {
        val now = System.currentTimeMillis()
        return when (period) {
            EarningsPeriod.DAILY -> (0 until 14).map { i ->
                val day = now - (13 - i) * 86400_000L
                EarningsBucket(
                    label = "D${i + 1}",
                    startEpochMs = day,
                    endEpochMs = day + 86400_000L,
                    amountBtc = 0.00035 + Random(i).nextDouble() * 0.00012,
                )
            }
            EarningsPeriod.WEEKLY -> (0 until 8).map { i ->
                val start = now - (7 - i) * 7 * 86400_000L
                EarningsBucket(
                    label = "W${i + 1}",
                    startEpochMs = start,
                    endEpochMs = start + 7 * 86400_000L,
                    amountBtc = 0.0024 + Random(i + 20).nextDouble() * 0.0006,
                )
            }
            EarningsPeriod.MONTHLY -> (0 until 6).map { i ->
                val start = now - (5 - i) * 30L * 86400_000L
                EarningsBucket(
                    label = "M${i + 1}",
                    startEpochMs = start,
                    endEpochMs = start + 30L * 86400_000L,
                    amountBtc = 0.010 + Random(i + 40).nextDouble() * 0.003,
                )
            }
        }
    }

    fun payouts(): List<Payout> {
        val now = System.currentTimeMillis()
        return listOf(
            Payout("p9", 0.00420000, "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456", PayoutStatus.CONFIRMED, now - 3 * 86400_000L),
            Payout("p8", 0.00385000, "b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef12345678", PayoutStatus.CONFIRMED, now - 10 * 86400_000L),
            Payout("p7", 0.00401000, "c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567890", PayoutStatus.SENT, now - 17 * 86400_000L),
            Payout("p6", 0.00290000, null, PayoutStatus.PENDING, now - 24 * 86400_000L),
        )
    }

    val settings = PoolPublicSettings(
        visibleFeePercent = VISIBLE_FEE,
        minPayoutBtc = 0.0001,
        payoutSchedule = "Daily · Stratum: stratum+tcp://p2pmine.com:3333,2525,9999",
        supportUrl = "https://p2pmine.com",
        supportEmail = "admin@p2pmine.com",
    )
}
