package com.cucho.qhawa.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cucho.qhawa.QhawaApplication
import com.cucho.qhawa.R
import com.cucho.qhawa.core.common.MIN_PASSWORD_LENGTH
import com.cucho.qhawa.core.common.isValidEmail
import com.cucho.qhawa.data.auth.AuthRepository
import com.cucho.qhawa.feature.auth.toAuthMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Los errores son ids de R.string, o null si no hay error. */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val confirmPasswordError: Int? = null,
    val errorMessage: Int? = null,
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, errorMessage = null) }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (state.isLoading) return

        val name = state.name.trim()
        val email = state.email.trim()

        val nameError = if (name.isEmpty()) R.string.error_name_empty else null
        val emailError = when {
            email.isEmpty() -> R.string.error_email_empty
            !isValidEmail(email) -> R.string.error_email_invalid
            else -> null
        }
        val passwordError = when {
            state.password.isEmpty() -> R.string.error_password_empty
            state.password.length < MIN_PASSWORD_LENGTH -> R.string.error_password_short
            else -> null
        }
        val confirmPasswordError =
            if (state.confirmPassword != state.password) R.string.error_password_mismatch else null

        if (nameError != null || emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signUp(name, email, state.password)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { current.copy(isLoading = false, isRegistered = true) },
                    onFailure = { error -> current.copy(isLoading = false, errorMessage = error.toAuthMessage()) },
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as QhawaApplication
                RegisterViewModel(application.container.authRepository)
            }
        }
    }
}
