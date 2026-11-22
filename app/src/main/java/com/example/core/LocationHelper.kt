package com.example.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.data.routing.GuatemalaRoutingEngine
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class PreciseLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val nearestDepartment: String,
    val isLiveGps: Boolean
)

class LocationHelper(private val context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getPreciseLocation(fallbackDepartment: String = "Guatemala"): PreciseLocationResult {
        val location: Location? = withTimeoutOrNull(8_000L) {
            try {
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                    ?: fusedClient.lastLocation.await()
            } catch (_: Exception) {
                try {
                    fusedClient.lastLocation.await()
                } catch (_: Exception) {
                    null
                }
            }
        }

        if (location != null) {
            val dept = GuatemalaRoutingEngine.findNearestDepartment(
                location.latitude,
                location.longitude
            )
            return PreciseLocationResult(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy.toDouble().coerceAtLeast(1.0),
                nearestDepartment = dept.name,
                isLiveGps = true
            )
        }

        val fallbackNode = GuatemalaRoutingEngine.departmentByName(fallbackDepartment)
        return PreciseLocationResult(
            latitude = fallbackNode.latitude,
            longitude = fallbackNode.longitude,
            accuracyMeters = 50.0,
            nearestDepartment = fallbackNode.name,
            isLiveGps = false
        )
    }
}
