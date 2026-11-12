package com.hashcore.pool.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.RegisterMode
import com.hashcore.pool.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val mode: RegisterMode = RegisterMode.WALLET,
    val email: String = "",
    val password: String = "",
    val btcAddress: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun setMode(mode: RegisterMode) = _state.update { it.copy(mode = mode, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onBtc(v: String) = _state.update { it.copy(btcAddress = v, error = null) }
    fun onDisplayName(v: String) = _state.update { it.copy(displayName = v, error = null) }

    fun register() {
        val s = _state.value
        when (s.mode) {
            RegisterMode.EMAIL -> {
                if (!Validators.isValidEmail(s.email)) {
                    _state.update { it.copy(error = "Enter a valid email") }
                    return
                }
                if (!Validators.isValidPassword(s.password)) {
                    _state.update { it.copy(error = "Password must be at least 8 characters") }
                    return
                }
            }
            RegisterMode.WALLET -> {
                if (s.btcAddress.trim().length < 14) {
                    _state.update { it.copy(error = "Enter a valid BTC wallet address") }
                    return
                }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = authRepository.register(
                mode = s.mode,
                email = s.email.trim().takeIf { it.isNotEmpty() },
                password = s.password.takeIf { it.isNotEmpty() },
                btcAddress = s.btcAddress.trim().takeIf { it.isNotEmpty() },
                displayName = s.displayName.trim().takeIf { it.isNotEmpty() },
            )
            when (result) {
                is AppResult.Success -> _state.update { it.copy(loading = false, success = true) }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
