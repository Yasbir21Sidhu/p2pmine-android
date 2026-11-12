package com.hashcore.pool.domain.repository

import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.AuthTokens
import com.hashcore.pool.domain.model.Dashboard
import com.hashcore.pool.domain.model.EarningsBucket
import com.hashcore.pool.domain.model.EarningsPeriod
import com.hashcore.pool.domain.model.LiveUpdate
import com.hashcore.pool.domain.model.Payout
import com.hashcore.pool.domain.model.PoolPublicSettings
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.model.Worker
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val sessionUser: Flow<User?>

    suspend fun login(email: String, password: String): AppResult<User>
    suspend fun register(
        mode: RegisterMode,
        email: String? = null,
        password: String? = null,
        btcAddress: String? = null,
        displayName: String? = null,
    ): AppResult<User>
    suspend fun refreshIfNeeded(): AppResult<AuthTokens>
    suspend fun logout(): AppResult<Unit>
    suspend fun changePassword(current: String, newPassword: String): AppResult<Unit>
    suspend fun updateProfile(
        btcAddress: String? = null,
        fiatCurrency: String? = null,
        displayName: String? = null,
    ): AppResult<User>
    suspend fun getAccessToken(): String?
    suspend fun getStoredUser(): User?
}

enum class RegisterMode {
    WALLET,
    EMAIL,
}

interface DashboardRepository {
    /** @param range one of 1h, 24h, 7d, 30d */
    suspend fun getDashboard(range: String = "24h"): AppResult<Dashboard>
}

data class MinerAlert(
    val id: String,
    val type: String,
    val message: String,
    val createdAtEpochMs: Long?,
    val dismissed: Boolean,
)

interface AlertsRepository {
    suspend fun getAlerts(): AppResult<List<MinerAlert>>
    suspend fun dismissAlert(id: String): AppResult<Unit>
}

interface WorkersRepository {
    suspend fun getWorkers(): AppResult<List<Worker>>
    suspend fun getWorker(id: String): AppResult<Worker>
    suspend fun createWorker(workerName: String): AppResult<Unit>
    /** Soft-disable (pool keeps the row with isActive=false). Not a hard delete. */
    suspend fun deleteWorker(id: String): AppResult<Unit>
}

interface EarningsRepository {
    suspend fun getEarnings(period: EarningsPeriod): AppResult<List<EarningsBucket>>
}

interface PayoutsRepository {
    suspend fun getPayouts(): AppResult<List<Payout>>
    /** Request withdrawal. Null/0 amount = full unpaid balance. */
    suspend fun requestPayout(amountBtc: Double?): AppResult<Unit>
}

interface SettingsRepository {
    suspend fun getPublicSettings(): AppResult<PoolPublicSettings>
    val displayFiat: Flow<Boolean>
    suspend fun setDisplayFiat(enabled: Boolean)
    val fiatCurrency: Flow<String>
    suspend fun setFiatCurrency(code: String)
    val notificationsEnabled: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    val biometricEnabled: Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)
    /** Minutes of background before biometric lock is required again. 0 = every resume. */
    val biometricTimeoutMinutes: Flow<Int>
    suspend fun setBiometricTimeoutMinutes(minutes: Int)
    val amoledBlack: Flow<Boolean>
    suspend fun setAmoledBlack(enabled: Boolean)
    /** Epoch ms until which alerts are muted (0 = not muted). */
    val alertsMutedUntilEpochMs: Flow<Long>
    suspend fun setAlertsMutedUntil(epochMs: Long)
    val firstShareCelebrated: Flow<Boolean>
    suspend fun setFirstShareCelebrated(done: Boolean)
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
}

data class TwoFaStatus(val enabled: Boolean)
data class TwoFaSetup(val secret: String, val qrDataUrl: String?)
data class ObserverLink(val id: String, val label: String?, val url: String?, val createdAt: String?)

interface SecurityExtrasRepository {
    suspend fun twoFaStatus(): AppResult<TwoFaStatus>
    suspend fun twoFaSetup(): AppResult<TwoFaSetup>
    suspend fun twoFaEnable(secret: String, code: String): AppResult<Unit>
    suspend fun twoFaDisable(code: String, password: String): AppResult<Unit>
    suspend fun listObserverLinks(): AppResult<List<ObserverLink>>
    suspend fun createObserverLink(label: String?): AppResult<ObserverLink>
    suspend fun deleteObserverLink(id: String): AppResult<Unit>
}

/**
 * Live updates via WebSocket with polling fallback.
 * Implementations must close the socket when [stop] is called (background).
 */
interface LiveDataRepository {
    val updates: Flow<LiveUpdate>
    val connectionState: Flow<LiveConnectionState>
    fun start()
    fun stop()
}

enum class LiveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    POLLING_FALLBACK,
}
