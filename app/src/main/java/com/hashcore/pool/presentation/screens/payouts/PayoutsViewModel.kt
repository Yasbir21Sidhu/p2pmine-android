package com.hashcore.pool.presentation.screens.payouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.Dashboard
import com.hashcore.pool.domain.model.Payout
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.DashboardRepository
import com.hashcore.pool.domain.repository.PayoutsRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayoutsScreenData(
    val payouts: List<Payout>,
    val unpaidBalanceBtc: Double,
    val minPayoutBtc: Double,
    val payoutSchedule: String,
    val btcUsdRate: Double?,
    val visibleFeePercent: Double,
    val btcAddress: String?,
)

data class PayoutRequestUi(
    val amountText: String = "",
    val addressText: String = "",
    /**
     * When false and a saved address exists, the field is locked (view mode).
     * True = user is adding/changing the address.
     */
    val addressEditing: Boolean = true,
    val addressSaving: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    /** Confirm dialog before submitting a payout request. */
    val showPayoutConfirm: Boolean = false,
    /** Parsed amount for the confirm dialog; null means full unpaid balance. */
    val confirmAmountBtc: Double? = null,
)

@HiltViewModel
class PayoutsViewModel @Inject constructor(
    private val payoutsRepository: PayoutsRepository,
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow<UiState<PayoutsScreenData>>(UiState.Loading)
    val ui: StateFlow<UiState<PayoutsScreenData>> = _ui.asStateFlow()

    private val _request = MutableStateFlow(PayoutRequestUi())
    val request: StateFlow<PayoutRequestUi> = _request.asStateFlow()

    val displayFiat = settingsRepository.displayFiat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load()
    }

    fun onAmountChange(v: String) {
        _request.update {
            it.copy(amountText = v.filter { ch -> ch.isDigit() || ch == '.' }, error = null, message = null)
        }
    }

    fun onAddressChange(v: String) {
        if (!_request.value.addressEditing) return
        _request.update { it.copy(addressText = v.trim(), error = null, message = null) }
    }

    /**
     * Switch to edit mode to replace the stored address.
     * Field is cleared so the user must re-type the full address (no soft edit of one character).
     */
    fun startEditAddress() {
        _request.update {
            it.copy(
                addressEditing = true,
                addressText = "",
                error = null,
                message = null,
                showPayoutConfirm = false,
            )
        }
    }

    /** Cancel edit and restore the last saved address (if any). */
    fun cancelEditAddress() {
        val saved = (_ui.value as? UiState.Success)?.data?.btcAddress
        _request.update {
            it.copy(
                addressEditing = saved.isNullOrBlank(),
                addressText = saved.orEmpty(),
                error = null,
                message = null,
            )
        }
    }

    fun load() {
        viewModelScope.launch {
            if (_ui.value !is UiState.Success) _ui.value = UiState.Loading
            // Refresh profile so payout address is current
            val profile = authRepository.getStoredUser()
            val refreshed = try {
                // best-effort refresh from server if available via me()
                when (val r = (authRepository as? com.hashcore.pool.data.repository.AuthRepositoryImpl)
                    ?.refreshProfile()) {
                    is AppResult.Success -> r.data
                    else -> profile
                }
            } catch (_: Exception) {
                profile
            }

            val payoutsDeferred = async { payoutsRepository.getPayouts() }
            val dashDeferred = async { dashboardRepository.getDashboard() }
            val payoutsResult = payoutsDeferred.await()
            val dashResult = dashDeferred.await()
            val dash: Dashboard? = (dashResult as? AppResult.Success)?.data
            val user: User? = refreshed ?: authRepository.getStoredUser()

            when (payoutsResult) {
                is AppResult.Success -> {
                    val data = PayoutsScreenData(
                        payouts = payoutsResult.data,
                        unpaidBalanceBtc = dash?.unpaidBalanceBtc ?: 0.0,
                        minPayoutBtc = dash?.minPayoutBtc ?: 0.0001,
                        payoutSchedule = dash?.payoutSchedule.orEmpty(),
                        btcUsdRate = dash?.btcUsdRate,
                        visibleFeePercent = dash?.visibleFeePercent ?: 1.5,
                        btcAddress = user?.walletAddress,
                    )
                    _ui.value = UiState.Success(data)
                    // Keep draft while user is typing a new address; otherwise lock to saved.
                    val midEdit = _request.value.addressEditing && !_request.value.addressSaving
                    if (!midEdit) {
                        val saved = data.btcAddress
                        _request.update {
                            it.copy(
                                addressText = saved.orEmpty(),
                                addressEditing = saved.isNullOrBlank(),
                            )
                        }
                    }
                }
                is AppResult.Error -> {
                    if (_ui.value !is UiState.Success) {
                        _ui.value = UiState.Error(payoutsResult.message, payoutsResult.isNetworkError)
                    }
                }
            }
        }
    }

    fun savePayoutAddress() {
        val address = _request.value.addressText.trim()
        if (address.isBlank()) {
            _request.update { it.copy(error = "Enter a BTC receiving address.") }
            return
        }
        // Light validation: bech32 or legacy-ish
        if (address.length < 14) {
            _request.update { it.copy(error = "That does not look like a valid BTC address.") }
            return
        }
        viewModelScope.launch {
            _request.update { it.copy(addressSaving = true, error = null, message = null) }
            when (val r = authRepository.updateProfile(btcAddress = address)) {
                is AppResult.Success -> {
                    val saved = r.data.walletAddress?.takeIf { it.isNotBlank() } ?: address
                    _request.update {
                        it.copy(
                            addressSaving = false,
                            addressEditing = false,
                            addressText = saved,
                            message = "Payout address saved. Future withdrawals go here.",
                        )
                    }
                    // Refresh summary card; keep locked view mode.
                    load()
                }
                is AppResult.Error -> {
                    _request.update { it.copy(addressSaving = false, error = r.message) }
                }
            }
        }
    }

    /**
     * Validate and open the confirmation dialog.
     * Payouts always go to the **saved** address only (never a draft in the edit field).
     */
    fun requestPayout() {
        val data = (_ui.value as? UiState.Success)?.data
        if (_request.value.addressEditing) {
            _request.update {
                it.copy(
                    error = if (data?.btcAddress.isNullOrBlank()) {
                        "Save a BTC receiving address before requesting a payout."
                    } else {
                        "Finish or cancel address change before requesting a payout."
                    },
                    showPayoutConfirm = false,
                )
            }
            return
        }
        val address = data?.btcAddress?.takeIf { it.isNotBlank() }
        if (address.isNullOrBlank()) {
            _request.update {
                it.copy(error = "Set a BTC receiving address above before requesting a payout.")
            }
            return
        }
        if ((data.unpaidBalanceBtc) < (data.minPayoutBtc)) {
            _request.update {
                it.copy(error = "Unpaid balance is below the minimum payout threshold.")
            }
            return
        }
        val amountText = _request.value.amountText.trim()
        val amount = if (amountText.isBlank()) {
            null
        } else {
            amountText.toDoubleOrNull() ?: run {
                _request.update { it.copy(error = "Enter a valid amount in BTC, or leave empty for full balance.") }
                return
            }
        }
        if (amount != null && amount <= 0) {
            _request.update { it.copy(error = "Amount must be greater than zero.") }
            return
        }
        if (amount != null && amount > data.unpaidBalanceBtc) {
            _request.update { it.copy(error = "Amount is higher than your unpaid balance.") }
            return
        }
        _request.update {
            it.copy(
                error = null,
                message = null,
                showPayoutConfirm = true,
                confirmAmountBtc = amount,
            )
        }
    }

    fun dismissPayoutConfirm() {
        _request.update {
            it.copy(showPayoutConfirm = false, confirmAmountBtc = null)
        }
    }

    /** User confirmed destination address + amount in the dialog. */
    fun confirmRequestPayout() {
        val data = (_ui.value as? UiState.Success)?.data ?: return
        val address = data.btcAddress?.takeIf { it.isNotBlank() }
        if (address.isNullOrBlank()) {
            _request.update {
                it.copy(
                    showPayoutConfirm = false,
                    error = "Set a BTC receiving address above before requesting a payout.",
                )
            }
            return
        }
        val amount = _request.value.confirmAmountBtc
        viewModelScope.launch {
            _request.update {
                it.copy(loading = true, showPayoutConfirm = false, error = null, message = null)
            }
            when (val r = payoutsRepository.requestPayout(amount)) {
                is AppResult.Success -> {
                    _request.update {
                        it.copy(
                            loading = false,
                            message = "Payout requested. Operator typically processes within 24h.",
                            amountText = "",
                            confirmAmountBtc = null,
                        )
                    }
                    load()
                }
                is AppResult.Error -> {
                    _request.update {
                        it.copy(
                            loading = false,
                            error = r.message,
                            confirmAmountBtc = null,
                        )
                    }
                }
            }
        }
    }
}
