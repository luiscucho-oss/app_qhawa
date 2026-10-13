package com.cucho.qhawa.feature.auth.login

import com.cucho.qhawa.R
import com.cucho.qhawa.data.auth.AuthError
import com.cucho.qhawa.data.auth.AuthException
import com.cucho.qhawa.testing.FakeAuthRepository
import com.cucho.qhawa.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = LoginViewModel(repository)

    @Test
    fun camposVacios_muestraErrores_yNoLlamaAFirebase() {
        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_email_empty, state.emailError)
        assertEquals(R.string.error_password_empty, state.passwordError)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun correoInvalido_muestraError() {
        viewModel.onEmailChange("luis@")
        viewModel.onPasswordChange("123456")
        viewModel.onLoginClick()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun loginCorrecto_marcaSesionIniciada() {
        viewModel.onEmailChange("  luis@correo.com ")
        viewModel.onPasswordChange("123456")
        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals("luis@correo.com", repository.lastEmail)
    }

    @Test
    fun credencialesIncorrectas_muestraMensaje() {
        repository.result = Result.failure(AuthException(AuthError.INVALID_CREDENTIALS))
        viewModel.onEmailChange("luis@correo.com")
        viewModel.onPasswordChange("incorrecta")
        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_invalid_credentials, state.errorMessage)
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
    }

    @Test
    fun sinInternet_muestraMensajeDeRed() {
        repository.result = Result.failure(AuthException(AuthError.NETWORK))
        viewModel.onEmailChange("luis@correo.com")
        viewModel.onPasswordChange("123456")
        viewModel.onLoginClick()

        assertEquals(R.string.error_network, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun alEscribir_seQuitaElError() {
        viewModel.onLoginClick()
        viewModel.onEmailChange("l")

        assertNull(viewModel.uiState.value.emailError)
    }
}
