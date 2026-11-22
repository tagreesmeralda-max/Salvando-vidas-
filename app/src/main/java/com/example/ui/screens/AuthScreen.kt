package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.QrGenerator
import com.example.data.remote.GoogleSignInButton
import com.example.ui.viewmodel.OfflineEmergencyViewModel

@Composable
fun AuthScreen(
    offlineViewModel: OfflineEmergencyViewModel,
    onAuthSuccess: () -> Unit
) {
    var authError by remember { mutableStateOf<String?>(null) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    val offlineCard by offlineViewModel.offlineCard.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Banner Card
                    Card(
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_rescue),
                                contentDescription = "Ilustración de respuesta vial y médica en Guatemala",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xCC06282E)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(18.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "GUATEMALA · EMERGENCIAS 24/7",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "VidaQR",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tu ficha médica QR, alertas automáticas y rutas óptimas en todo el país.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }
                    }

                    // Feature highlights
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureRow(
                                icon = Icons.Default.QrCode2,
                                title = "Código QR de Emergencia Offline",
                                desc = "Acceso inmediato a tipo de sangre, alergias y contactos sin conexión."
                            )
                            FeatureRow(
                                icon = Icons.Default.HealthAndSafety,
                                title = "Historial Médico y Privacidad Estricta",
                                desc = "Control granular de qué datos médicos pueden ver los paramédicos."
                            )
                            FeatureRow(
                                icon = Icons.Default.Route,
                                title = "Rutas Carro y Moto + Verificación IA",
                                desc = "Cobertura en los 22 departamentos y verificación real de tu vehículo."
                            )
                        }
                    }

                    if (authError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError!!,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    GoogleSignInButton(
                        onAuthSuccess = {
                            authError = null
                            onAuthSuccess()
                        },
                        onAuthError = { msg -> authError = msg }
                    )

                    OutlinedButton(
                        onClick = { showOfflineDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("offline_emergency_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modo Emergencia Offline (QR Local)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Datos cifrados y protegidos por aislamiento de cuenta",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showOfflineDialog) {
        val card = offlineCard
        val qrBitmap: Bitmap? = remember(card?.qrPayload) {
            card?.qrPayload?.let { QrGenerator.generateQrBitmap(it, 440) }
        }
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = { Text("Tarjeta Médica Offline") },
            text = {
                if (card == null) {
                    Text(
                        "Aún no hay una tarjeta médica guardada localmente en este dispositivo. " +
                            "Inicia sesión con Google una vez y guarda tus datos para habilitar el acceso offline permanente."
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Código QR médico offline",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Text(
                            text = card.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sangre: ${card.bloodType} · ${card.department}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Alergias: ${card.allergies.ifBlank { "Ninguna" }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Contacto: ${card.primaryContactName} (${card.primaryContactPhone})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Vehículo: ${card.vehicleSummary}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOfflineDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
