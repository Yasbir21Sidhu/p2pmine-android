package com.hashcore.pool.presentation.screens.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.EarningsBucket
import com.hashcore.pool.domain.model.EarningsPeriod
import com.hashcore.pool.domain.repository.DashboardRepository
import com.hashcore.pool.domain.repository.EarningsRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EarningsScreenData(
    val period: EarningsPeriod,
    val buckets: List<EarningsBucket>,
    val visibleFeePercent: Double,
    val btcUsdRate: Double?,
    val totalBtc: Double,
)

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val earningsRepository: EarningsRepository,
    private val dashboardRepository: DashboardRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _period = MutableStateFlow(EarningsPeriod.DAILY)
    val period: StateFlow<EarningsPeriod> = _period.asStateFlow()

    private val _ui = MutableStateFlow<UiState<EarningsScreenData>>(UiState.Loading)
    val ui: StateFlow<UiState<EarningsScreenData>> = _ui.asStateFlow()

    val displayFiat = settingsRepository.displayFiat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var feePercent = 1.5
    private var btcUsd: Double? = null

    init {
        load()
    }

    fun setPeriod(period: EarningsPeriod) {
        if (_period.value == period) return
        _period.value = period
        load()
    }

    fun load() {
        viewModelScope.launch {
            _ui.value = UiState.Loading
            // Best-effort fee/rate from dashboard
            when (val dash = dashboardRepository.getDashboard()) {
                is AppResult.Success -> {
                    feePercent = dash.data.visibleFeePercent
                    btcUsd = dash.data.btcUsdRate
                }
                is AppResult.Error -> Unit
            }
            when (val result = earningsRepository.getEarnings(_period.value)) {
                is AppResult.Success -> {
                    val buckets = result.data
                    _ui.value = UiState.Success(
                        EarningsScreenData(
                            period = _period.value,
                            buckets = buckets,
                            visibleFeePercent = feePercent,
                            btcUsdRate = btcUsd,
                            totalBtc = buckets.sumOf { it.amountBtc },
                        ),
                    )
                }
                is AppResult.Error -> _ui.value = UiState.Error(result.message, result.isNetworkError)
            }
        }
    }
}
