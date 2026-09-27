package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurityHelper {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256 // bits
    private const val SALT_LENGTH = 16 // bytes
    private const val PREFIX = "PBKDF2"

    fun hashPin(pin: String, salt: ByteArray = generateSalt()): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        return "$PREFIX:$ITERATIONS:$saltHex:$hashHex"
    }

    fun verifyPin(pin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) return false

        // Check if stored in PBKDF2 format
        if (storedHash.startsWith("$PREFIX:")) {
            val parts = storedHash.split(":")
            if (parts.size == 4) {
                val iterations = parts[1].toIntOrNull() ?: ITERATIONS
                val saltHex = parts[2]
                val expectedHashHex = parts[3]
                val salt = hexToBytes(saltHex)
                val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val computedHash = factory.generateSecret(spec).encoded
                val computedHashHex = computedHash.joinToString("") { "%02x".format(it) }
                return MessageDigest.isEqual(computedHashHex.toByteArray(), expectedHashHex.toByteArray())
            }
        }

        // Backward compatibility fallback for legacy SHA-256 hash ("FINPULSE_SALT_$pin")
        val legacyMd = MessageDigest.getInstance("SHA-256")
        val legacyBytes = legacyMd.digest("FINPULSE_SALT_$pin".toByteArray())
        val legacyHash = legacyBytes.joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(legacyHash.toByteArray(), storedHash.toByteArray())
    }

    fun isLegacyHash(storedHash: String?): Boolean {
        return storedHash != null && !storedHash.startsWith("$PREFIX:")
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
