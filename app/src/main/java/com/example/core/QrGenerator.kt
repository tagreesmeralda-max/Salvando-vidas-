package com.example.core

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class DecodedVidaQrPayload(
    val token: String,
    val fullName: String,
    val bloodType: String,
    val allergies: String,
    val conditions: String,
    val medications: String,
    val contactName: String,
    val contactPhone: String,
    val vehicleSummary: String,
    val department: String
)

object QrGenerator {
    fun buildOfflineQrPayload(
        token: String,
        fullName: String,
        bloodType: String,
        allergies: String,
        conditions: String,
        medications: String,
        contactName: String,
        contactPhone: String,
        vehicleSummary: String,
        department: String,
        shareMedicalInQr: Boolean
    ): String {
        fun clean(s: String) = s.replace("|", "/").replace("\n", " ").trim()
        return if (shareMedicalInQr) {
            listOf(
                "VQR1",
                clean(token),
                clean(fullName),
                clean(bloodType),
                clean(allergies.ifBlank { "Ninguna" }),
                clean(conditions.ifBlank { "Ninguna" }),
                clean(medications.ifBlank { "Ninguno" }),
                clean(contactName.ifBlank { "Sin contacto" }),
                clean(contactPhone.ifBlank { "N/D" }),
                clean(vehicleSummary.ifBlank { "Sin vehículo" }),
                clean(department)
            ).joinToString("|")
        } else {
            // Privacy-restricted QR exposes only lookup token & vital blood type + emergency contact
            listOf(
                "VQR1",
                clean(token),
                clean(fullName),
                clean(bloodType),
                "Privacidad protegida (ver con token)",
                "Privacidad protegida",
                "Privacidad protegida",
                clean(contactName.ifBlank { "Sin contacto" }),
                clean(contactPhone.ifBlank { "N/D" }),
                clean(vehicleSummary.ifBlank { "Sin vehículo" }),
                clean(department)
            ).joinToString("|")
        }
    }

    fun parseVidaQrPayload(raw: String): DecodedVidaQrPayload? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("VQR1|")) return null
        val parts = trimmed.split("|")
        if (parts.size < 11) return null
        return DecodedVidaQrPayload(
            token = parts[1],
            fullName = parts[2],
            bloodType = parts[3],
            allergies = parts[4],
            conditions = parts[5],
            medications = parts[6],
            contactName = parts[7],
            contactPhone = parts[8],
            vehicleSummary = parts[9],
            department = parts[10]
        )
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.parseColor("#0F172A") else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }
}
