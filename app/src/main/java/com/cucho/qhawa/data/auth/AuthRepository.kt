package com.cucho.qhawa.data.auth

/**
 * Sesión del usuario. Las pantallas nunca hablan con Firebase directamente:
 * siempre pasan por aquí.
 */
interface AuthRepository {

    /** true si hay una sesión abierta. Firebase la recuerda aunque se cierre la app. */
    val isLoggedIn: Boolean

    /** Nombre del usuario con sesión abierta (el que escribió al registrarse), o null. */
    val currentUserName: String?

    /** Correo del usuario con sesión abierta, o null si no hay sesión. */
    val currentUserEmail: String?

    /** Inicia sesión. Si falla, el error es un [AuthException]. */
    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Crea la cuenta y deja la sesión iniciada. Si falla, el error es un [AuthException]. */
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>

    /** Envía el correo para restablecer la contraseña. Si falla, el error es un [AuthException]. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun signOut()
}

enum class AuthError {
    INVALID_CREDENTIALS,
    INVALID_EMAIL,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    USER_DISABLED,
    TOO_MANY_REQUESTS,
    NETWORK,
    NOT_CONFIGURED,
    UNKNOWN,
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(cause)
