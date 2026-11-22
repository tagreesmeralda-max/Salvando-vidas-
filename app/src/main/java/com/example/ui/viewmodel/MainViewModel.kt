package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.LocationHelper
import com.example.core.NetworkMonitor
import com.example.core.PreciseLocationResult
import com.example.core.QrGenerator
import com.example.core.TotpSecurity
import com.example.data.local.OfflineMedicalCardEntity
import com.example.data.local.PendingIncidentEntity
import com.example.data.model.EmergencyContact
import com.example.data.model.EmergencyIncident
import com.example.data.model.ParamedicCard
import com.example.data.model.PaymentTransaction
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VehicleAiVerificationResult
import com.example.data.model.VehicleRecord
import com.example.data.repository.VehicleAiRepository
import com.example.data.repository.VidaQrRepository
import com.example.data.routing.GuatemalaRoutingEngine
import com.example.data.routing.OptimalRouteResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OperationalMetrics(
    val totalIncidents: Int,
    val activeIncidents: Int,
    val attendedOrClosedIncidents: Int,
    val offlineSyncedIncidents: Int,
    val motoCount: Int,
    val carroCount: Int,
    val aiVerifiedRatePercent: Int,
    val averageMinimumFeeGtq: Double,
    val departmentBreakdown: Map<String, Int>
)

data class SosDispatchEvent(
    val offlineQueued: Boolean,
    val latitude: Double,
    val longitude: Double,
    val department: String,
    val notifiedPhones: List<String>,
    val userName: String,
    val bloodType: String,
    val vehicleSummary: String
)

