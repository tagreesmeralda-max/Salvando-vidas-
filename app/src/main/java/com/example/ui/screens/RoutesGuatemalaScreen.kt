package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.EmergencyNotificationHelper
import com.example.data.routing.GuatemalaNode
import com.example.data.routing.GuatemalaRoutingEngine
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GuatemalaJade
import com.example.ui.theme.RescueCoral
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesGuatemalaScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val origin by viewModel.routeOrigin.collectAsStateWithLifecycle()
    val destination by viewModel.routeDestination.collectAsStateWithLifecycle()
    val vehicleType by viewModel.selectedRouteVehicle.collectAsStateWithLifecycle()
    val route by viewModel.optimalRoute.collectAsStateWithLifecycle()
    val gpsLocation by viewModel.gpsLocation.collectAsStateWithLifecycle()

    var originExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }

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
            // Vehicle Mode & Department Selection
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Rutas Óptimas Nacionales (22 Deptos)", style = MaterialTheme.typography.titleLarge)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = vehicleType == "CARRO",
                            onClick = { viewModel.updateRouteSelection(origin, destination, "CARRO") },
                            label = { Text("Carro / Ambulancia") },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("route_car_chip")
                        )
                        FilterChip(
                            selected = vehicleType == "MOTO",
                            onClick = { viewModel.updateRouteSelection(origin, destination, "MOTO") },
                            label = { Text("Moto / Respuesta Ágil") },
                            leadingIcon = {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("route_moto_chip")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = originExpanded,
                            onExpandedChange = { originExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = origin,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Origen") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = originExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = originExpanded,
                                onDismissRequest = { originExpanded = false }
                            ) {
                                GuatemalaRoutingEngine.departments.forEach { node ->
                                    DropdownMenuItem(
                                        text = { Text(node.name) },
                                        onClick = {
                                            viewModel.updateRouteSelection(node.name, destination, vehicleType)
                                            originExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = destExpanded,
                            onExpandedChange = { destExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = destination,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Destino") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = destExpanded,
                                onDismissRequest = { destExpanded = false }
                            ) {
                                GuatemalaRoutingEngine.departments.forEach { node ->
                                    DropdownMenuItem(
                                        text = { Text(node.name) },
                                        onClick = {
                                            viewModel.updateRouteSelection(origin, node.name, vehicleType)
                                            destExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.refreshGpsLocation()
                            viewModel.updateRouteSelection(gpsLocation.nearestDepartment, destination, vehicleType)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fijar origen por GPS (${gpsLocation.nearestDepartment})")
                    }
                }
            }

            // Interactive Guatemala Corridor Map Canvas
            Card(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mapa Vial Nacional GT", style = MaterialTheme.typography.titleMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", route.totalDistanceKm)} km · ${route.totalMinutes} min",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    GuatemalaRouteCanvas(
                        allNodes = GuatemalaRoutingEngine.departments,
                        pathNodes = route.pathNodes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFF0E1D24), MaterialTheme.shapes.medium)
                    )

                    Text(
                        text = route.advisory,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = RescueCoral)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Centro de Atención Destino",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${route.destination.emergencyHub} (Tel. ${route.destination.emergencyPhone})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            context.startActivity(
                                EmergencyNotificationHelper.buildNavigationIntent(
                                    route.destination.latitude,
                                    route.destination.longitude,
                                    route.destination.emergencyHub
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_gps_navigation")
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Navegación Turn-by-Turn")
                    }
                }
            }

            // Corridor Segments Breakdown
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tramos Óptimos (${route.vehicleType})", style = MaterialTheme.typography.titleMedium)
                    route.segments.forEachIndexed { index, seg ->
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
                                    Text(
                                        text = "${index + 1}. ${seg.fromName} → ${seg.toName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = seg.corridor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${seg.distanceKm.toInt()} km · ${seg.estimatedMinutes} min",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuatemalaRouteCanvas(
    allNodes: List<GuatemalaNode>,
    pathNodes: List<GuatemalaNode>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(18.dp)) {
        val minLat = 13.9
        val maxLat = 17.2
        val minLng = -92.1
        val maxLng = -88.3

        fun toOffset(node: GuatemalaNode): Offset {
            val xNorm = ((node.longitude - minLng) / (maxLng - minLng)).toFloat().coerceIn(0f, 1f)
            val yNorm = (1f - ((node.latitude - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0f, 1f)
            return Offset(x = xNorm * size.width, y = yNorm * size.height)
        }

        // Subtle grid lines
        for (i in 1..4) {
            val y = size.height * (i / 5f)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw all 22 department hubs as subtle nodes
        allNodes.forEach { node ->
            val pt = toOffset(node)
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = 4.dp.toPx(),
                center = pt
            )
        }

        // Draw optimal route polyline
        if (pathNodes.size >= 2) {
            for (i in 0 until pathNodes.lastIndex) {
                val start = toOffset(pathNodes[i])
                val end = toOffset(pathNodes[i + 1])
                drawLine(
                    color = EmeraldPrimary.copy(alpha = 0.55f),
                    start = start,
                    end = end,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = GuatemalaJade,
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                )
            }
        }

        // Highlight route waypoints, origin, and destination
        pathNodes.forEachIndexed { idx, node ->
            val pt = toOffset(node)
            val color = when (idx) {
                0 -> GuatemalaJade
                pathNodes.lastIndex -> RescueCoral
                else -> Color(0xFFF59E0B)
            }
            val radius = if (idx == 0 || idx == pathNodes.lastIndex) 8.dp.toPx() else 5.dp.toPx()
            drawCircle(color = Color.White, radius = radius + 2.dp.toPx(), center = pt)
            drawCircle(color = color, radius = radius, center = pt)
        }
    }
}
