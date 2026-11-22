package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val dpi: String = "",
    val bloodType: String = "O+",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val insuranceInfo: String = "",
    val organDonor: Boolean = false,
    val department: String = "Guatemala",
    val minimumFeeGtq: Double = 150.0,
    val shareMedicalInQr: Boolean = true,
    val privacyAccepted: Boolean = true,
    val qrToken: String = "",
    val totpSecret: String = "",
    val totpEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "fullName" to fullName.take(100),
        "email" to email.take(120),
        "phone" to phone.take(30),
        "dpi" to dpi.take(30),
        "bloodType" to bloodType,
        "allergies" to allergies.take(300),
        "conditions" to conditions.take(300),
        "medications" to medications.take(300),
        "insuranceInfo" to insuranceInfo.take(150),
        "organDonor" to organDonor,
        "department" to department.take(60),
        "minimumFeeGtq" to minimumFeeGtq.coerceIn(50.0, 10000.0),
        "shareMedicalInQr" to shareMedicalInQr,
        "privacyAccepted" to privacyAccepted,
        "qrToken" to qrToken.take(80),
        "totpSecret" to totpSecret.take(64),
        "totpEnabled" to totpEnabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "fullName" to fullName.take(100),
        "email" to email.take(120),
        "phone" to phone.take(30),
        "dpi" to dpi.take(30),
        "bloodType" to bloodType,
        "allergies" to allergies.take(300),
        "conditions" to conditions.take(300),
        "medications" to medications.take(300),
        "insuranceInfo" to insuranceInfo.take(150),
        "organDonor" to organDonor,
        "department" to department.take(60),
        "minimumFeeGtq" to minimumFeeGtq.coerceIn(50.0, 10000.0),
        "shareMedicalInQr" to shareMedicalInQr,
        "privacyAccepted" to privacyAccepted,
        "qrToken" to qrToken.take(80),
        "totpSecret" to totpSecret.take(64),
        "totpEnabled" to totpEnabled,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class EmergencyContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
    val notifyAuto: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VehicleRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vehicleType: String = "CARRO",
    val plate: String = "",
    val brandModel: String = "",
    val color: String = "",
    val aiVerified: Boolean = false,
    val aiConfidence: Long = 0L,
    val aiDetectedType: String = "",
    val aiSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amountGtq: Double = 150.0,
    val gatewayName: String = "Visanet GT 3DS",
    val maskedLast4: String = "0000",
    val tokenReference: String = "",
    val encryptionProtocol: String = "TLS 1.3 + AES-256-GCM",
    val status: String = "APROBADO",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EmergencyIncident(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val bloodType: String = "O+",
    val allergies: String = "",
    val vehicleType: String = "CARRO",
    val department: String = "Guatemala",
    val latitude: Double = 14.6349,
    val longitude: Double = -90.5069,
    val accuracyMeters: Double = 10.0,
    val status: String = "ACTIVA",
    val severity: String = "ALTA",
    val minimumFeeGtq: Double = 150.0,
    val notifiedContactsCount: Long = 0L,
    val offlineQueued: Boolean = false,
    val notes: String = "",
    val assignedUnit: String = "Pendiente",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ParamedicCard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val cardToken: String = "",
    val fullName: String = "",
    val bloodType: String = "O+",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val primaryContactName: String = "",
    val primaryContactPhone: String = "",
    val vehicleSummary: String = "",
    val department: String = "Guatemala",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class VehicleAiVerificationResult(
    val isRealVehicle: Boolean,
    val detectedType: String,
    val confidencePercent: Int,
    val detectedBrandModel: String,
    val detectedColor: String,
    val visiblePlate: String,
    val damageAssessment: String,
    val verificationSummary: String
)
