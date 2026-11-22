package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_medical_cards")
data class OfflineMedicalCardEntity(
    @PrimaryKey val id: Int = 1,
    val userId: String,
    val fullName: String,
    val bloodType: String,
    val allergies: String,
    val conditions: String,
    val medications: String,
    val insuranceInfo: String,
    val primaryContactName: String,
    val primaryContactPhone: String,
    val allContactPhonesCsv: String,
    val vehicleType: String,
    val vehicleSummary: String,
    val department: String,
    val minimumFeeGtq: Double,
    val qrToken: String,
    val qrPayload: String,
    val shareMedicalInQr: Boolean,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_incidents")
data class PendingIncidentEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val userId: String,
    val userName: String,
    val bloodType: String,
    val allergies: String,
    val vehicleType: String,
    val department: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val severity: String,
    val minimumFeeGtq: Double,
    val notifiedContactsCount: Int,
    val notes: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
