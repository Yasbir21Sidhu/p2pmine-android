package com.hashcore.pool.data.remote.api

import com.hashcore.pool.data.remote.dto.AlertsResponseDto
import com.hashcore.pool.data.remote.dto.AuthLoginRequestDto
import com.hashcore.pool.data.remote.dto.AuthResponseDto
import com.hashcore.pool.data.remote.dto.DashboardDto
import com.hashcore.pool.data.remote.dto.MeResponseDto
import com.hashcore.pool.data.remote.dto.PayoutRequestDto
import com.hashcore.pool.data.remote.dto.PoolSettingsDto
import com.hashcore.pool.data.remote.dto.TwoFaVerifyRequestDto
import com.hashcore.pool.data.remote.dto.UpdateMeRequestDto
import com.hashcore.pool.data.remote.dto.WorkerDetailResponseDto
import com.hashcore.pool.data.remote.dto.WorkerHistoryResponseDto
import com.hashcore.pool.data.remote.dto.WorkersResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Live P2PMine API — https://p2pmine.com
 * Discovered from production Next.js client + public/config probe.
 */
interface HashCoreApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthLoginRequestDto): AuthResponseDto

    @POST("api/auth/2fa/verify")
    suspend fun verify2fa(@Body body: TwoFaVerifyRequestDto): AuthResponseDto

    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, @JvmSuppressWildcards Any?>): AuthResponseDto

    @GET("api/miner/me")
    suspend fun me(): MeResponseDto

    @PATCH("api/miner/me")
    suspend fun updateMe(@Body body: UpdateMeRequestDto): MeResponseDto

    @GET("api/miner/dashboard")
    suspend fun dashboard(@Query("range") range: String? = "24h"): DashboardDto

    @GET("api/miner/workers")
    suspend fun workers(): WorkersResponseDto

    /**
     * Single worker detail with averages + hashrate history for [range]
     * (`1h` | `24h` | `7d` | `30d`).
     */
    @GET("api/miner/workers/{id}")
    suspend fun workerDetail(
        @retrofit2.http.Path("id") id: String,
        @Query("range") range: String? = "24h",
    ): WorkerDetailResponseDto

    /** Raw body fallback when typed parse fails (tolerant history extraction). */
    @GET("api/miner/workers/{id}")
    suspend fun workerDetailRaw(
        @retrofit2.http.Path("id") id: String,
        @Query("range") range: String? = "24h",
    ): ResponseBody

    /** Per-worker hashrate series only. */
    @GET("api/miner/workers/{id}/history")
    suspend fun workerHistory(
        @retrofit2.http.Path("id") id: String,
        @Query("range") range: String? = "24h",
    ): WorkerHistoryResponseDto

    @GET("api/miner/workers/{id}/history")
    suspend fun workerHistoryRaw(
        @retrofit2.http.Path("id") id: String,
        @Query("range") range: String? = "24h",
    ): ResponseBody

    /** Account (or optional worker-filtered) hashrate history. */
    @GET("api/miner/history")
    suspend fun minerHistory(
        @Query("range") range: String? = "24h",
        @Query("workerId") workerId: String? = null,
    ): WorkerHistoryResponseDto

    @GET("api/miner/history")
    suspend fun minerHistoryRaw(
        @Query("range") range: String? = "24h",
        @Query("workerId") workerId: String? = null,
    ): ResponseBody

    @POST("api/miner/workers")
    suspend fun createWorker(@Body body: Map<String, @JvmSuppressWildcards Any?>): ResponseBody

    @retrofit2.http.DELETE("api/miner/workers/{id}")
    suspend fun deleteWorker(@retrofit2.http.Path("id") id: String): ResponseBody

    /** Request a withdrawal. Empty body = full unpaid balance. */
    @POST("api/miner/payouts")
    suspend fun requestPayout(@Body body: PayoutRequestDto = PayoutRequestDto()): ResponseBody

    @GET("api/miner/alerts")
    suspend fun alerts(): AlertsResponseDto

    @POST("api/miner/alerts/{id}/dismiss")
    suspend fun dismissAlert(@retrofit2.http.Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): ResponseBody

    @GET("api/miner/2fa/status")
    suspend fun twoFaStatus(): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/miner/2fa/setup")
    suspend fun twoFaSetup(): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/miner/2fa/enable")
    suspend fun twoFaEnable(@Body body: Map<String, @JvmSuppressWildcards Any?>): ResponseBody

    @POST("api/miner/2fa/disable")
    suspend fun twoFaDisable(@Body body: Map<String, @JvmSuppressWildcards Any?>): ResponseBody

    @GET("api/miner/observer-links")
    suspend fun observerLinks(): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/miner/observer-links")
    suspend fun createObserverLink(@Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.DELETE("api/miner/observer-links/{id}")
    suspend fun deleteObserverLink(@retrofit2.http.Path("id") id: String): ResponseBody

    @GET("api/public/config")
    suspend fun publicConfig(): PoolSettingsDto
}

