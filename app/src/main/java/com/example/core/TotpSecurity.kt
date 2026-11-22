package com.example.core

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 TOTP (Time-Based One-Time Password) helper for 2FA Admin access
 * and TLS tokenized payment reference generation.
 */
object TotpSecurity {
    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val PERIOD_SECONDS = 30L
    private const val DIGITS = 6

    fun generateBase32Secret(length: Int = 16): String {
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) {
                append(BASE32_CHARS[random.nextInt(BASE32_CHARS.length)])
            }
        }
    }

    fun currentTotpCode(secretBase32: String, timeMillis: Long = System.currentTimeMillis()): String {
        val counter = (timeMillis / 1000L) / PERIOD_SECONDS
        return generateHotp(secretBase32, counter)
    }

    fun secondsRemainingInWindow(timeMillis: Long = System.currentTimeMillis()): Int {
        val elapsed = ((timeMillis / 1000L) % PERIOD_SECONDS).toInt()
        return (PERIOD_SECONDS.toInt() - elapsed).coerceIn(1, PERIOD_SECONDS.toInt())
    }

    fun verifyTotp(secretBase32: String, codeInput: String, timeMillis: Long = System.currentTimeMillis()): Boolean {
        val sanitized = codeInput.trim()
        if (sanitized.length != DIGITS || !sanitized.all { it.isDigit() }) return false
        val currentCounter = (timeMillis / 1000L) / PERIOD_SECONDS
        // Allow +-1 window (30s skew tolerance)
        return (-1L..1L).any { offset ->
            generateHotp(secretBase32, currentCounter + offset) == sanitized
        }
    }

    private fun generateHotp(secretBase32: String, counter: Long): String {
        val key = decodeBase32(secretBase32)
        if (key.isEmpty()) return "000000"
        val data = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash.last().toInt() and 0x0F
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 1_000_000
        return String.format(Locale.US, "%06d", otp)
    }

    private fun decodeBase32(input: String): ByteArray {
        val clean = input.uppercase(Locale.US).replace("=", "").filter { it in BASE32_CHARS }
        if (clean.isEmpty()) return ByteArray(0)
        val output = ArrayList<Byte>(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        for (ch in clean) {
            val value = BASE32_CHARS.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }

    fun generateEncryptedPaymentToken(gateway: String, last4: String): String {
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        val prefix = gateway.take(3).lowercase(Locale.US).filter { it.isLetterOrDigit() }.ifBlank { "gtq" }
        return "tok_${prefix}_${last4}_$hex"
    }
}
