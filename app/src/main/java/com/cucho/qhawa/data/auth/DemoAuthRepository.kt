package com.cucho.qhawa.data.auth

/** Sesión local de demostración cuando Firebase no está configurado. */
class DemoAuthRepository : AuthRepository {

    private var signedIn = false
    private var name: String? = null
    private var email: String? = null

    override val isLoggedIn: Boolean get() = signedIn
    override val currentUserName: String? get() = name
    override val currentUserEmail: String? get() = email

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        this.email = email
        name = email.substringBefore('@').replace('.', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        signedIn = true
        return Result.success(Unit)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        this.name = name
        this.email = email
        signedIn = true
        return Result.success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)

    override fun signOut() {
        signedIn = false
        name = null
        email = null
    }
}
