package com.hashcore.pool.presentation.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.FiatCurrency
import com.hashcore.pool.domain.model.PoolPublicSettings
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val user: User? = null,
    val settings: PoolPublicSettings? = null,
    val message: String? = null,
    val error: String? = null,
    val loggedOut: Boolean = false,
    val passwordChanging: Boolean = false,
    val passwordChanged: Boolean = false,
    val fiatSaving: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AccountUiState())
    val ui: StateFlow<AccountUiState> = _ui.asStateFlow()

    val displayFiat = settingsRepository.displayFiat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val fiatCurrency = settingsRepository.fiatCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "USD")
    val notifications = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val biometric = settingsRepository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val biometricTimeout = settingsRepository.biometricTimeoutMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)
    val amoledBlack = settingsRepository.amoledBlack
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val mutedUntil = settingsRepository.alertsMutedUntilEpochMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    init {
        viewModelScope.launch {
            authRepository.sessionUser.collect { user ->
                _ui.update { it.copy(user = user ?: authRepository.getStoredUser()) }
            }
        }
        viewModelScope.launch {
            when (val s = settingsRepository.getPublicSettings()) {
                is AppResult.Success -> {
                    _ui.update { it.copy(settings = s.data) }
                    val userFiat = authRepository.getStoredUser()?.fiatCurrency
                    if (!userFiat.isNullOrBlank()) {
                        settingsRepository.setFiatCurrency(userFiat)
                    }
                }
                is AppResult.Error -> Unit
            }
        }
    }

    fun setDisplayFiat(v: Boolean) = viewModelScope.launch {
        settingsRepository.setDisplayFiat(v)
    }

    fun setFiatCurrency(code: String) = viewModelScope.launch {
        _ui.update { it.copy(fiatSaving = true, error = null, message = null) }
        settingsRepository.setFiatCurrency(code)
        when (val r = authRepository.updateProfile(fiatCurrency = code)) {
            is AppResult.Success -> {
                _ui.update {
                    it.copy(
                        fiatSaving = false,
                        user = r.data,
                        message = "Display currency set to $code",
                    )
                }
            }
            is AppResult.Error -> {
                _ui.update {
                    it.copy(
                        fiatSaving = false,
                        error = "Saved locally; server sync failed: ${r.message}",
                    )
                }
            }
        }
    }

    fun setNotifications(v: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationsEnabled(v)
    }

    fun setBiometric(v: Boolean) = viewModelScope.launch {
        settingsRepository.setBiometricEnabled(v)
    }

    fun setBiometricTimeout(minutes: Int) = viewModelScope.launch {
        settingsRepository.setBiometricTimeoutMinutes(minutes)
    }

    fun setAmoledBlack(v: Boolean) = viewModelScope.launch {
        settingsRepository.setAmoledBlack(v)
    }

    fun muteAlerts24h() = viewModelScope.launch {
        settingsRepository.setAlertsMutedUntil(System.currentTimeMillis() + 24L * 60 * 60 * 1000)
        _ui.update { it.copy(message = "Alerts muted for 24 hours") }
    }

    fun unmuteAlerts() = viewModelScope.launch {
        settingsRepository.setAlertsMutedUntil(0L)
        _ui.update { it.copy(message = "Alerts unmuted") }
    }

    fun availableFiats(): List<FiatCurrency> {
        val list = _ui.value.settings?.fiatCurrencies.orEmpty()
        if (list.isNotEmpty()) return list
        return listOf(
            FiatCurrency("USD", "US Dollar", "$", 1.0),
            FiatCurrency("INR", "Indian Rupee", "₹", 96.0),
            FiatCurrency("EUR", "Euro", "€", 0.88),
        )
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _ui.update { it.copy(loggedOut = true) }
        }
    }

    fun changePassword(current: String, newPass: String, confirm: String) {
        if (!Validators.isValidPassword(newPass)) {
            _ui.update { it.copy(error = "New password must be at least 8 characters.") }
            return
        }
        if (newPass != confirm) {
            _ui.update { it.copy(error = "Passwords do not match.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(passwordChanging = true, error = null, passwordChanged = false) }
            when (val r = authRepository.changePassword(current, newPass)) {
                is AppResult.Success -> _ui.update {
                    it.copy(passwordChanging = false, passwordChanged = true, message = "Password updated.")
                }
                is AppResult.Error -> _ui.update {
                    it.copy(passwordChanging = false, error = r.message)
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(message = null, error = null, passwordChanged = false) }
}
