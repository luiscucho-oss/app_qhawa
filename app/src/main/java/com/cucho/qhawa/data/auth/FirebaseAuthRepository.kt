package com.cucho.qhawa.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override val currentUserName: String?
        get() = firebaseAuth.currentUser?.displayName

    override val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    override suspend fun signIn(email: String, password: String): Result<Unit> = runAuth {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> = runAuth {
        val user = firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
        // La cuenta ya está creada y con sesión. Si falla guardar el nombre (por ejemplo,
        // se cortó internet justo ahora), no se avisa error: el usuario podrá ponerlo después.
        try {
            val profile = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user?.updateProfile(profile)?.await()
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: Exception) {
            // Se ignora a propósito (ver el comentario de arriba).
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runAuth {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Si el correo no tiene cuenta, se responde igual que si la tuviera,
            // para que nadie pueda averiguar qué correos están registrados.
            if (e.errorCode != "ERROR_USER_NOT_FOUND") throw e
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}

private suspend fun runAuth(block: suspend () -> Unit): Result<Unit> =
    try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AuthException(e.toAuthError(), e))
    }

// El orden importa: FirebaseAuthWeakPasswordException es un tipo de FirebaseAuthInvalidCredentialsException.
private fun Exception.toAuthError(): AuthError = when (this) {
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseAuthInvalidUserException ->
        if (errorCode == "ERROR_USER_DISABLED") AuthError.USER_DISABLED else AuthError.INVALID_CREDENTIALS
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.INVALID_CREDENTIALS
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    is FirebaseNetworkException -> AuthError.NETWORK
    else -> AuthError.UNKNOWN
}
