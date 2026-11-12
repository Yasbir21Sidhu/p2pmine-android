package com.hashcore.pool.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hashcore_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val displayFiat = booleanPreferencesKey("display_fiat")
        val fiatCurrency = stringPreferencesKey("fiat_currency")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val biometric = booleanPreferencesKey("biometric_enabled")
        val biometricTimeout = intPreferencesKey("biometric_timeout_min")
        val amoledBlack = booleanPreferencesKey("amoled_black")
        val alertsMutedUntil = longPreferencesKey("alerts_muted_until")
        val firstShareCelebrated = booleanPreferencesKey("first_share_celebrated")
        val onboardingDone = booleanPreferencesKey("onboarding_completed")
    }

    val displayFiat: Flow<Boolean> = context.dataStore.data.map { it[Keys.displayFiat] ?: false }
    val fiatCurrency: Flow<String> = context.dataStore.data.map {
        it[Keys.fiatCurrency] ?: defaultFiatForDevice()
    }
    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.notifications] ?: true }
    val biometricEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.biometric] ?: false }
    val biometricTimeoutMinutes: Flow<Int> =
        context.dataStore.data.map { it[Keys.biometricTimeout] ?: 5 }
    val amoledBlack: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.amoledBlack] ?: false }
    val alertsMutedUntilEpochMs: Flow<Long> =
        context.dataStore.data.map { it[Keys.alertsMutedUntil] ?: 0L }
    val firstShareCelebrated: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.firstShareCelebrated] ?: false }
    val onboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.onboardingDone] ?: false }

    suspend fun setDisplayFiat(enabled: Boolean) {
        context.dataStore.edit { it[Keys.displayFiat] = enabled }
    }

    suspend fun setFiatCurrency(code: String) {
        context.dataStore.edit { it[Keys.fiatCurrency] = code.uppercase() }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.notifications] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.biometric] = enabled }
    }

    suspend fun setBiometricTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.biometricTimeout] = minutes.coerceIn(0, 120) }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.amoledBlack] = enabled }
    }

    suspend fun setAlertsMutedUntil(epochMs: Long) {
        context.dataStore.edit { it[Keys.alertsMutedUntil] = epochMs }
    }

    suspend fun setFirstShareCelebrated(done: Boolean) {
        context.dataStore.edit { it[Keys.firstShareCelebrated] = done }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.onboardingDone] = completed }
    }

    private fun defaultFiatForDevice(): String {
        val country = try {
            java.util.Locale.getDefault().country
        } catch (_: Exception) {
            ""
        }
        return if (country.equals("IN", ignoreCase = true)) "INR" else "USD"
    }
}
