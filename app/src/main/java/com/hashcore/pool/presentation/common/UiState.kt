package com.hashcore.pool.presentation.common

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val isNetwork: Boolean = false) : UiState<Nothing>()
}
