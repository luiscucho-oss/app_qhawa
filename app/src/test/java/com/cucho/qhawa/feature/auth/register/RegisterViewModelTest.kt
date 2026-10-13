package com.cucho.qhawa.feature.auth.register

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

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = RegisterViewModel(repository)

    private fun fillValidForm() {
        viewModel.onNameChange("  Luis Cucho ")
        viewModel.onEmailChange(" luis@correo.com ")
        viewModel.onPasswordChange("secreto123")
        viewModel.onConfirmPasswordChange("secreto123")
    }

    @Test
    fun formularioVacio_muestraErrores_yNoCreaCuenta() {
        viewModel.onRegisterClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_name_empty, state.nameError)
        assertEquals(R.string.error_email_empty, state.emailError)
        assertEquals(R.string.error_password_empty, state.passwordError)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun contrasenaCorta_muestraError() {
        fillValidForm()
        viewModel.onPasswordChange("123")
        viewModel.onConfirmPasswordChange("123")
        viewModel.onRegisterClick()

        assertEquals(R.string.error_password_short, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun contrasenasDistintas_muestraError() {
        fillValidForm()
        viewModel.onConfirmPasswordChange("otra-cosa")
        viewModel.onRegisterClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_password_mismatch, state.confirmPasswordError)
        assertNull(state.passwordError)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun registroCorrecto_enviaDatosSinEspacios_yMarcaRegistrado() {
        fillValidForm()
        viewModel.onRegisterClick()

        val state = viewModel.uiState.value
        assertTrue(state.isRegistered)
        assertFalse(state.isLoading)
        assertEquals(1, repository.signUpCalls)
        assertEquals("Luis Cucho", repository.lastName)
        assertEquals("luis@correo.com", repository.lastEmail)
    }

    @Test
    fun correoYaRegistrado_muestraMensaje() {
        repository.result = Result.failure(AuthException(AuthError.EMAIL_IN_USE))
        fillValidForm()
        viewModel.onRegisterClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_email_in_use, state.errorMessage)
        assertFalse(state.isRegistered)
        assertFalse(state.isLoading)
    }

    @Test
    fun contrasenaDebilDeFirebase_muestraMensaje() {
        repository.result = Result.failure(AuthException(AuthError.WEAK_PASSWORD))
        fillValidForm()
        viewModel.onRegisterClick()

        assertEquals(R.string.error_password_weak, viewModel.uiState.value.errorMessage)
    }
}
