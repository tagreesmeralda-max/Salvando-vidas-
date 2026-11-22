package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.EmergencyNotificationHelper
import com.example.core.QrGenerator
import com.example.data.model.UiState
import com.example.ui.theme.AmberAlertContainer
import com.example.ui.theme.GuatemalaJadeContainer
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun HomeSosQrScreen(
    viewModel: MainViewModel,
    onNavigateToMedical: () -> Unit,
    onNavigateToRoutes: () -> Unit,
    onNavigateToVehiclePay: () -> Unit
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val offlineCard by viewModel.offlineCard.collectAsStateWithLifecycle()
    val pendingIncidents by viewModel.pendingIncidents.collectAsStateWithLifecycle()
    val gpsLocation by viewModel.gpsLocation.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var showSosConfirmDialog by remember { mutableStateOf(false) }
    var showFullscreenQr by remember { mutableStateOf(false) }
    var sosSeverity by remember { mutableStateOf("CRITICA") }
    var sosNotes by remember { mutableStateOf("") }

    val profile = (profileState as? UiState.Success)?.data
    val contacts = (contactsState as? UiState.Success)?.data.orEmpty()
    val vehicles = (vehiclesState as? UiState.Success)?.data.orEmpty()
    val primaryContact = contacts.firstOrNull { it.notifyAuto } ?: contacts.firstOrNull()
    val primaryVehicle = vehicles.firstOrNull()

    val effectiveName = profile?.fullName?.ifBlank { viewModel.initialDisplayName }
        ?: offlineCard?.fullName
        ?: viewModel.initialDisplayName.ifBlank { "Usuario VidaQR" }
    val effectiveBlood = profile?.bloodType ?: offlineCard?.bloodType ?: "O+"
    val effectiveAllergies = profile?.allergies ?: offlineCard?.allergies ?: "Sin alergias registradas"
    val effectiveDept = profile?.department ?: offlineCard?.department ?: gpsLocation.nearestDepartment
    val effectiveFee = profile?.minimumFeeGtq ?: offlineCard?.minimumFeeGtq ?: 150.0
    val effectiveToken = profile?.qrToken?.ifBlank { "VQR_${viewModel.currentUserId.take(6)}" }
        ?: offlineCard?.qrToken
        ?: "VQR_${viewModel.currentUserId.take(6)}"
    val effectiveVehicleSummary = if (primaryVehicle != null) {
        "${primaryVehicle.vehicleType} · ${primaryVehicle.plate} (${primaryVehicle.brandModel})"
    } else {
        offlineCard?.vehicleSummary ?: "Sin vehículo registrado"
    }

    val qrPayload = remember(
        effectiveToken,
        effectiveName,
        effectiveBlood,
        effectiveAllergies,
        profile?.conditions,
        profile?.medications,
        primaryContact,
        effectiveVehicleSummary,
        effectiveDept,
        profile?.shareMedicalInQr
    ) {
        QrGenerator.buildOfflineQrPayload(
            token = effectiveToken,
            fullName = effectiveName,
            bloodType = effectiveBlood,
            allergies = effectiveAllergies,
            conditions = profile?.conditions ?: offlineCard?.conditions ?: "",
            medications = profile?.medications ?: offlineCard?.medications ?: "",
            contactName = primaryContact?.name ?: offlineCard?.primaryContactName ?: "",
            contactPhone = primaryContact?.phone ?: offlineCard?.primaryContactPhone ?: "",
            vehicleSummary = effectiveVehicleSummary,
            department = effectiveDept,
            shareMedicalInQr = profile?.shareMedicalInQr ?: offlineCard?.shareMedicalInQr ?: true
        )
    }
    val qrBitmap = remember(qrPayload) { QrGenerator.generateQrBitmap(qrPayload, 512) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Connectivity & GPS Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isOnline) GuatemalaJadeContainer else AmberAlertContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) {
                                "En línea · Nube activa"
                            } else {
                                "Modo Offline · Cola local (${pendingIncidents.size})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { viewModel.refreshGpsLocation() },
                    modifier = Modifier.testTag("refresh_gps_button"),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Actualizar GPS",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(gpsLocation.nearestDepartment, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Onboarding Callout if Cloud Profile not created yet
            if (profileState is UiState.Success && profile == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Configura tu Ficha Médica",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Ingresa tu tipo de sangre, alergias y contactos para activar tu QR de vida.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToMedical,
                            modifier = Modifier.testTag("complete_profile_cta")
                        ) {
                            Text("Completar")
                        }
                    }
                }
            }

            // SOS Emergency Hero Button Card
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFB71C28), Color(0xFFE63946))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Asistencia de Accidente",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "GPS: %.4f, %.4f (±%.0fm)",
                                            gpsLocation.latitude,
                                            gpsLocation.longitude,
                                            gpsLocation.accuracyMeters
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showSosConfirmDialog = true },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("sos_emergency_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFB71C28)
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            } else {
                                Icon(Icons.Default.Warning, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVAR SOS Y NOTIFICAR CONTACTOS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(EmergencyNotificationHelper.buildDialIntent("122"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("call_bomberos_122"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bomberos 122")
                            }
                            OutlinedButton(
                                onClick = onNavigateToRoutes,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_routes_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Ruta Óptima GT")
                            }
                        }
                    }
                }
            }

            // QR Medical Life Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Mi Código QR Médico",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Token: $effectiveToken",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { showFullscreenQr = true }) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Ampliar código QR")
                            }
                            IconButton(onClick = onNavigateToMedical) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar ficha médica")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Código QR médico de emergencia",
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .padding(6.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = effectiveName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Sangre: $effectiveBlood",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Q${"%.0f".format(effectiveFee)} min",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Alergias: ${effectiveAllergies.ifBlank { "Ninguna" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                            Text(
                                text = "Vehículo: $effectiveVehicleSummary",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Contactos auto-alerta: ${contacts.count { it.notifyAuto }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToMedical,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Editar Historial")
                }
                FilledTonalButton(
                    onClick = onNavigateToVehiclePay,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Vehículo IA / Pago")
                }
            }
        }
    }

    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            title = { Text("Confirmar Alerta SOS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Se enviará tu ubicación GPS exacta, ficha médica y alerta automática a tus contactos seleccionados (${contacts.count { it.notifyAuto }})."
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CRITICA", "ALTA", "MEDIA").forEach { level ->
                            val selected = sosSeverity == level
                            if (selected) {
                                Button(onClick = { sosSeverity = level }) { Text(level) }
                            } else {
                                OutlinedButton(onClick = { sosSeverity = level }) { Text(level) }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = sosNotes,
                        onValueChange = { sosNotes = it },
                        label = { Text("Detalle breve (opcional)") },
                        placeholder = { Text("Ej. Colisión km 22 ruta al Pacífico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmDialog = false
                        viewModel.triggerSosEmergency(sosSeverity, sosNotes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("confirm_sos_dispatch")
                ) {
                    Text("Enviar Alerta Ahora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showFullscreenQr && qrBitmap != null) {
        AlertDialog(
            onDismissRequest = { showFullscreenQr = false },
            title = { Text("QR Médico · $effectiveName") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Código QR ampliado",
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    )
                    Text(
                        "Legible por paramédicos incluso sin internet (Modo Offline).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullscreenQr = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
