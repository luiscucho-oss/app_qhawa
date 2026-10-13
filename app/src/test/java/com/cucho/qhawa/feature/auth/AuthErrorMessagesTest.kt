package com.cucho.qhawa.feature.auth

import com.cucho.qhawa.R
import com.cucho.qhawa.data.auth.AuthError
import com.cucho.qhawa.data.auth.AuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuthErrorMessagesTest {

    @Test
    fun todoErrorTieneUnMensaje() {
        AuthError.entries.forEach { error ->
            assertNotEquals("Falta mensaje para $error", 0, AuthException(error).toAuthMessage())
        }
    }

    @Test
    fun faltaDeFirebase_tieneSuPropioMensaje() {
        assertEquals(
            R.string.error_firebase_not_configured,
            AuthException(AuthError.NOT_CONFIGURED).toAuthMessage(),
        )
    }

    @Test
    fun errorDesconocido_usaElMensajeGeneral() {
        assertEquals(R.string.error_unknown, IllegalStateException("raro").toAuthMessage())
    }
}
