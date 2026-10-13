package com.cucho.qhawa.core.common

/** Firebase no acepta contraseñas de menos de 6 caracteres. */
const val MIN_PASSWORD_LENGTH = 6

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/** Revisa que el texto tenga forma de correo (nombre@dominio.com). */
fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)
