package com.hashcore.pool.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.data.local.prefs.SecureTokenStore
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionState {
    data object Loading : SessionState()
    data class Ready(
        val loggedIn: Boolean,
        val onboardingDone: Boolean,
        val biometricEnabled: Boolean,
        val unlockedThisSession: Boolean,
    ) : SessionState() {
        val needsBiometric: Boolean
            get() = loggedIn && biometricEnabled && !unlockedThisSession
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val tokenStore: SecureTokenStore,
) : ViewModel() {

    private val unlocked = MutableStateFlow(false)
    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    /** Epoch ms when app last went to background while unlocked. */
    private var backgroundedAt: Long = 0L

    val amoledBlack = settingsRepository.amoledBlack

    init {
        viewModelScope.launch {
            combine(
                authRepository.isLoggedIn,
                settingsRepository.onboardingCompleted,
                settingsRepository.biometricEnabled,
                unlocked,
            ) { loggedIn, onboarding, biometric, isUnlocked ->
                SessionState.Ready(
                    loggedIn = loggedIn || tokenStore.hasSession(),
                    onboardingDone = onboarding,
                    biometricEnabled = biometric,
                    unlockedThisSession = isUnlocked,
                )
            }.collect { _session.value = it }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun markUnlocked() {
        unlocked.value = true
        backgroundedAt = 0L
    }

    fun refreshSession() {
        // Flow will emit; unlock after fresh login
        unlocked.value = true
        backgroundedAt = 0L
    }

    fun logoutLocal() {
        viewModelScope.launch {
            authRepository.logout()
            unlocked.value = false
            backgroundedAt = 0L
        }
    }

    /** Call when process moves to background while user is in main app. */
    fun onAppBackgrounded() {
        if (unlocked.value) {
            backgroundedAt = System.currentTimeMillis()
        }
    }

    /**
     * Call when process returns to foreground. Re-locks if biometric enabled and
     * timeout minutes elapsed (0 = lock every time).
     */
    fun onAppForegrounded() {
        viewModelScope.launch {
            val ready = _session.value as? SessionState.Ready ?: return@launch
            if (!ready.loggedIn || !ready.biometricEnabled || !unlocked.value) return@launch
            val timeoutMin = settingsRepository.biometricTimeoutMinutes.first()
            val bg = backgroundedAt
            if (bg <= 0L) return@launch
            val elapsedMin = (System.currentTimeMillis() - bg) / 60_000L
            if (timeoutMin == 0 || elapsedMin >= timeoutMin) {
                unlocked.value = false
            }
            backgroundedAt = 0L
        }
    }
}
