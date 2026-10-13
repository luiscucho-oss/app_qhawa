package com.cucho.qhawa.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotConfiguredAuthRepositoryTest {

    private val repository = NotConfiguredAuthRepository()

    @Test
    fun sinFirebase_nuncaHaySesion() {
        assertFalse(repository.isLoggedIn)
    }

    @Test
    fun sinFirebase_cadaOperacionFallaConAvisoDeConfiguracion() = runTest {
        val results = listOf(
            repository.signIn("luis@correo.com", "123456"),
            repository.signUp("Luis", "luis@correo.com", "123456"),
            repository.sendPasswordReset("luis@correo.com"),
        )

        results.forEach { result ->
            assertTrue(result.isFailure)
            val error = result.exceptionOrNull() as AuthException
            assertEquals(AuthError.NOT_CONFIGURED, error.error)
        }
    }
}
