package com.hashcore.pool.presentation.screens.account

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.SecurityExtrasRepository
import com.hashcore.pool.util.TotpQr
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TwoFaUi(
    val loading: Boolean = false,
    val enabled: Boolean? = null,
    val setupSecret: String? = null,
    /** Server-provided data:image/... or otpauth:// URL */
    val setupQrDataUrl: String? = null,
    /** Client-generated QR bitmap when we have a secret (always preferred for scan UX). */
    val setupQrBitmap: Bitmap? = null,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TwoFaViewModel @Inject constructor(
    private val security: SecurityExtrasRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(TwoFaUi(loading = true))
    val ui: StateFlow<TwoFaUi> = _ui.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = security.twoFaStatus()) {
                is AppResult.Success -> _ui.update {
                    it.copy(
                        loading = false,
                        enabled = r.data.enabled,
                        setupSecret = null,
                        setupQrDataUrl = null,
                        setupQrBitmap = null,
                    )
                }
                is AppResult.Error -> _ui.update {
                    it.copy(loading = false, error = r.message, enabled = false)
                }
            }
        }
    }

    fun startSetup() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            when (val r = security.twoFaSetup()) {
                is AppResult.Success -> {
                    val user = authRepository.getStoredUser()
                    val label = user?.email?.takeIf { it.isNotBlank() }
                        ?: user?.displayName?.takeIf { it.isNotBlank() }
                        ?: user?.walletAddress?.take(12)
                        ?: "miner"
                    val otpauth = TotpQr.otpAuthUri(r.data.secret, accountLabel = label)
                    val bitmap = withContext(Dispatchers.Default) {
                        // Prefer encoding otpauth so any authenticator can scan it.
                        TotpQr.encodeBitmap(otpauth)
                    }
                    _ui.update {
                        it.copy(
                            loading = false,
                            setupSecret = r.data.secret,
                            setupQrDataUrl = r.data.qrDataUrl,
                            setupQrBitmap = bitmap,
                        )
                    }
                }
                is AppResult.Error -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
            }
        }
    }

    fun enable(code: String) {
        val secret = _ui.value.setupSecret ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            when (val r = security.twoFaEnable(secret, code.trim())) {
                is AppResult.Success -> _ui.update {
                    it.copy(
                        loading = false,
                        enabled = true,
                        setupSecret = null,
                        setupQrDataUrl = null,
                        setupQrBitmap = null,
                        message = "Two-factor authentication enabled.",
                    )
                }
                is AppResult.Error -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
            }
        }
    }

    fun disable(code: String, password: String) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, message = null) }
            when (val r = security.twoFaDisable(code.trim(), password)) {
                is AppResult.Success -> _ui.update {
                    it.copy(
                        loading = false,
                        enabled = false,
                        message = "Two-factor authentication disabled.",
                    )
                }
                is AppResult.Error -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
            }
        }
    }
}
