package com.hashcore.pool.data.repository

import com.hashcore.pool.BuildConfig
import com.hashcore.pool.data.local.prefs.SecureTokenStore
import com.hashcore.pool.data.remote.DemoData
import com.hashcore.pool.data.remote.NetworkErrorMapper
import com.hashcore.pool.data.remote.api.HashCoreApi
import com.hashcore.pool.data.remote.dto.AuthLoginRequestDto
import com.hashcore.pool.data.remote.dto.TwoFaVerifyRequestDto
import com.hashcore.pool.data.remote.dto.toTokens
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.AuthTokens
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.RegisterMode
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val tokenStore: SecureTokenStore,
    private val moshi: Moshi,
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(tokenStore.hasSession())
    private val _user = MutableStateFlow(tokenStore.getUser())

    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()
    override val sessionUser: Flow<User?> = _user.asStateFlow()

    /** Pending 2FA temp token if login requires second step. */
    @Volatile
    var pending2faToken: String? = null
        private set

    override suspend fun register(
        mode: RegisterMode,
        email: String?,
        password: String?,
        btcAddress: String?,
        displayName: String?,
    ): AppResult<User> {
        if (BuildConfig.DEMO_MODE) {
            delay(500)
            val id = email?.trim().orEmpty().ifBlank { btcAddress?.trim().orEmpty() }
            if (id.isBlank()) return AppResult.Error("Email or wallet is required.")
            tokenStore.saveTokens(DemoData.tokens)
            val user = DemoData.user.copy(
                email = email?.trim().orEmpty().ifBlank { id },
                walletAddress = btcAddress?.trim(),
                displayName = displayName?.trim(),
            )
            tokenStore.saveUser(user)
            _user.value = user
            _isLoggedIn.value = true
            return AppResult.Success(user)
        }
        return try {
            val body: Map<String, Any?> = when (mode) {
                RegisterMode.EMAIL -> mapOf(
                    "email" to email?.trim(),
                    "password" to password,
                    "displayName" to displayName?.trim()?.takeIf { it.isNotEmpty() },
                    "btcAddress" to btcAddress?.trim()?.takeIf { it.isNotEmpty() },
                )
                RegisterMode.WALLET -> mapOf(
                    "btcAddress" to btcAddress?.trim(),
                    "displayName" to displayName?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
            val res = api.register(body)
            val token = res.token
            val userDto = res.user
            if (token.isNullOrBlank() || userDto == null) {
                return AppResult.Error(res.error ?: res.message ?: "Registration failed")
            }
            tokenStore.saveTokens(res.toTokens())
            val user = userDto.toDomain()
            tokenStore.saveUser(user)
            _user.value = user
            _isLoggedIn.value = true
            AppResult.Success(user)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        // email param is identifier (email OR wallet) for P2PMine
        if (BuildConfig.DEMO_MODE) {
            delay(600)
            if (email.isBlank()) return AppResult.Error("Email or wallet is required.")
            tokenStore.saveTokens(DemoData.tokens)
            val user = DemoData.user.copy(email = email.trim())
            tokenStore.saveUser(user)
            _user.value = user
            _isLoggedIn.value = true
            return AppResult.Success(user)
        }
        return try {
            val res = api.login(
                AuthLoginRequestDto(
                    identifier = email.trim(),
                    password = password.takeIf { it.isNotBlank() },
                ),
            )
            if (res.needs2fa()) {
                // Keep temp token only in memory (never embed secrets in error strings / logs)
                pending2faToken = res.resolvedTempToken()
                return AppResult.Error(
                    message = "2FA_REQUIRED",
                    isAuthError = false,
                )
            }
            val token = res.token
            val userDto = res.user
            if (token.isNullOrBlank() || userDto == null) {
                return AppResult.Error(res.error ?: res.message ?: "Unexpected login response")
            }
            tokenStore.saveTokens(res.toTokens())
            val user = userDto.toDomain()
            tokenStore.saveUser(user)
            _user.value = user
            _isLoggedIn.value = true
            pending2faToken = null
            AppResult.Success(user)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    suspend fun verify2fa(tempToken: String, code: String): AppResult<User> {
        return try {
            val res = api.verify2fa(TwoFaVerifyRequestDto(tempToken, code))
            val token = res.token
            val userDto = res.user
            if (token.isNullOrBlank() || userDto == null) {
                return AppResult.Error(res.error ?: res.message ?: "2FA verification failed")
            }
            tokenStore.saveTokens(res.toTokens())
            val user = userDto.toDomain()
            tokenStore.saveUser(user)
            _user.value = user
            _isLoggedIn.value = true
            pending2faToken = null
            AppResult.Success(user)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun refreshIfNeeded(): AppResult<AuthTokens> {
        val existing = tokenStore.getTokens()
            ?: return AppResult.Error("Not signed in.", isAuthError = true)
        // P2PMine uses a single long-lived token (no refresh endpoint)
        return AppResult.Success(existing)
    }

    override suspend fun logout(): AppResult<Unit> {
        tokenStore.clear()
        _isLoggedIn.value = false
        _user.value = null
        pending2faToken = null
        return AppResult.Success(Unit)
    }

    override suspend fun changePassword(current: String, newPassword: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(400)
            return AppResult.Success(Unit)
        }
        return try {
            api.updateMe(
                com.hashcore.pool.data.remote.dto.UpdateMeRequestDto(password = newPassword),
            )
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun updateProfile(
        btcAddress: String?,
        fiatCurrency: String?,
        displayName: String?,
    ): AppResult<User> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            val current = tokenStore.getUser() ?: DemoData.user
            val updated = current.copy(
                walletAddress = btcAddress ?: current.walletAddress,
                fiatCurrency = fiatCurrency ?: current.fiatCurrency,
                displayName = displayName ?: current.displayName,
            )
            tokenStore.saveUser(updated)
            _user.value = updated
            return AppResult.Success(updated)
        }
        return try {
            val res = api.updateMe(
                com.hashcore.pool.data.remote.dto.UpdateMeRequestDto(
                    btcAddress = btcAddress,
                    fiatCurrency = fiatCurrency,
                    displayName = displayName,
                ),
            )
            val user = res.user.toDomain()
            tokenStore.saveUser(user)
            _user.value = user
            AppResult.Success(user)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun getAccessToken(): String? = tokenStore.getAccessToken()

    override suspend fun getStoredUser(): User? = tokenStore.getUser()

    suspend fun refreshProfile(): AppResult<User> {
        return try {
            val me = api.me()
            val user = me.user.toDomain()
            tokenStore.saveUser(user)
            _user.value = user
            AppResult.Success(user)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }
}
