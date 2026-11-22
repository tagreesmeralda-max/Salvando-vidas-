package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.routing.GuatemalaRoutingEngine
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalProfileScreen(viewModel: MainViewModel) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val existingProfile = (profileState as? UiState.Success)?.data
    val contacts = (contactsState as? UiState.Success)?.data.orEmpty()

    var fullName by remember { mutableStateOf(viewModel.initialDisplayName) }
    var phone by remember { mutableStateOf("") }
    var dpi by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("O+") }
    var allergies by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var insuranceInfo by remember { mutableStateOf("") }
    var organDonor by remember { mutableStateOf(false) }
    var department by remember { mutableStateOf("Guatemala") }
    var minimumFeeText by remember { mutableStateOf("150") }
    var shareMedicalInQr by remember { mutableStateOf(true) }
    var privacyAccepted by remember { mutableStateOf(true) }
    var deptDropdownExpanded by remember { mutableStateOf(false) }

    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var newContactRel by remember { mutableStateOf("Familiar") }
    var newContactNotifyAuto by remember { mutableStateOf(true) }

    LaunchedEffect(existingProfile) {
        if (existingProfile != null) {
            fullName = existingProfile.fullName
            phone = existingProfile.phone
            dpi = existingProfile.dpi
            bloodType = existingProfile.bloodType
            allergies = existingProfile.allergies
            conditions = existingProfile.conditions
            medications = existingProfile.medications
            insuranceInfo = existingProfile.insuranceInfo
            organDonor = existingProfile.organDonor
            department = existingProfile.department
            minimumFeeText = existingProfile.minimumFeeGtq.toInt().toString()
            shareMedicalInQr = existingProfile.shareMedicalInQr
            privacyAccepted = existingProfile.privacyAccepted
        }
    }

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
            // Privacy Compliance Shield Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Protección y Privacidad de Datos de Salud",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tu expediente médico está protegido con reglas de aislamiento por cuenta. Solo la tarjeta vital autorizada es accesible mediante tu token QR único.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Personal & Medical Form
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Historial Médico y Datos Iniciales", style = MaterialTheme.typography.titleLarge)
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre completo *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono GT") },
                            placeholder = { Text("+50255551234") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dpi,
                            onValueChange = { dpi = it },
                            label = { Text("DPI / CUI") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Text("Tipo de sangre", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-").forEach { bt ->
                            FilterChip(
                                selected = bloodType == bt,
                                onClick = { bloodType = bt },
                                label = { Text(bt) }
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = deptDropdownExpanded,
                        onExpandedChange = { deptDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Departamento (Guatemala)") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = deptDropdownExpanded,
                            onDismissRequest = { deptDropdownExpanded = false }
                        ) {
                            GuatemalaRoutingEngine.departments.forEach { node ->
                                DropdownMenuItem(
                                    text = { Text("${node.name} (${node.region})") },
                                    onClick = {
                                        department = node.name
                                        deptDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Alergias conocidas") },
                        placeholder = { Text("Ej. Penicilina, látex, ninguna") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = conditions,
                        onValueChange = { conditions = it },
                        label = { Text("Condiciones médicas") },
                        placeholder = { Text("Ej. Diabetes, hipertensión, asma") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = medications,
                        onValueChange = { medications = it },
                        label = { Text("Medicamentos actuales") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = insuranceInfo,
                        onValueChange = { insuranceInfo = it },
                        label = { Text("Seguro médico / IGSS") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minimumFeeText,
                        onValueChange = { minimumFeeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Costo mínimo de asistencia editable (GTQ)") },
                        prefix = { Text("Q ") },
                        supportingText = { Text("Rango permitido: Q50.00 - Q10,000.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("minimum_fee_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Donante de órganos", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = organDonor, onCheckedChange = { organDonor = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mostrar alergias y meds en QR", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Si se desactiva, el QR solo expone sangre y contacto.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = shareMedicalInQr, onCheckedChange = { shareMedicalInQr = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Consentimiento de privacidad de salud", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Autorizo uso exclusivo para paramédicos ante emergencia.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = privacyAccepted, onCheckedChange = { privacyAccepted = it })
                    }

                    Button(
                        onClick = {
                            val parsedFee = (minimumFeeText.toDoubleOrNull() ?: 150.0).coerceIn(50.0, 10000.0)
                            val updated = (existingProfile ?: UserProfile(
                                userId = viewModel.currentUserId,
                                email = viewModel.currentUserEmail
                            )).copy(
                                fullName = fullName.trim().ifBlank { viewModel.initialDisplayName.ifBlank { "Usuario VidaQR" } },
                                email = viewModel.currentUserEmail,
                                phone = phone.trim(),
                                dpi = dpi.trim(),
                                bloodType = bloodType,
                                allergies = allergies.trim(),
                                conditions = conditions.trim(),
                                medications = medications.trim(),
                                insuranceInfo = insuranceInfo.trim(),
                                organDonor = organDonor,
                                department = department,
                                minimumFeeGtq = parsedFee,
                                shareMedicalInQr = shareMedicalInQr,
                                privacyAccepted = privacyAccepted
                            )
                            viewModel.saveProfile(updated)
                        },
                        enabled = !isSaving && privacyAccepted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Historial y Actualizar QR")
                    }
                }
            }

            // Emergency Contacts Section
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Contactos de Emergencia Automáticos", style = MaterialTheme.typography.titleLarge)

                    contacts.forEach { contact ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${contact.name} (${contact.relationship})", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${contact.phone} · ${if (contact.notifyAuto) "Notificación automática activa" else "Solo consulta"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteEmergencyContact(contact.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar contacto")
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Nombre del contacto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_input"),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            label = { Text("Teléfono") },
                            placeholder = { Text("+50244445555") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("contact_phone_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newContactRel,
                            onValueChange = { newContactRel = it },
                            label = { Text("Parentesco") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notificar automáticamente en SOS", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = newContactNotifyAuto, onCheckedChange = { newContactNotifyAuto = it })
                    }
                    Button(
                        onClick = {
                            if (newContactName.isNotBlank() && newContactPhone.trim().length >= 6) {
                                viewModel.addEmergencyContact(
                                    newContactName,
                                    newContactPhone,
                                    newContactRel,
                                    newContactNotifyAuto
                                )
                                newContactName = ""
                                newContactPhone = ""
                            }
                        },
                        enabled = newContactName.isNotBlank() && newContactPhone.trim().length >= 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_contact_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agregar Contacto de Emergencia")
                    }
                }
            }
        }
    }
}
