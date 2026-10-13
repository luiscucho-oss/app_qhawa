package com.cucho.qhawa.feature.auth.forgotpassword

import com.cucho.qhawa.R
import com.cucho.qhawa.data.auth.AuthError
import com.cucho.qhawa.data.auth.AuthException
import com.cucho.qhawa.testing.FakeAuthRepository
import com.cucho.qhawa.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = ForgotPasswordViewModel(repository)

    @Test
    fun correoVacio_muestraError_yNoEnvia() {
        viewModel.onSendClick()

        assertEquals(R.string.error_email_empty, viewModel.uiState.value.emailError)
        assertEquals(0, repository.resetCalls)
    }

    @Test
    fun correoInvalido_muestraError() {
        viewModel.onEmailChange("luis@")
        viewModel.onSendClick()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
        assertEquals(0, repository.resetCalls)
    }

    @Test
    fun envioCorrecto_marcaCorreoEnviado() {
        viewModel.onEmailChange(" luis@correo.com ")
        viewModel.onSendClick()

        val state = viewModel.uiState.value
        assertTrue(state.emailSent)
        assertFalse(state.isLoading)
        assertEquals("luis@correo.com", repository.lastEmail)
    }

    @Test
    fun sinInternet_muestraMensaje_yNoMarcaEnviado() {
        repository.result = Result.failure(AuthException(AuthError.NETWORK))
        viewModel.onEmailChange("luis@correo.com")
        viewModel.onSendClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_network, state.errorMessage)
        assertFalse(state.emailSent)
    }
}
