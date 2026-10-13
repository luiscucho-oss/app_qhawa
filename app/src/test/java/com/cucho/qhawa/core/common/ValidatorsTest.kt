package com.cucho.qhawa.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun correosValidos() {
        assertTrue(isValidEmail("luis@correo.com"))
        assertTrue(isValidEmail("luis.cucho+qhawa@tecsup.edu.pe"))
    }

    @Test
    fun correosInvalidos() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("luis"))
        assertFalse(isValidEmail("luis@"))
        assertFalse(isValidEmail("luis@correo"))
        assertFalse(isValidEmail("luis @correo.com"))
        assertFalse(isValidEmail("@correo.com"))
    }
}
