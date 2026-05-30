package com.dasariravi145.agrolynch.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<S>(initialState: S) : ViewModel() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    protected fun launchSafe(
        context: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context + exceptionHandler) {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    protected open fun handleError(throwable: Throwable) {
        val message = throwable.message ?: "An unknown error occurred"
        Timber.e(throwable, "ViewModel Error: $message")
        FirebaseCrashlytics.getInstance().recordException(throwable)
        
        viewModelScope.launch {
            _error.emit(message)
        }
    }

    protected fun updateState(update: (S) -> S) {
        _uiState.update(update)
    }
}
