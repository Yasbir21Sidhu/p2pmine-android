package com.hashcore.pool.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.data.repository.AuthRepositoryImpl
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val twoFaCode: String = "",
    val needs2fa: Boolean = false,
    val tempToken: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onIdentifierChange(v: String) =
        _state.update { it.copy(identifier = v, error = null) }

    fun onPasswordChange(v: String) =
        _state.update { it.copy(password = v, error = null) }

    fun onTwoFaChange(v: String) =
        _state.update { it.copy(twoFaCode = v, error = null) }

    fun login() {
        val s = _state.value
        if (s.identifier.isBlank()) {
            _state.update { it.copy(error = "Enter email or BTC wallet address") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.login(s.identifier.trim(), s.password)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, success = true) }
                is AppResult.Error -> {
                    if (result.message == "2FA_REQUIRED" || result.message.startsWith("2FA_REQUIRED")) {
                        val impl = authRepository as? AuthRepositoryImpl
                        val token = impl?.pending2faToken.orEmpty()
                        _state.update {
                            it.copy(
                                loading = false,
                                needs2fa = true,
                                tempToken = token,
                                error = null,
                            )
                        }
                    } else {
                        _state.update { it.copy(loading = false, error = result.message) }
                    }
                }
            }
        }
    }

    fun verify2fa() {
        val s = _state.value
        if (s.twoFaCode.isBlank()) {
            _state.update { it.copy(error = "Enter authenticator code") }
            return
        }
        val impl = authRepository as? AuthRepositoryImpl
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = impl?.verify2fa(s.tempToken, s.twoFaCode.trim())
                ?: AppResult.Error("2FA not available")
            when (result) {
                is AppResult.Success -> _state.update { it.copy(loading = false, success = true) }
                is AppResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
