package com.example.mypersonality.server.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordHasher {

    fun hash(password: String): String {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val digest = digest(password = password, salt = salt)
        return "${salt.encode()}:${digest.encode()}"
    }

    fun matches(password: String, storedValue: String): Boolean {
        val parts = storedValue.split(":", limit = 2)
        if (parts.size != 2) return false
        val salt = runCatching { Base64.getDecoder().decode(parts[0]) }.getOrNull() ?: return false
        val expected = parts[1]
        return digest(password = password, salt = salt).encode() == expected
    }

    private fun digest(password: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(salt + password.toByteArray())

    private fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)
}
