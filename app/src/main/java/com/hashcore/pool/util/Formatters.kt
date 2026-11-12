package com.hashcore.pool.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

object HashrateFormatter {
    private val units = arrayOf("H/s", "KH/s", "MH/s", "GH/s", "TH/s", "PH/s", "EH/s")

    fun format(hs: Double): String {
        if (hs <= 0.0 || hs.isNaN()) return "0 H/s"
        val exp = (ln(hs) / ln(1000.0)).toInt().coerceIn(0, units.lastIndex)
        val value = hs / 1000.0.pow(exp.toDouble())
        val df = if (value >= 100) DecimalFormat("#,##0") else DecimalFormat("#,##0.##")
        return "${df.format(value)} ${units[exp]}"
    }
}

object BtcFormatter {
    private val btcDf = DecimalFormat("0.00000000")
    private val compactBtc = DecimalFormat("0.####")

    fun btc(amount: Double, compact: Boolean = false): String {
        if (amount == 0.0) return if (compact) "0 BTC" else "0.00000000 BTC"
        val body = if (compact && abs(amount) < 1) compactBtc.format(amount) else btcDf.format(amount)
        return "$body BTC"
    }

    /**
     * @param usdRate multiplies USD (btc * btcUsd) to target fiat (from public config).
     * @param symbol currency symbol e.g. "$", "₹"
     * @param decimals 0 for INR-style, 2 for USD/EUR
     */
    fun fiat(
        amountBtc: Double,
        btcUsd: Double?,
        usdRate: Double = 1.0,
        symbol: String = "$",
        decimals: Int = 2,
    ): String {
        if (btcUsd == null || btcUsd <= 0) return "—"
        val amount = amountBtc * btcUsd * usdRate
        val pattern = if (decimals <= 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        val df = DecimalFormat(pattern)
        return "$symbol${df.format(amount)}"
    }

    fun dual(
        amountBtc: Double,
        btcUsd: Double?,
        preferFiat: Boolean,
        usdRate: Double = 1.0,
        symbol: String = "$",
        decimals: Int = 2,
    ): String {
        return if (preferFiat && btcUsd != null) {
            "${fiat(amountBtc, btcUsd, usdRate, symbol, decimals)} (${btc(amountBtc, compact = true)})"
        } else {
            val fiatPart = if (btcUsd != null) {
                " ≈ ${fiat(amountBtc, btcUsd, usdRate, symbol, decimals)}"
            } else ""
            "${btc(amountBtc, compact = true)}$fiatPart"
        }
    }
}

object TimeFormatter {
    private val dateTime = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private val dateOnly = SimpleDateFormat("dd MMM yyyy", Locale.US)

    fun dateTime(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0) return "—"
        return dateTime.format(Date(epochMs))
    }

    fun date(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0) return "—"
        return dateOnly.format(Date(epochMs))
    }

    fun relative(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0) return "never"
        val diff = System.currentTimeMillis() - epochMs
        if (diff < 0) return "just now"
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        return when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86400}d ago"
        }
    }

    fun duration(seconds: Long): String {
        if (seconds <= 0) return "—"
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }
}

object FeeFormatter {
    fun percent(value: Double): String {
        val df = if (value == value.toLong().toDouble()) DecimalFormat("0") else DecimalFormat("0.##")
        return df.format(value)
    }
}