class MainViewModel(
    private val repository: VidaQrRepository,
    private val aiRepository: VehicleAiRepository,
    private val locationHelper: LocationHelper,
    private val networkMonitor: NetworkMonitor,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    val initialDisplayName: String
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val isEligibleAdmin: Boolean =
        isEmailVerified && currentUserEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiState("No se pudo cargar el perfil médico")

    val contactsState: StateFlow<UiState<List<EmergencyContact>>> =
        repository.observeContacts(currentUserId)
            .toUiState("No se pudieron cargar los contactos de emergencia")

    val vehiclesState: StateFlow<UiState<List<VehicleRecord>>> =
        repository.observeVehicles(currentUserId)
            .toUiState("No se pudieron cargar los vehículos")

    val paymentsState: StateFlow<UiState<List<PaymentTransaction>>> =
        repository.observePayments(currentUserId)
            .toUiState("No se pudieron cargar los pagos")

    val userIncidentsState: StateFlow<UiState<List<EmergencyIncident>>> =
        repository.observeUserIncidents(currentUserId)
            .toUiState("No se pudieron cargar las incidencias")

    val offlineCard: StateFlow<OfflineMedicalCardEntity?> =
        repository.observeOfflineCard()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val pendingIncidents: StateFlow<List<PendingIncidentEntity>> =
        repository.observePendingIncidents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _isAdmin2faUnlocked = MutableStateFlow(false)
    val isAdmin2faUnlocked: StateFlow<Boolean> = _isAdmin2faUnlocked.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminIncidentsState: StateFlow<UiState<List<EmergencyIncident>>> =
        _isAdmin2faUnlocked.flatMapLatest { unlocked ->
            if (unlocked && isEligibleAdmin) {
                repository.observeAllIncidentsForAdmin(currentUserId)
                    .toUiStateFlow("No se pudieron cargar las incidencias operativas")
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _gpsLocation = MutableStateFlow(
        PreciseLocationResult(14.6349, -90.5069, 35.0, "Guatemala", false)
    )
    val gpsLocation: StateFlow<PreciseLocationResult> = _gpsLocation.asStateFlow()

    private val _selectedRouteVehicle = MutableStateFlow("CARRO")
    val selectedRouteVehicle: StateFlow<String> = _selectedRouteVehicle.asStateFlow()

    private val _routeOrigin = MutableStateFlow("Guatemala")
    val routeOrigin: StateFlow<String> = _routeOrigin.asStateFlow()

    private val _routeDestination = MutableStateFlow("Sacatepéquez")
    val routeDestination: StateFlow<String> = _routeDestination.asStateFlow()

    val optimalRoute: StateFlow<OptimalRouteResult> = combine(
        _routeOrigin,
        _routeDestination,
        _selectedRouteVehicle
    ) { origin, dest, vehicle ->
        GuatemalaRoutingEngine.calculateOptimalRoute(origin, dest, vehicle)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        GuatemalaRoutingEngine.calculateOptimalRoute("Guatemala", "Sacatepéquez", "CARRO")
    )

    private val _aiState = MutableStateFlow<UiState<VehicleAiVerificationResult>?>(null)
    val aiState: StateFlow<UiState<VehicleAiVerificationResult>?> = _aiState.asStateFlow()

    private val _paramedicLookupState = MutableStateFlow<UiState<ParamedicCard?>?>(null)
    val paramedicLookupState: StateFlow<UiState<ParamedicCard?>?> = _paramedicLookupState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _lastSosEvent = MutableStateFlow<SosDispatchEvent?>(null)
    val lastSosEvent: StateFlow<SosDispatchEvent?> = _lastSosEvent.asStateFlow()

    private val _webhookUrl = MutableStateFlow("")
    val webhookUrl: StateFlow<String> = _webhookUrl.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        refreshGpsLocation()
        // Auto-sync offline Room cache whenever cloud profile/contacts/vehicles update
        viewModelScope.launch {
            combine(profileState, contactsState, vehiclesState) { p, c, v ->
                Triple(p, c, v)
            }.collect { (pState, cState, vState) ->
                val profile = (pState as? UiState.Success)?.data
                val contacts = (cState as? UiState.Success)?.data.orEmpty()
                val vehicles = (vState as? UiState.Success)?.data.orEmpty()
                if (profile != null) {
                    repository.cacheOfflineProfile(profile, contacts, vehicles)
                }
            }
        }
        // Auto-upload queued offline incidents when connectivity returns
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    val result = repository.syncPendingOfflineIncidents()
                    val synced = result.getOrNull() ?: 0
                    if (synced > 0) {
                        _statusBanner.value = "Conexión restablecida: $synced incidencia(s) offline sincronizada(s) en la nube."
                    }
                }
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun clearLastSosEvent() {
        _lastSosEvent.value = null
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            val fallbackDept = (profileState.value as? UiState.Success)?.data?.department ?: "Guatemala"
            val loc = locationHelper.getPreciseLocation(fallbackDept)
            _gpsLocation.value = loc
            _routeOrigin.value = loc.nearestDepartment
        }
    }

    fun updateRouteSelection(origin: String, destination: String, vehicleType: String) {
        _routeOrigin.value = origin
        _routeDestination.value = destination
        _selectedRouteVehicle.value = vehicleType
    }

    fun updateWebhookUrl(url: String) {
        _webhookUrl.value = url
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _isSaving.value = true
            val contacts = (contactsState.value as? UiState.Success)?.data.orEmpty()
            val vehicles = (vehiclesState.value as? UiState.Success)?.data.orEmpty()
            val result = repository.saveUserProfile(profile, contacts, vehicles)
            _isSaving.value = false
            result.onSuccess {
                _statusBanner.value = "Perfil médico y código QR actualizados y cifrados."
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Error al guardar el perfil."
            }
        }
    }

    fun addEmergencyContact(name: String, phone: String, relationship: String, notifyAuto: Boolean) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addContact(name, phone, relationship, notifyAuto)
            _isSaving.value = false
            result.onSuccess {
                _statusBanner.value = "Contacto de emergencia agregado."
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "No se pudo agregar el contacto."
            }
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId).onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Error al eliminar contacto."
            }
        }
    }

    fun verifyAndSaveVehicle(
        context: Context,
        imageUri: Uri?,
        vehicleType: String,
        plate: String,
        brandModel: String,
        color: String
    ) {
        viewModelScope.launch {
            if (imageUri == null) {
                _aiState.value = UiState.Error("Selecciona una foto real del vehículo para verificar con IA.")
                return@launch
            }
            _aiState.value = UiState.Loading
            val aiResult = aiRepository.verifyVehicleImage(context, imageUri, vehicleType, plate)
            aiResult.onSuccess { verification ->
                _aiState.value = UiState.Success(verification)
                val matchesDeclared = verification.isRealVehicle &&
                    (verification.detectedType == vehicleType || verification.confidencePercent >= 70)
                val record = VehicleRecord(
                    userId = currentUserId,
                    vehicleType = vehicleType,
                    plate = plate.ifBlank { verification.visiblePlate },
                    brandModel = brandModel.ifBlank { verification.detectedBrandModel },
                    color = color.ifBlank { verification.detectedColor },
                    aiVerified = matchesDeclared,
                    aiConfidence = verification.confidencePercent.toLong(),
                    aiDetectedType = verification.detectedType,
                    aiSummary = verification.verificationSummary
                )
                repository.saveVehicle(record).onSuccess {
                    _statusBanner.value = if (matchesDeclared) {
                        "Vehículo verificado por IA (${verification.confidencePercent}%) y guardado."
                    } else {
                        "Imagen analizada por IA: revisa la coincidencia de tipo (${verification.detectedType})."
                    }
                }.onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Error al guardar el vehículo."
                }
            }.onFailure { e ->
                _aiState.value = UiState.Error(e.localizedMessage ?: "Falló la verificación con IA.")
            }
        }
    }

    fun processEncryptedPayment(amountGtq: Double, gatewayName: String, cardLast4: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.recordEncryptedPayment(amountGtq, gatewayName, cardLast4)
            _isSaving.value = false
            result.onSuccess { tx ->
                _statusBanner.value = "Pago seguro Q${"%.2f".format(tx.amountGtq)} aprobado (${tx.tokenReference})."
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Error en la pasarela cifrada."
            }
        }
    }

    fun triggerSosEmergency(severity: String, notes: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val profile = (profileState.value as? UiState.Success)?.data
                ?: UserProfile(
                    userId = currentUserId,
                    fullName = initialDisplayName.ifBlank { "Usuario VidaQR" },
                    email = currentUserEmail,
                    bloodType = offlineCard.value?.bloodType ?: "O+",
                    allergies = offlineCard.value?.allergies ?: "",
                    department = offlineCard.value?.department ?: "Guatemala",
                    minimumFeeGtq = offlineCard.value?.minimumFeeGtq ?: 150.0,
                    qrToken = offlineCard.value?.qrToken ?: "VQR_${currentUserId.take(6)}"
                )
            val contacts = (contactsState.value as? UiState.Success)?.data.orEmpty()
            val vehicles = (vehiclesState.value as? UiState.Success)?.data.orEmpty()
            val primaryVehicle = vehicles.firstOrNull()
            val vehicleType = primaryVehicle?.vehicleType ?: offlineCard.value?.vehicleType ?: "CARRO"
            val vehicleSummary = if (primaryVehicle != null) {
                "${primaryVehicle.vehicleType} ${primaryVehicle.plate}"
            } else {
                offlineCard.value?.vehicleSummary ?: "Vehículo VidaQR"
            }

            val loc = locationHelper.getPreciseLocation(profile.department)
            _gpsLocation.value = loc

            val phones = contacts.filter { it.notifyAuto }.map { it.phone }.ifEmpty {
                offlineCard.value?.allContactPhonesCsv
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
            }

            val res = repository.triggerSosIncident(
                profile = profile,
                vehicleType = vehicleType,
                department = loc.nearestDepartment,
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = loc.accuracyMeters,
                severity = severity,
                notifiedContactsCount = phones.size,
                notes = notes.ifBlank { "Alerta SOS activada desde VidaQR" },
                isOnline = isOnline.value,
                webhookUrl = _webhookUrl.value.takeIf { it.isNotBlank() }
            )
            _isSaving.value = false
            res.onSuccess { queuedOffline ->
                _lastSosEvent.value = SosDispatchEvent(
                    offlineQueued = queuedOffline,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    department = loc.nearestDepartment,
                    notifiedPhones = phones,
                    userName = profile.fullName.ifBlank { initialDisplayName },
                    bloodType = profile.bloodType,
                    vehicleSummary = vehicleSummary
                )
                _statusBanner.value = if (queuedOffline) {
                    "Modo Offline: SOS guardado localmente y listo para notificar por SMS."
                } else {
                    "SOS enviado en tiempo real a la nube (${loc.nearestDepartment})."
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Error al activar SOS."
            }
        }
    }

    fun lookupParamedicCard(input: String) {
        viewModelScope.launch {
            val trimmed = input.trim()
            if (trimmed.isBlank()) {
                _paramedicLookupState.value = UiState.Error("Ingresa un token VQR o pega el contenido escaneado del QR.")
                return@launch
            }
            // Check offline QR payload format VQR1|... first (works 100% offline!)
            val offlineDecoded = QrGenerator.parseVidaQrPayload(trimmed)
            if (offlineDecoded != null) {
                _paramedicLookupState.value = UiState.Success(
                    ParamedicCard(
                        cardToken = offlineDecoded.token,
                        fullName = offlineDecoded.fullName,
                        bloodType = offlineDecoded.bloodType,
                        allergies = offlineDecoded.allergies,
                        conditions = offlineDecoded.conditions,
                        medications = offlineDecoded.medications,
                        primaryContactName = offlineDecoded.contactName,
                        primaryContactPhone = offlineDecoded.contactPhone,
                        vehicleSummary = offlineDecoded.vehicleSummary,
                        department = offlineDecoded.department
                    )
                )
                return@launch
            }

            _paramedicLookupState.value = UiState.Loading
            repository.lookupParamedicCard(trimmed).onSuccess { card ->
                if (card != null) {
                    _paramedicLookupState.value = UiState.Success(card)
                } else {
                    _paramedicLookupState.value = UiState.Error("No se encontró tarjeta paramédica para '$trimmed'.")
                }
            }.onFailure { e ->
                _paramedicLookupState.value = UiState.Error(e.localizedMessage ?: "Error al consultar base paramédica.")
            }
        }
    }

    fun verifyAdmin2fa(secret: String, code: String): Boolean {
        if (!isEligibleAdmin) {
            _statusBanner.value = "Acceso restringido: solo la cuenta administradora verificada puede desbloquear el panel."
            return false
        }
        val ok = TotpSecurity.verifyTotp(secret, code)
        if (ok) {
            _isAdmin2faUnlocked.value = true
            _statusBanner.value = "Autenticación de doble factor (2FA) verificada."
        } else {
            _statusBanner.value = "Código 2FA inválido o expirado. Verifica los 6 dígitos."
        }
        return ok
    }

    fun lockAdminPanel() {
        _isAdmin2faUnlocked.value = false
    }

    fun updateIncidentAdmin(incidentId: String, status: String, severity: String, assignedUnit: String, notes: String) {
        viewModelScope.launch {
            repository.updateIncidentAdmin(incidentId, status, severity, assignedUnit, notes)
                .onSuccess {
                    _statusBanner.value = "Incidencia actualizada ($status)."
                }
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Error al actualizar incidencia."
                }
        }
    }

    fun computeMetrics(incidents: List<EmergencyIncident>, vehicles: List<VehicleRecord>): OperationalMetrics {
        val total = incidents.size
        val active = incidents.count { it.status == "ACTIVA" || it.status == "EN_RUTA" }
        val closed = incidents.count { it.status == "ATENDIDA" || it.status == "CERRADA" }
        val offlineSynced = incidents.count { it.offlineQueued }
        val moto = incidents.count { it.vehicleType == "MOTO" }
        val carro = incidents.count { it.vehicleType == "CARRO" }
        val verifiedCount = vehicles.count { it.aiVerified }
        val aiRate = if (vehicles.isEmpty()) 100 else ((verifiedCount * 100.0) / vehicles.size).toInt()
        val avgFee = if (incidents.isEmpty()) 150.0 else incidents.map { it.minimumFeeGtq }.average()
        val deptMap = incidents.groupingBy { it.department }.eachCount()
        return OperationalMetrics(
            totalIncidents = total,
            activeIncidents = active,
            attendedOrClosedIncidents = closed,
            offlineSyncedIncidents = offlineSynced,
            motoCount = moto,
            carroCount = carro,
            aiVerifiedRatePercent = aiRate,
            averageMinimumFeeGtq = avgFee,
            departmentBreakdown = deptMap
        )
    }

    private fun <T> Flow<T>.toUiState(fallbackError: String): StateFlow<UiState<T>> =
        toUiStateFlow(fallbackError)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private fun <T> Flow<T>.toUiStateFlow(fallbackError: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error: ${rootError.message}", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }

    companion object {
        private const val TAG = "MainViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        const val ADMIN_EMAIL = "tagreesmeralda@gmail.com"
    }
}
