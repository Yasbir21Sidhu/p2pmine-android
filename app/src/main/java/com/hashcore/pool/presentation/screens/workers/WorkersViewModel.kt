package com.hashcore.pool.presentation.screens.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.model.PoolPublicSettings
import com.hashcore.pool.domain.model.User
import com.hashcore.pool.domain.model.Worker
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.LiveDataRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.domain.repository.WorkersRepository
import com.hashcore.pool.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkersScreenState(
    val workers: List<Worker> = emptyList(),
    val settings: PoolPublicSettings? = null,
    val user: User? = null,
    val creating: Boolean = false,
    val deletingId: String? = null,
    val actionError: String? = null,
    val actionMessage: String? = null,
)

sealed class WorkersEvent {
    data class Copied(val label: String) : WorkersEvent()
}

@HiltViewModel
class WorkersViewModel @Inject constructor(
    private val workersRepository: WorkersRepository,
    private val liveDataRepository: LiveDataRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow<UiState<WorkersScreenState>>(UiState.Loading)
    val ui: StateFlow<UiState<WorkersScreenState>> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<WorkersEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WorkersEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            liveDataRepository.updates.collect { update ->
                update.workers?.let { workers ->
                    val current = (_ui.value as? UiState.Success)?.data
                    _ui.value = UiState.Success(
                        (current ?: WorkersScreenState()).copy(workers = workers),
                    )
                }
            }
        }
        viewModelScope.launch {
            when (val s = settingsRepository.getPublicSettings()) {
                is AppResult.Success -> patchState { copy(settings = s.data) }
                is AppResult.Error -> Unit
            }
        }
        viewModelScope.launch {
            authRepository.sessionUser.collect { user ->
                val resolved = user ?: authRepository.getStoredUser()
                patchState { copy(user = resolved) }
            }
        }
    }

    fun onScreenVisible(visible: Boolean) {
        if (visible) liveDataRepository.start() else liveDataRepository.stop()
    }

    fun load() {
        viewModelScope.launch {
            if (_ui.value !is UiState.Success) _ui.value = UiState.Loading
            when (val result = workersRepository.getWorkers()) {
                is AppResult.Success -> {
                    val prev = (_ui.value as? UiState.Success)?.data ?: WorkersScreenState()
                    _ui.value = UiState.Success(
                        prev.copy(
                            workers = result.data,
                            user = prev.user ?: authRepository.getStoredUser(),
                        ),
                    )
                }
                is AppResult.Error -> {
                    if (_ui.value !is UiState.Success) {
                        _ui.value = UiState.Error(result.message, result.isNetworkError)
                    } else {
                        patchState { copy(actionError = result.message) }
                    }
                }
            }
        }
    }

    fun createWorker(name: String) {
        viewModelScope.launch {
            patchState { copy(creating = true, actionError = null, actionMessage = null) }
            when (val r = workersRepository.createWorker(name)) {
                is AppResult.Success -> {
                    patchState {
                        copy(creating = false, actionMessage = "Worker \"$name\" created")
                    }
                    load()
                }
                is AppResult.Error -> {
                    patchState { copy(creating = false, actionError = r.message) }
                }
            }
        }
    }

    /** Soft-disables on the pool (same as website "Disable"). Worker stays listed as Disabled. */
    fun disableWorker(id: String) {
        viewModelScope.launch {
            if (id.isBlank() || id == "unknown") {
                patchState {
                    copy(actionError = "Cannot disable: this worker has no server id yet. Pull to refresh.")
                }
                return@launch
            }
            patchState { copy(deletingId = id, actionError = null, actionMessage = null) }
            when (val r = workersRepository.deleteWorker(id)) {
                is AppResult.Success -> {
                    patchState {
                        copy(
                            deletingId = null,
                            actionMessage = "Worker disabled (still listed; re-create name if needed)",
                        )
                    }
                    // Always reload from server — do not pretend it was hard-deleted.
                    load()
                }
                is AppResult.Error -> {
                    patchState { copy(deletingId = null, actionError = r.message) }
                }
            }
        }
    }

    @Deprecated("Use disableWorker", ReplaceWith("disableWorker(id)"))
    fun deleteWorker(id: String) = disableWorker(id)

    fun clearActionFeedback() = patchState { copy(actionError = null, actionMessage = null) }

    fun notifyCopied(label: String) {
        _events.tryEmit(WorkersEvent.Copied(label))
    }

    /** Build stratum username: API field, else account.worker pattern. */
    fun stratumUsername(worker: Worker, state: WorkersScreenState): String {
        worker.stratumUser?.takeIf { it.isNotBlank() }?.let { return it }
        val account = state.user?.email
            ?.substringBefore("@")
            ?.takeIf { it.isNotBlank() }
            ?: state.user?.displayName?.takeIf { it.isNotBlank() }
            ?: state.user?.walletAddress?.take(12)
            ?: "account"
        return "$account.${worker.name}"
    }

    fun stratumHost(state: WorkersScreenState): String =
        state.settings?.stratumHost?.takeIf { it.isNotBlank() } ?: "p2pmine.com"

    fun stratumPorts(state: WorkersScreenState): List<String> {
        val ports = state.settings?.stratumPorts.orEmpty().filter { it.isNotBlank() }
        return ports.ifEmpty { listOf("3333", "2525", "9999") }
    }

    fun stratumUrl(state: WorkersScreenState, port: String = stratumPorts(state).first()): String =
        "stratum+tcp://${stratumHost(state)}:$port"

    fun fullConfigBlock(worker: Worker, state: WorkersScreenState): String {
        val ports = stratumPorts(state).joinToString(" / ")
        return buildString {
            appendLine("URL: ${stratumUrl(state)}")
            appendLine("Alt ports: $ports")
            appendLine("User: ${stratumUsername(worker, state)}")
            appendLine("Pass: x")
        }.trim()
    }

    private fun patchState(block: WorkersScreenState.() -> WorkersScreenState) {
        val current = (_ui.value as? UiState.Success)?.data ?: return
        _ui.value = UiState.Success(current.block())
    }
}
