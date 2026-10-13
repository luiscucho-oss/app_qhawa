package com.cucho.qhawa.data.auth

/**
 * Se usa cuando falta google-services.json (el archivo de Firebase no se sube a GitHub).
 * La app abre igual, pero iniciar sesión o registrarse muestra un aviso en vez de cerrarse.
 */
class NotConfiguredAuthRepository : AuthRepository {

    override val isLoggedIn: Boolean = false
    override val currentUserName: String? = null
    override val currentUserEmail: String? = null

    override suspend fun signIn(email: String, password: String): Result<Unit> = notConfigured()

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> = notConfigured()

    override suspend fun sendPasswordReset(email: String): Result<Unit> = notConfigured()

    override fun signOut() = Unit

    private fun notConfigured(): Result<Unit> = Result.failure(AuthException(AuthError.NOT_CONFIGURED))
}
