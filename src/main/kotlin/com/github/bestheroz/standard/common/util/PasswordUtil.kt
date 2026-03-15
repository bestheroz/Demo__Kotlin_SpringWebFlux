package com.github.bestheroz.standard.common.util

import org.springframework.security.crypto.bcrypt.BCrypt

object PasswordUtil {
    private const val MAX_PASSWORD_LENGTH = 72

    fun isPasswordValid(
        plainPassword: String,
        hashedPassword: String,
    ): Boolean = BCrypt.checkpw(plainPassword.take(MAX_PASSWORD_LENGTH), hashedPassword)

    fun getPasswordHash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
}
