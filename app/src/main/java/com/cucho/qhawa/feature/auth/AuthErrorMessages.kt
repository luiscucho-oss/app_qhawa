package com.cucho.qhawa.feature.auth

import androidx.annotation.StringRes
import com.cucho.qhawa.R
import com.cucho.qhawa.data.auth.AuthError
import com.cucho.qhawa.data.auth.AuthException

/** Convierte un error de [com.cucho.qhawa.data.auth.AuthRepository] en el texto que ve el usuario. */
@StringRes
internal fun Throwable.toAuthMessage(): Int = when ((this as? AuthException)?.error) {
    AuthError.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    AuthError.INVALID_EMAIL -> R.string.error_email_invalid
    AuthError.EMAIL_IN_USE -> R.string.error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.error_password_weak
    AuthError.USER_DISABLED -> R.string.error_user_disabled
    AuthError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
    AuthError.NETWORK -> R.string.error_network
    AuthError.NOT_CONFIGURED -> R.string.error_firebase_not_configured
    AuthError.UNKNOWN, null -> R.string.error_unknown
}
