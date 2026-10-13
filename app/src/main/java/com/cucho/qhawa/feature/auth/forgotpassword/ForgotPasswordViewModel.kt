package com.cucho.qhawa.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cucho.qhawa.QhawaApplication
import com.cucho.qhawa.R
import com.cucho.qhawa.core.common.isValidEmail
import com.cucho.qhawa.data.auth.AuthRepository
import com.cucho.qhawa.feature.auth.toAuthMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Los errores son ids de R.string, o null si no hay error. */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: Int? = null,
    val errorMessage: Int? = null,
    val isLoading: Boolean = false,
    val emailSent: Boolean = false,
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onSendClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.email.trim()
        val emailError = when {
            email.isEmpty() -> R.string.error_email_empty
            !isValidEmail(email) -> R.string.error_email_invalid
            else -> null
        }
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { current.copy(isLoading = false, emailSent = true) },
                    onFailure = { error -> current.copy(isLoading = false, errorMessage = error.toAuthMessage()) },
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as QhawaApplication
                ForgotPasswordViewModel(application.container.authRepository)
            }
        }
    }
}
