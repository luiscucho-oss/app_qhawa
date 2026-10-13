package com.cucho.qhawa.feature.auth.login

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
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val errorMessage: Int? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.email.trim()
        val emailError = when {
            email.isEmpty() -> R.string.error_email_empty
            !isValidEmail(email) -> R.string.error_email_invalid
            else -> null
        }
        val passwordError = if (state.password.isEmpty()) R.string.error_password_empty else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, state.password)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { current.copy(isLoading = false, isLoggedIn = true) },
                    onFailure = { error -> current.copy(isLoading = false, errorMessage = error.toAuthMessage()) },
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as QhawaApplication
                LoginViewModel(application.container.authRepository)
            }
        }
    }
}
