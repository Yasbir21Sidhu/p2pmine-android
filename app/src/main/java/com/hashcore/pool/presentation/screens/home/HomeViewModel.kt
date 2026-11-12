package com.hashcore.pool.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.data.local.prefs.WidgetSnapshotStore
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.Dashboard
import com.hashcore.pool.domain.repository.AlertsRepository
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.DashboardRepository
import com.hashcore.pool.domain.repository.LiveConnectionState
import com.hashcore.pool.domain.repository.LiveDataRepository
import com.hashcore.pool.domain.repository.MinerAlert
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.domain.repository.WorkersRepository
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupProgress(
    val registered: Boolean,
    val hasWorker: Boolean,
    val hasShare: Boolean,
    val hasPayoutAddress: Boolean,
) {
    val allDone: Boolean get() = registered && hasWorker && hasShare && hasPayoutAddress
}

data class AlertGroup(
    val typeKey: String,
    val title: String,
    val alerts: List<MinerAlert>,
)

sealed class HomeEvent {
    data object FirstShare : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val liveDataRepository: LiveDataRepository,
    private val alertsRepository: AlertsRepository,
    private val workersRepository: WorkersRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val widgetSnapshotStore: WidgetSnapshotStore,
) : ViewModel() {

    private val _ui = MutableStateFlow<UiState<Dashboard>>(UiState.Loading)
    val ui: StateFlow<UiState<Dashboard>> = _ui.asStateFlow()

    private val _range = MutableStateFlow("24h")
    val range: StateFlow<String> = _range.asStateFlow()

    private val _alerts = MutableStateFlow<List<MinerAlert>>(emptyList())
    val alerts: StateFlow<List<MinerAlert>> = _alerts.asStateFlow()

    private val _alertGroups = MutableStateFlow<List<AlertGroup>>(emptyList())
    val alertGroups: StateFlow<List<AlertGroup>> = _alertGroups.asStateFlow()

    private val _lastUpdatedEpochMs = MutableStateFlow(0L)
    val lastUpdatedEpochMs: StateFlow<Long> = _lastUpdatedEpochMs.asStateFlow()

    private val _hashrateFlashToken = MutableStateFlow(0L)
    val hashrateFlashToken: StateFlow<Long> = _hashrateFlashToken.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val notifiedAlertIds = mutableSetOf<String>()
    private var previousHashrate: Double? = null

    private val _setup = MutableStateFlow(
        SetupProgress(registered = true, hasWorker = false, hasShare = false, hasPayoutAddress = false),
    )
    val setup: StateFlow<SetupProgress> = _setup.asStateFlow()

    val connectionState = liveDataRepository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveConnectionState.DISCONNECTED)

    val displayFiat = settingsRepository.displayFiat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val fiatCurrency = settingsRepository.fiatCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "INR")

    val mutedUntil = settingsRepository.alertsMutedUntilEpochMs
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val notificationsEnabled = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val firstShareCelebrated = settingsRepository.firstShareCelebrated
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        notificationHelper.ensureChannels()
        load()
        viewModelScope.launch {
            liveDataRepository.updates.collect { update ->
                val current = (_ui.value as? UiState.Success)?.data ?: return@collect
                val newHr = update.currentHashrateHs ?: current.currentHashrateHs
                if (previousHashrate != null &&
                    kotlin.math.abs(newHr - (previousHashrate ?: 0.0)) > 1.0
                ) {
                    _hashrateFlashToken.value = System.currentTimeMillis()
                }
                previousHashrate = newHr
                val merged = current.copy(
                    currentHashrateHs = newHr,
                    avg24hHashrateHs = update.avg24hHashrateHs ?: current.avg24hHashrateHs,
                    workers = update.workersSummary ?: current.workers,
                )
                _ui.value = UiState.Success(merged)
                _lastUpdatedEpochMs.value = update.timestampEpochMs
                widgetSnapshotStore.save(merged)
                maybeCelebrateFirstShare(merged)
                refreshSetupFlags(merged)
            }
        }
    }

    fun onScreenVisible(visible: Boolean) {
        if (visible) liveDataRepository.start() else liveDataRepository.stop()
    }

    fun setRange(range: String) {
        if (_range.value == range) return
        _range.value = range
        load(forceLoading = false)
    }

    fun load(forceLoading: Boolean = true) {
        viewModelScope.launch {
            if (forceLoading || _ui.value !is UiState.Success) {
                _ui.value = UiState.Loading
            }
            when (val result = dashboardRepository.getDashboard(_range.value)) {
                is AppResult.Success -> {
                    val data = result.data
                    val prevHr = previousHashrate
                    if (prevHr != null && kotlin.math.abs(data.currentHashrateHs - prevHr) > 1.0) {
                        _hashrateFlashToken.value = System.currentTimeMillis()
                    }
                    previousHashrate = data.currentHashrateHs
                    _ui.value = UiState.Success(data)
                    _lastUpdatedEpochMs.value = System.currentTimeMillis()
                    widgetSnapshotStore.save(data)
                    maybeCelebrateFirstShare(data)
                    refreshSetupFlags(data)
                }
                is AppResult.Error -> {
                    if (_ui.value !is UiState.Success) {
                        _ui.value = UiState.Error(result.message, result.isNetworkError)
                    }
                }
            }
            loadAlerts()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            when (val result = dashboardRepository.getDashboard(_range.value)) {
                is AppResult.Success -> {
                    previousHashrate = result.data.currentHashrateHs
                    _ui.value = UiState.Success(result.data)
                    _lastUpdatedEpochMs.value = System.currentTimeMillis()
                    widgetSnapshotStore.save(result.data)
                    maybeCelebrateFirstShare(result.data)
                    refreshSetupFlags(result.data)
                }
                is AppResult.Error -> Unit
            }
            loadAlerts()
            _refreshing.value = false
        }
    }

    fun muteAlerts24h() {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + 24L * 60 * 60 * 1000
            settingsRepository.setAlertsMutedUntil(until)
        }
    }

    fun unmuteAlerts() {
        viewModelScope.launch {
            settingsRepository.setAlertsMutedUntil(0L)
        }
    }

    private suspend fun loadAlerts() {
        when (val r = alertsRepository.getAlerts()) {
            is AppResult.Success -> {
                val active = r.data.filter { !it.dismissed }
                _alerts.value = active
                _alertGroups.value = groupAlerts(active)
                active.forEach { alert ->
                    val important = alert.type.contains("offline", true) ||
                        alert.type.contains("reject", true) ||
                        alert.type.contains("payout", true)
                    if (important &&
                        notificationsEnabled.value &&
                        notifiedAlertIds.add(alert.id)
                    ) {
                        notificationHelper.notifyAlert(alert, mutedUntil.value)
                    }
                }
            }
            is AppResult.Error -> Unit
        }
    }

    private fun groupAlerts(alerts: List<MinerAlert>): List<AlertGroup> {
        return alerts.groupBy { typeKey(it) }.map { (key, list) ->
            AlertGroup(
                typeKey = key,
                title = when {
                    key.contains("offline") -> "Workers offline (${list.size})"
                    key.contains("reject") -> "High reject (${list.size})"
                    key.contains("payout") -> "Payouts (${list.size})"
                    else -> "Alerts (${list.size})"
                },
                alerts = list,
            )
        }.sortedByDescending { it.alerts.maxOfOrNull { a -> a.createdAtEpochMs ?: 0L } ?: 0L }
    }

    private fun typeKey(alert: MinerAlert): String = when {
        alert.type.contains("offline", true) -> "offline"
        alert.type.contains("reject", true) -> "reject"
        alert.type.contains("payout", true) -> "payout"
        else -> alert.type.ifBlank { "other" }.lowercase()
    }

    fun dismissAlert(id: String) {
        viewModelScope.launch {
            alertsRepository.dismissAlert(id)
            _alerts.value = _alerts.value.filter { it.id != id }
            _alertGroups.value = groupAlerts(_alerts.value)
        }
    }

    fun dismissGroup(typeKey: String) {
        viewModelScope.launch {
            val ids = _alerts.value.filter { typeKey(it) == typeKey }.map { it.id }
            ids.forEach { alertsRepository.dismissAlert(it) }
            _alerts.value = _alerts.value.filter { typeKey(it) != typeKey }
            _alertGroups.value = groupAlerts(_alerts.value)
        }
    }

    private suspend fun maybeCelebrateFirstShare(dash: Dashboard) {
        if (firstShareCelebrated.value) return
        val hasShare = dash.currentHashrateHs > 0 ||
            dash.hashrateHistory24h.any { it.hashrateHs > 0 }
        if (!hasShare) return
        settingsRepository.setFirstShareCelebrated(true)
        _events.emit(HomeEvent.FirstShare)
    }

    private suspend fun refreshSetupFlags(dash: Dashboard) {
        val user = authRepository.getStoredUser()
        val workers = when (val w = workersRepository.getWorkers()) {
            is AppResult.Success -> w.data
            is AppResult.Error -> emptyList()
        }
        val hasShare = dash.currentHashrateHs > 0 ||
            workers.any { (it.lastShareEpochMs ?: 0) > 0 } ||
            dash.hashrateHistory24h.any { it.hashrateHs > 0 }
        _setup.value = SetupProgress(
            registered = user != null,
            hasWorker = workers.isNotEmpty() || dash.workers.total > 0,
            hasShare = hasShare,
            hasPayoutAddress = !user?.walletAddress.isNullOrBlank(),
        )
    }
}
