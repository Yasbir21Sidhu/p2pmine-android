package com.hashcore.pool.presentation.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.repository.ObserverLink
import com.hashcore.pool.domain.repository.SecurityExtrasRepository
import com.hashcore.pool.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ObserverLinksViewModel @Inject constructor(
    private val security: SecurityExtrasRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow<UiState<List<ObserverLink>>>(UiState.Loading)
    val ui: StateFlow<UiState<List<ObserverLink>>> = _ui.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (_ui.value !is UiState.Success) _ui.value = UiState.Loading
            when (val r = security.listObserverLinks()) {
                is AppResult.Success -> _ui.value = UiState.Success(r.data)
                is AppResult.Error -> {
                    if (_ui.value !is UiState.Success) {
                        _ui.value = UiState.Error(r.message, r.isNetworkError)
                    } else {
                        _feedback.value = "Error: ${r.message}"
                    }
                }
            }
        }
    }

    fun create(label: String?) {
        viewModelScope.launch {
            _creating.value = true
            _feedback.value = null
            when (val r = security.createObserverLink(label)) {
                is AppResult.Success -> {
                    _feedback.value = "Link created"
                    load()
                }
                is AppResult.Error -> _feedback.value = "Error: ${r.message}"
            }
            _creating.value = false
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val r = security.deleteObserverLink(id)) {
                is AppResult.Success -> {
                    val cur = (_ui.value as? UiState.Success)?.data.orEmpty()
                    _ui.value = UiState.Success(cur.filter { it.id != id })
                    _feedback.value = "Link removed"
                }
                is AppResult.Error -> _feedback.value = "Error: ${r.message}"
            }
        }
    }
}
