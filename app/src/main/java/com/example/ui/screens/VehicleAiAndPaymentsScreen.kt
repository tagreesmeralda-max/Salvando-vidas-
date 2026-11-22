package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.UiState
import com.example.ui.theme.GuatemalaJadeContainer
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun VehicleAiAndPaymentsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val vehicles = (vehiclesState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val profile = (profileState as? UiState.Success)?.data

    var vehicleType by remember { mutableStateOf("CARRO") }
    var plate by remember { mutableStateOf("") }
    var brandModel by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var selectedGateway by remember { mutableStateOf("Visanet GT 3DS") }
    var paymentAmountText by remember { mutableStateOf("150") }
    var cardLast4 by remember { mutableStateOf("4242") }

    LaunchedEffect(profile?.minimumFeeGtq) {
        profile?.minimumFeeGtq?.let {
            paymentAmountText = it.toInt().toString()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) selectedImageUri = uri
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AI Vehicle Verification Section
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificación Vehicular con IA", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "Selecciona una foto real de tu carro o moto. Gemini Vision valida automáticamente la autenticidad y características del vehículo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = vehicleType == "CARRO",
                            onClick = { vehicleType = "CARRO" },
                            label = { Text("Carro") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = vehicleType == "MOTO",
                            onClick = { vehicleType = "MOTO" },
                            label = { Text("Moto") },
                            leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = plate,
                            onValueChange = { plate = it },
                            label = { Text("Placa (ej. P-123ABC)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vehicle_plate_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("Color") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = brandModel,
                        onValueChange = { brandModel = it },
                        label = { Text("Marca y modelo") },
                        placeholder = { Text("Ej. Toyota Corolla / Honda XR150") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vehicle_model_input"),
                        singleLine = true
                    )

                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto real del vehículo seleccionada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_vehicle_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedImageUri == null) "Elegir Foto" else "Cambiar Foto")
                        }

                        Button(
                            onClick = {
                                viewModel.verifyAndSaveVehicle(
                                    context = context,
                                    imageUri = selectedImageUri,
                                    vehicleType = vehicleType,
                                    plate = plate,
                                    brandModel = brandModel,
                                    color = color
                                )
                            },
                            enabled = selectedImageUri != null && aiState !is UiState.Loading && plate.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("verify_vehicle_ai_button")
                        ) {
                            if (aiState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Verificar con IA")
                            }
                        }
                    }

                    when (val state = aiState) {
                        is UiState.Success -> {
                            val r = state.data
                            Surface(
                                color = if (r.isRealVehicle) GuatemalaJadeContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (r.isRealVehicle) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (r.isRealVehicle) {
                                                "Vehículo Real Verificado (${r.confidencePercent}%)"
                                            } else {
                                                "Imagen No Validada como Vehículo (${r.confidencePercent}%)"
                                            },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Text("Detectado: ${r.detectedType} · ${r.detectedBrandModel} (${r.detectedColor})")
                                    Text("Placa visible: ${r.visiblePlate} · Estado: ${r.damageAssessment}")
                                    Text(r.verificationSummary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is UiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.message,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        else -> Unit
                    }

                    if (vehicles.isNotEmpty()) {
                        Text("Vehículos Registrados", style = MaterialTheme.typography.titleMedium)
                        vehicles.forEach { v ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${v.vehicleType} · ${v.plate}", fontWeight = FontWeight.Bold)
                                        Text("${v.brandModel} (${v.color})", style = MaterialTheme.typography.bodyMedium)
                                        Text(v.aiSummary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = if (v.aiVerified) GuatemalaJadeContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (v.aiVerified) "IA ${v.aiConfidence}%" else "Revisar",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Encrypted Payment Gateway Section (GTQ)
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pasarela de Pagos Cifrada (GTQ)", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "Transacciones tokenizadas bajo TLS 1.3 + AES-256-GCM. Nunca almacenamos números completos de tarjeta ni códigos CVV.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Visanet GT 3DS", "NeoNet TLS", "Bi-Pay Cifrado").forEach { gw ->
                            FilterChip(
                                selected = selectedGateway == gw,
                                onClick = { selectedGateway = gw },
                                label = { Text(gw) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = paymentAmountText,
                            onValueChange = { paymentAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Monto cobertura (GTQ)") },
                            prefix = { Text("Q ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cardLast4,
                            onValueChange = { cardLast4 = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Últimos 4 dígitos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val amt = (paymentAmountText.toDoubleOrNull() ?: 150.0).coerceIn(50.0, 50000.0)
                            viewModel.processEncryptedPayment(amt, selectedGateway, cardLast4)
                        },
                        enabled = !isSaving && cardLast4.length == 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("encrypted_payment_button")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Procesar Pago Tokenizado")
                    }

                    if (payments.isNotEmpty()) {
                        Text("Comprobantes Cifrados Recientes", style = MaterialTheme.typography.titleMedium)
                        payments.take(5).forEach { tx ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Q${String.format(Locale.US, "%.2f", tx.amountGtq)} · ${tx.gatewayName}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "•••• ${tx.maskedLast4} · ${tx.tokenReference}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = GuatemalaJadeContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = tx.status,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
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
