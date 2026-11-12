package com.hashcore.pool.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hashcore.pool.domain.model.AuthTokens
import com.hashcore.pool.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tokens + minimal user profile in EncryptedSharedPreferences (Android Keystore-backed master key).
 * Excluded from cloud backup via backup rules.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Fallback only if Keystore is unavailable (rare); still private mode
        context.getSharedPreferences(FILE_NAME + "_fallback", Context.MODE_PRIVATE)
    }

    @Synchronized
    fun saveTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putLong(KEY_ACCESS_EXP, tokens.accessExpiresAtEpochMs ?: 0L)
            .apply()
    }

    @Synchronized
    fun getTokens(): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val exp = prefs.getLong(KEY_ACCESS_EXP, 0L).takeIf { it > 0 }
        return AuthTokens(access, refresh, exp)
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun isAccessExpiringSoon(skewMs: Long = 60_000L): Boolean {
        val exp = prefs.getLong(KEY_ACCESS_EXP, 0L)
        if (exp <= 0L) return false
        return System.currentTimeMillis() >= exp - skewMs
    }

    @Synchronized
    fun saveUser(user: User) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_WALLET, user.walletAddress)
            .putString(KEY_USER_FIAT, user.fiatCurrency)
            .apply()
    }

    fun getUser(): User? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        return User(
            id = id,
            email = email,
            displayName = prefs.getString(KEY_USER_NAME, null),
            walletAddress = prefs.getString(KEY_USER_WALLET, null),
            fiatCurrency = prefs.getString(KEY_USER_FIAT, null),
        )
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasSession(): Boolean =
        !prefs.getString(KEY_ACCESS, null).isNullOrBlank()

    companion object {
        private const val FILE_NAME = "hashcore_secure_prefs"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_ACCESS_EXP = "access_exp"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_WALLET = "user_wallet"
        private const val KEY_USER_FIAT = "user_fiat"
    }
}
