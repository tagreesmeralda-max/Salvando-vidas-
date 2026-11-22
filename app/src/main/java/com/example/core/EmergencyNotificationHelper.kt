package com.example.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object EmergencyNotificationHelper {
    private const val CHANNEL_ID = "vidaqr_emergency_alerts"
    private const val CHANNEL_NAME = "Alertas de Emergencia VidaQR"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones críticas de accidentes y envío a contactos de emergencia"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showEmergencyDispatchedNotification(
        context: Context,
        title: String,
        body: String
    ) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    fun buildEmergencySmsIntent(
        phones: List<String>,
        userName: String,
        bloodType: String,
        vehicleSummary: String,
        latitude: Double,
        longitude: Double,
        department: String
    ): Intent? {
        val cleanPhones = phones.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanPhones.isEmpty()) return null
        val recipients = cleanPhones.joinToString(";")
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val message =
            "ALERTA VIDAQR ($department): $userName reportó un accidente ($vehicleSummary). " +
                "Tipo sangre: $bloodType. Ubicación exacta: $mapsLink"
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$recipients")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildDialIntent(phone: String): Intent {
        return Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phone.trim()}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildNavigationIntent(latitude: Double, longitude: Double, label: String): Intent {
        val encodedLabel = Uri.encode(label)
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
