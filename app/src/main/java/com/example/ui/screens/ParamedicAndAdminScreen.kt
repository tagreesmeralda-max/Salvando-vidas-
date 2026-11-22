package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.EmergencyNotificationHelper
import com.example.core.TotpSecurity
import com.example.data.model.EmergencyIncident
import com.example.data.model.UiState
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GuatemalaJade
import com.example.ui.theme.GuatemalaJadeContainer
import com.example.ui.theme.RescueCoral
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OperationalMetrics
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ParamedicAndAdminScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Paramédicos, 1 = Admin 2FA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                    modifier = Modifier.testTag("tab_paramedics")
                ) {
                    Text("Paramédicos")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                    modifier = Modifier.testTag("tab_admin_2fa")
                ) {
                    Text("Admin (2FA)")
                }
            }

            if (selectedTab == 0) {
                ParamedicAccessSection(viewModel)
            } else {
                AdminDashboardSection(viewModel)
            }
        }
    }
}

@Composable
private fun ParamedicAccessSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lookupState by viewModel.paramedicLookupState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val offlineCard by viewModel.offlineCard.collectAsStateWithLifecycle()
    var queryInput by remember { mutableStateOf("") }

    val ownToken = (profileState as? UiState.Success)?.data?.qrToken
        ?: offlineCard?.qrToken
        ?: ""

    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_paramedic_card),
                contentDescription = "Acceso rápido para paramédicos",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )

            Text("Acceso Rápido de Paramédicos", style = MaterialTheme.typography.titleLarge)
            Text(
                "Ingresa el token de la tarjeta (ej. VQR_...) o pega la cadena escaneada de un QR offline (VQR1|...) para ver sangre, alergias y contacto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                label = { Text("Token QR o cadena VQR1|...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paramedic_lookup_input"),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.lookupParamedicCard(queryInput) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("paramedic_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Consultar Ficha")
                }
                if (ownToken.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            queryInput = ownToken
                            viewModel.lookupParamedicCard(ownToken)
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Mi Token")
                    }
                }
            }

            when (val state = lookupState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val card = state.data
                    if (card != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(card.fullName, style = MaterialTheme.typography.titleLarge)
                                    Surface(color = RescueCoral, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = card.bloodType,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text("Departamento: ${card.department}")
                                Text("Alergias: ${card.allergies.ifBlank { "Ninguna" }}")
                                Text("Condiciones: ${card.conditions.ifBlank { "Ninguna" }}")
                                Text("Medicamentos: ${card.medications.ifBlank { "Ninguno" }}")
                                Text("Vehículo: ${card.vehicleSummary}")
                                if (card.primaryContactPhone.isNotBlank() && card.primaryContactPhone != "N/D") {
                                    Button(
                                        onClick = {
                                            context.startActivity(
                                                EmergencyNotificationHelper.buildDialIntent(card.primaryContactPhone)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Llamar a ${card.primaryContactName} (${card.primaryContactPhone})")
                                    }
                                }
                            }
                        }
                    }
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun AdminDashboardSection(viewModel: MainViewModel) {
    val isUnlocked by viewModel.isAdmin2faUnlocked.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val adminIncidentsState by viewModel.adminIncidentsState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val webhookUrl by viewModel.webhookUrl.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val totpSecret = remember(profile?.totpSecret) {
        profile?.totpSecret?.ifBlank { "JBSWY3DPEHPK3PXP" } ?: "JBSWY3DPEHPK3PXP"
    }

    if (!viewModel.isEligibleAdmin) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(40.dp), tint = RescueCoral)
                Text("Acceso Administrativo Restringido", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Solo el administrador autorizado con cuenta verificada (${MainViewModel.ADMIN_EMAIL}) y doble factor 2FA puede consultar incidencias nacionales y reportes operativos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (!isUnlocked) {
        var codeInput by remember { mutableStateOf("") }
        var currentTotp by remember { mutableStateOf(TotpSecurity.currentTotpCode(totpSecret)) }
        var remainingSec by remember { mutableIntStateOf(TotpSecurity.secondsRemainingInWindow()) }

        LaunchedEffect(totpSecret) {
            while (true) {
                currentTotp = TotpSecurity.currentTotpCode(totpSecret)
                remainingSec = TotpSecurity.secondsRemainingInWindow()
                delay(1000L)
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Autenticación Doble Factor (2FA)", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Factor 1 verificado: ${viewModel.currentUserEmail}. Ingresa tu código TOTP de 6 dígitos para desbloquear el panel administrativo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Clave TOTP vinculada: $totpSecret",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Código vigente: $currentTotp",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${remainingSec}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Código 2FA (6 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_2fa_code_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { codeInput = currentTotp },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Autocompletar TOTP")
                    }
                    Button(
                        onClick = { viewModel.verifyAdmin2fa(totpSecret, codeInput) },
                        enabled = codeInput.length == 6,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_2fa_unlock_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desbloquear")
                    }
                }
            }
        }
        return
    }

    // Unlocked Admin Dashboard
    val incidents = (adminIncidentsState as? UiState.Success)?.data.orEmpty()
    val vehicles = (vehiclesState as? UiState.Success)?.data.orEmpty()
    val metrics = remember(incidents, vehicles) { viewModel.computeMetrics(incidents, vehicles) }

    var statusFilter by remember { mutableStateOf("TODAS") }
    var editingIncident by remember { mutableStateOf<EmergencyIncident?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = GuatemalaJadeContainer, shape = RoundedCornerShape(50)) {
            Text(
                text = "2FA Activo · Admin Verificado",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        TextButton(onClick = { viewModel.lockAdminPanel() }) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bloquear Panel")
        }
    }

    // Operational Performance Reports Card
    OperationalReportsCard(metrics = metrics)

    // Real-Time Cloud API & Webhook Sync Card
    Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Api, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("API Cloud en Tiempo Real", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Sincronización bidireccional Firestore activa. Opcionalmente configura un endpoint HTTPS de despacho externo (Retrofit TLS).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { viewModel.updateWebhookUrl(it) },
                label = { Text("Webhook HTTPS de despacho (opcional)") },
                placeholder = { Text("https://tu-api-despacho.gt/v1/incidents") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }

    // Incident Management List
    Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gestión Nacional de Incidencias", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("TODAS", "ACTIVA", "EN_RUTA", "ATENDIDA", "CERRADA").forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st) }
                    )
                }
            }

            when (adminIncidentsState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(
                    (adminIncidentsState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                is UiState.Success -> {
                    val filtered = if (statusFilter == "TODAS") incidents else incidents.filter { it.status == statusFilter }
                    if (filtered.isEmpty()) {
                        Text(
                            "No hay incidencias registradas con el filtro '$statusFilter'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filtered.forEach { inc ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${inc.userName} · ${inc.bloodType}", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${inc.status} (${inc.severity})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (inc.status == "ACTIVA") RescueCoral else EmeraldPrimary
                                        )
                                    }
                                    Text("${inc.department} · ${inc.vehicleType} · Unidad: ${inc.assignedUnit}")
                                    Text(
                                        text = String.format(Locale.US, "GPS: %.4f, %.4f · Q%.0f", inc.latitude, inc.longitude, inc.minimumFeeGtq),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    if (inc.notes.isNotBlank()) {
                                        Text(inc.notes, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(onClick = { editingIncident = inc }) {
                                            Text("Gestionar / Despachar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingIncident?.let { inc ->
        var newStatus by remember(inc.id) { mutableStateOf(inc.status) }
        var newSeverity by remember(inc.id) { mutableStateOf(inc.severity) }
        var newUnit by remember(inc.id) { mutableStateOf(inc.assignedUnit) }
        var newNotes by remember(inc.id) { mutableStateOf(inc.notes) }

        AlertDialog(
            onDismissRequest = { editingIncident = null },
            title = { Text("Despachar Incidencia · ${inc.department}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ACTIVA", "EN_RUTA", "ATENDIDA", "CERRADA").forEach { st ->
                            FilterChip(selected = newStatus == st, onClick = { newStatus = st }, label = { Text(st) })
                        }
                    }
                    OutlinedTextField(
                        value = newUnit,
                        onValueChange = { newUnit = it },
                        label = { Text("Unidad asignada (ej. Bomberos Cia. 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Bitácora operativa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateIncidentAdmin(inc.id, newStatus, newSeverity, newUnit, newNotes)
                        editingIncident = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIncident = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun OperationalReportsCard(metrics: OperationalMetrics) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reportes de Rendimiento Operativo", style = MaterialTheme.typography.titleLarge)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiBox("Total", "${metrics.totalIncidents}", EmeraldPrimary, Modifier.weight(1f))
                KpiBox("Activas", "${metrics.activeIncidents}", RescueCoral, Modifier.weight(1f))
                KpiBox("Atendidas", "${metrics.attendedOrClosedIncidents}", GuatemalaJade, Modifier.weight(1f))
                KpiBox("IA Veh.", "${metrics.aiVerifiedRatePercent}%", AmberAlert, Modifier.weight(1f))
            }

            Text(
                "Distribución Carro vs Moto y Sincronización Offline (${metrics.offlineSyncedIncidents} offline)",
                style = MaterialTheme.typography.labelLarge
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                val maxVal = maxOf(1, metrics.carroCount, metrics.motoCount, metrics.activeIncidents, metrics.attendedOrClosedIncidents).toFloat()
                val bars = listOf(
                    Triple("Carro", metrics.carroCount.toFloat(), EmeraldPrimary),
                    Triple("Moto", metrics.motoCount.toFloat(), AmberAlert),
                    Triple("Activas", metrics.activeIncidents.toFloat(), RescueCoral),
                    Triple("Cerradas", metrics.attendedOrClosedIncidents.toFloat(), GuatemalaJade)
                )
                val barHeight = 14.dp.toPx()
                val gap = 7.dp.toPx()
                bars.forEachIndexed { idx, (_, value, color) ->
                    val y = idx * (barHeight + gap)
                    drawRoundRect(
                        color = color.copy(alpha = 0.16f),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    val filledW = ((value / maxVal) * size.width).coerceAtLeast(if (value > 0) 12f else 0f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, y),
                        size = Size(filledW, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Text(
                text = "Carro: ${metrics.carroCount} · Moto: ${metrics.motoCount} · Cobertura promedio: Q${String.format(Locale.US, "%.2f", metrics.averageMinimumFeeGtq)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KpiBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
