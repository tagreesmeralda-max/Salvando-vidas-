package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.OperationType
import com.example.core.QrGenerator
import com.example.core.TotpSecurity
import com.example.core.handleFirestoreError
import com.example.data.local.OfflineMedicalCardEntity
import com.example.data.local.PendingIncidentEntity
import com.example.data.local.VidaQrDao
import com.example.data.model.EmergencyContact
import com.example.data.model.EmergencyIncident
import com.example.data.model.ParamedicCard
import com.example.data.model.PaymentTransaction
import com.example.data.model.UserProfile
import com.example.data.model.VehicleRecord
import com.example.data.remote.CloudSyncApiService
import com.example.data.remote.IncidentWebhookPayload
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VidaQrRepository(
    private val db: FirebaseFirestore,
    private val dao: VidaQrDao,
    private val webhookApi: CloudSyncApiService,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, dao: VidaQrDao) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao = dao,
        webhookApi = CloudSyncApiService.create()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google antes de operar en la nube.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeContacts(userId: String): Flow<List<EmergencyContact>> = flow {
        val path = "users/$userId/contacts"
        emitAll(
            db.collection("users").document(userId).collection("contacts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EmergencyContact::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeVehicles(userId: String): Flow<List<VehicleRecord>> = flow {
        val path = "users/$userId/vehicles"
        emitAll(
            db.collection("users").document(userId).collection("vehicles")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VehicleRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePayments(userId: String): Flow<List<PaymentTransaction>> = flow {
        val path = "users/$userId/payments"
        emitAll(
            db.collection("users").document(userId).collection("payments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeUserIncidents(userId: String): Flow<List<EmergencyIncident>> = flow {
        val path = "incidents"
        emitAll(
            db.collection("incidents")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EmergencyIncident::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllIncidentsForAdmin(userId: String): Flow<List<EmergencyIncident>> = flow {
        val path = "incidents"
        emitAll(
            db.collection("incidents")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EmergencyIncident::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeOfflineCard(): Flow<OfflineMedicalCardEntity?> = dao.observeOfflineCard()

    fun observePendingIncidents(): Flow<List<PendingIncidentEntity>> = dao.observePendingIncidents()

    suspend fun cacheOfflineProfile(
        profile: UserProfile,
        contacts: List<EmergencyContact>,
        vehicles: List<VehicleRecord>
    ) {
        val primaryContact = contacts.firstOrNull { it.notifyAuto } ?: contacts.firstOrNull()
        val primaryVehicle = vehicles.firstOrNull()
        val vehicleSummary = if (primaryVehicle != null) {
            "${primaryVehicle.vehicleType} · ${primaryVehicle.brandModel} (${primaryVehicle.plate})"
        } else {
            "Sin vehículo registrado"
        }
        val payload = QrGenerator.buildOfflineQrPayload(
            token = profile.qrToken.ifBlank { "VQR_${profile.userId.take(8)}" },
            fullName = profile.fullName,
            bloodType = profile.bloodType,
            allergies = profile.allergies,
            conditions = profile.conditions,
            medications = profile.medications,
            contactName = primaryContact?.name ?: "",
            contactPhone = primaryContact?.phone ?: "",
            vehicleSummary = vehicleSummary,
            department = profile.department,
            shareMedicalInQr = profile.shareMedicalInQr
        )
        dao.upsertOfflineCard(
            OfflineMedicalCardEntity(
                userId = profile.userId,
                fullName = profile.fullName,
                bloodType = profile.bloodType,
                allergies = profile.allergies,
                conditions = profile.conditions,
                medications = profile.medications,
                insuranceInfo = profile.insuranceInfo,
                primaryContactName = primaryContact?.name ?: "Sin contacto",
                primaryContactPhone = primaryContact?.phone ?: "",
                allContactPhonesCsv = contacts.filter { it.notifyAuto }.joinToString(",") { it.phone },
                vehicleType = primaryVehicle?.vehicleType ?: "CARRO",
                vehicleSummary = vehicleSummary,
                department = profile.department,
                minimumFeeGtq = profile.minimumFeeGtq,
                qrToken = profile.qrToken,
                qrPayload = payload,
                shareMedicalInQr = profile.shareMedicalInQr
            )
        )
    }

    suspend fun saveUserProfile(
        profile: UserProfile,
        contacts: List<EmergencyContact>,
        vehicles: List<VehicleRecord>
    ): Result<UserProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada."))
        val path = "users/$uid"
        return try {
            require(profile.fullName.trim().isNotEmpty()) { "El nombre completo es obligatorio." }
            val resolvedToken = profile.qrToken.ifBlank {
                "VQR_${uid.take(6)}_${UUID.randomUUID().toString().take(4).uppercase()}"
            }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val resolvedSecret = profile.totpSecret.ifBlank { TotpSecurity.generateBase32Secret() }

            val normalized = profile.copy(
                userId = uid,
                qrToken = resolvedToken,
                totpSecret = resolvedSecret,
                minimumFeeGtq = profile.minimumFeeGtq.coerceIn(50.0, 10000.0)
            )
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(normalized.toUpdateMap()).await()
            } else {
                docRef.set(normalized.toCreateMap()).await()
            }

            if (normalized.shareMedicalInQr) {
                syncParamedicCard(normalized, contacts, vehicles)
            }
            cacheOfflineProfile(normalized, contacts, vehicles)
            Result.success(normalized)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun syncParamedicCard(
        profile: UserProfile,
        contacts: List<EmergencyContact>,
        vehicles: List<VehicleRecord>
    ) {
        val token = profile.qrToken
        if (token.isBlank()) return
        val primaryContact = contacts.firstOrNull { it.notifyAuto } ?: contacts.firstOrNull()
        val primaryVehicle = vehicles.firstOrNull()
        val vehicleSummary = if (primaryVehicle != null) {
            "${primaryVehicle.vehicleType} · ${primaryVehicle.plate} (${primaryVehicle.brandModel})"
        } else {
            "Sin vehículo"
        }
        val cardRef = db.collection("paramedic_cards").document(token)
        val existing = cardRef.get().await()
        if (existing.exists()) {
            cardRef.update(
                mapOf(
                    "fullName" to profile.fullName.take(100),
                    "bloodType" to profile.bloodType,
                    "allergies" to profile.allergies.take(300),
                    "conditions" to profile.conditions.take(300),
                    "medications" to profile.medications.take(300),
                    "primaryContactName" to (primaryContact?.name ?: "No registrado").take(80),
                    "primaryContactPhone" to (primaryContact?.phone ?: "N/D").take(30),
                    "vehicleSummary" to vehicleSummary.take(120),
                    "department" to profile.department.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            cardRef.set(
                mapOf(
                    "userId" to profile.userId,
                    "cardToken" to token,
                    "fullName" to profile.fullName.take(100),
                    "bloodType" to profile.bloodType,
                    "allergies" to profile.allergies.take(300),
                    "conditions" to profile.conditions.take(300),
                    "medications" to profile.medications.take(300),
                    "primaryContactName" to (primaryContact?.name ?: "No registrado").take(80),
                    "primaryContactPhone" to (primaryContact?.phone ?: "N/D").take(30),
                    "vehicleSummary" to vehicleSummary.take(120),
                    "department" to profile.department.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun addContact(name: String, phone: String, relationship: String, notifyAuto: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No autenticado"))
        val path = "users/$uid/contacts"
        return try {
            require(name.trim().isNotEmpty()) { "Nombre del contacto requerido" }
            require(phone.trim().length >= 6) { "Teléfono válido requerido" }
            val docRef = db.collection("users").document(uid).collection("contacts").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to name.trim().take(80),
                    "phone" to phone.trim().take(30),
                    "relationship" to relationship.trim().ifBlank { "Familiar" }.take(50),
                    "notifyAuto" to notifyAuto,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No autenticado"))
        val path = "users/$uid/contacts/$contactId"
        return try {
            require(contactId.isNotBlank())
            db.collection("users").document(uid).collection("contacts").document(contactId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveVehicle(vehicle: VehicleRecord): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No autenticado"))
        val path = "users/$uid/vehicles"
        return try {
            require(vehicle.plate.trim().isNotEmpty()) { "Placa requerida" }
            require(vehicle.brandModel.trim().isNotEmpty()) { "Marca y modelo requeridos" }
            val docRef = db.collection("users").document(uid).collection("vehicles").document()
            val payload = mapOf(
                "userId" to uid,
                "vehicleType" to if (vehicle.vehicleType == "MOTO") "MOTO" else "CARRO",
                "plate" to vehicle.plate.trim().uppercase().take(20),
                "brandModel" to vehicle.brandModel.trim().take(80),
                "color" to vehicle.color.trim().ifBlank { "No especificado" }.take(40),
                "aiVerified" to vehicle.aiVerified,
                "aiConfidence" to vehicle.aiConfidence.coerceIn(0L, 100L),
                "aiDetectedType" to vehicle.aiDetectedType.take(30),
                "aiSummary" to vehicle.aiSummary.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordEncryptedPayment(
        amountGtq: Double,
        gatewayName: String,
        last4: String
    ): Result<PaymentTransaction> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No autenticado"))
        val path = "users/$uid/payments"
        return try {
            val validLast4 = last4.filter { it.isDigit() }.takeLast(4).padStart(4, '0')
            val tokenRef = TotpSecurity.generateEncryptedPaymentToken(gatewayName, validLast4)
            val safeAmount = amountGtq.coerceIn(50.0, 50000.0)
            val docRef = db.collection("users").document(uid).collection("payments").document()
            val payload = mapOf(
                "userId" to uid,
                "amountGtq" to safeAmount,
                "gatewayName" to gatewayName.take(60),
                "maskedLast4" to validLast4,
                "tokenReference" to tokenRef,
                "encryptionProtocol" to "TLS 1.3 + AES-256-GCM",
                "status" to "APROBADO",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                PaymentTransaction(
                    id = docRef.id,
                    userId = uid,
                    amountGtq = safeAmount,
                    gatewayName = gatewayName,
                    maskedLast4 = validLast4,
                    tokenReference = tokenRef,
                    status = "APROBADO"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun triggerSosIncident(
        profile: UserProfile,
        vehicleType: String,
        department: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        severity: String,
        notifiedContactsCount: Int,
        notes: String,
        isOnline: Boolean,
        webhookUrl: String? = null
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No autenticado"))
        if (!isOnline) {
            dao.insertPendingIncident(
                PendingIncidentEntity(
                    userId = uid,
                    userName = profile.fullName.ifBlank { "Usuario VidaQR" },
                    bloodType = profile.bloodType,
                    allergies = profile.allergies,
                    vehicleType = if (vehicleType == "MOTO") "MOTO" else "CARRO",
                    department = department,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    severity = severity,
                    minimumFeeGtq = profile.minimumFeeGtq,
                    notifiedContactsCount = notifiedContactsCount,
                    notes = notes
                )
            )
            return Result.success(true) // Queued offline
        }

        val path = "incidents"
        return try {
            val docRef = db.collection("incidents").document()
            val payload = mapOf(
                "userId" to uid,
                "userName" to profile.fullName.ifBlank { "Usuario VidaQR" }.take(100),
                "bloodType" to profile.bloodType,
                "allergies" to profile.allergies.take(300),
                "vehicleType" to if (vehicleType == "MOTO") "MOTO" else "CARRO",
                "department" to department.take(60),
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "accuracyMeters" to accuracyMeters.coerceIn(0.0, 50000.0),
                "status" to "ACTIVA",
                "severity" to if (severity in listOf("CRITICA", "ALTA", "MEDIA")) severity else "ALTA",
                "minimumFeeGtq" to profile.minimumFeeGtq.coerceIn(50.0, 10000.0),
                "notifiedContactsCount" to notifiedContactsCount.coerceIn(0, 20),
                "offlineQueued" to false,
                "notes" to notes.take(500),
                "assignedUnit" to "Pendiente",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            if (!webhookUrl.isNullOrBlank() && webhookUrl.startsWith("https://")) {
                try {
                    webhookApi.pushIncidentWebhook(
                        fullHttpsUrl = webhookUrl.trim(),
                        payload = IncidentWebhookPayload(
                            incidentId = docRef.id,
                            userId = uid,
                            bloodType = profile.bloodType,
                            vehicleType = vehicleType,
                            department = department,
                            latitude = latitude,
                            longitude = longitude,
                            status = "ACTIVA",
                            severity = severity,
                            minimumFeeGtq = profile.minimumFeeGtq,
                            timestampUtcMillis = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.w("VidaQrRepo", "Optional webhook sync failed: ${e.message}")
                }
            }
            Result.success(false) // Synced live to cloud
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            // Fallback to offline Room queue so emergency is never lost
            dao.insertPendingIncident(
                PendingIncidentEntity(
                    userId = uid,
                    userName = profile.fullName.ifBlank { "Usuario VidaQR" },
                    bloodType = profile.bloodType,
                    allergies = profile.allergies,
                    vehicleType = if (vehicleType == "MOTO") "MOTO" else "CARRO",
                    department = department,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    severity = severity,
                    minimumFeeGtq = profile.minimumFeeGtq,
                    notifiedContactsCount = notifiedContactsCount,
                    notes = notes
                )
            )
            Result.success(true)
        }
    }

    suspend fun syncPendingOfflineIncidents(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.success(0)
        val pending = dao.getPendingIncidentsOnce().filter { it.userId == uid }
        if (pending.isEmpty()) return Result.success(0)
        var syncedCount = 0
        for (item in pending) {
            try {
                val docRef = db.collection("incidents").document()
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "userName" to item.userName.take(100),
                        "bloodType" to item.bloodType,
                        "allergies" to item.allergies.take(300),
                        "vehicleType" to item.vehicleType,
                        "department" to item.department.take(60),
                        "latitude" to item.latitude,
                        "longitude" to item.longitude,
                        "accuracyMeters" to item.accuracyMeters,
                        "status" to "ACTIVA",
                        "severity" to item.severity,
                        "minimumFeeGtq" to item.minimumFeeGtq,
                        "notifiedContactsCount" to item.notifiedContactsCount,
                        "offlineQueued" to true,
                        "notes" to "${item.notes} [Sincronizado tras modo offline]".take(500),
                        "assignedUnit" to "Pendiente",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                dao.deletePendingIncident(item.localId)
                syncedCount++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "incidents")
                return Result.failure(e)
            }
        }
        return Result.success(syncedCount)
    }

    suspend fun updateIncidentAdmin(
        incidentId: String,
        status: String,
        severity: String,
        assignedUnit: String,
        notes: String
    ): Result<Unit> {
        val path = "incidents/$incidentId"
        return try {
            requireUserId()
            require(incidentId.isNotBlank())
            db.collection("incidents").document(incidentId).update(
                mapOf(
                    "status" to status,
                    "severity" to severity,
                    "assignedUnit" to assignedUnit.take(100),
                    "notes" to notes.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun lookupParamedicCard(cardToken: String): Result<ParamedicCard?> {
        val cleanToken = cardToken.trim()
        val path = "paramedic_cards/$cleanToken"
        return try {
            requireUserId()
            require(cleanToken.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                "El código de tarjeta contiene caracteres inválidos."
            }
            val doc = db.collection("paramedic_cards").document(cleanToken).get().await()
            val card = doc.toObject(
                ParamedicCard::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(card)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
