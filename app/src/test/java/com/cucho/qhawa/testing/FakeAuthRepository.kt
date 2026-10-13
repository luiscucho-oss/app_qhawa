package com.cucho.qhawa.testing

import com.cucho.qhawa.data.auth.AuthRepository

/** AuthRepository falso para las pruebas: no usa Firebase y guarda lo que le piden. */
class FakeAuthRepository : AuthRepository {

    /** Resultado que devolverán signIn, signUp y sendPasswordReset. Se cambia en cada prueba. */
    var result: Result<Unit> = Result.success(Unit)

    var signInCalls = 0
    var signUpCalls = 0
    var resetCalls = 0
    var lastName: String? = null
    var lastEmail: String? = null

    override val isLoggedIn: Boolean = false
    override val currentUserName: String? = null
    override val currentUserEmail: String? = null

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        signInCalls++
        lastEmail = email
        return result
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        signUpCalls++
        lastName = name
        lastEmail = email
        return result
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        resetCalls++
        lastEmail = email
        return result
    }

    override fun signOut() = Unit
}
