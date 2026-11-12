package com.hashcore.pool.presentation.screens.workerdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.Worker
import com.hashcore.pool.domain.model.WorkerStatus
import com.hashcore.pool.domain.repository.WorkersRepository
import com.hashcore.pool.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workersRepository: WorkersRepository,
) : ViewModel() {

    private val workerId: String = checkNotNull(savedStateHandle["workerId"])

    private val _ui = MutableStateFlow<UiState<Worker>>(UiState.Loading)
    val ui: StateFlow<UiState<Worker>> = _ui.asStateFlow()

    private val _chartNote = MutableStateFlow<String?>(null)
    val chartNote: StateFlow<String?> = _chartNote.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _ui.value = UiState.Loading
            when (val result = workersRepository.getWorker(workerId)) {
                is AppResult.Success -> {
                    _ui.value = UiState.Success(result.data)
                    val w = result.data
                    _chartNote.value = when {
                        w.hashrateHistory.isNotEmpty() && w.historyIsEstimated ->
                            "Approximate trend from live averages — pool sample series is empty for this worker."
                        w.hashrateHistory.isNotEmpty() -> null
                        w.status == WorkerStatus.DISABLED || !w.isActive ->
                            "This worker is disabled. Re-create it or point a miner with this name again."
                        w.currentHashrateHs <= 0 && w.avg24hHashrateHs <= 0 ->
                            "No hashrate yet for this worker. Point your ASIC using the stratum username on the Workers tab."
                        else ->
                            "No hashrate samples yet for this worker. Chart appears after the pool stores history points."
                    }
                }
                is AppResult.Error -> {
                    _ui.value = UiState.Error(result.message, result.isNetworkError)
                    _chartNote.value = null
                }
            }
        }
    }
}
