package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.VehicleAiVerificationResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max

class VehicleAiRepository {
    private val verificationSchema = Schema.obj(
        properties = mapOf(
            "isRealVehicle" to Schema.boolean(description = "True if the photo clearly depicts a real physical car or motorcycle, false for screenshots, drawings, or unrelated objects"),
            "detectedType" to Schema.enumeration(
                values = listOf("CARRO", "MOTO", "OTRO"),
                description = "Detected vehicle category"
            ),
            "confidencePercent" to Schema.integer(description = "Confidence score from 0 to 100"),
            "detectedBrandModel" to Schema.string(description = "Estimated brand/model or body style in Spanish"),
            "detectedColor" to Schema.string(description = "Primary exterior color in Spanish"),
            "visiblePlate" to Schema.string(description = "Visible license plate text or 'No visible'"),
            "damageAssessment" to Schema.string(description = "Brief visual condition or damage inspection in Spanish"),
            "verificationSummary" to Schema.string(description = "Concise Spanish summary explaining the verification outcome")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = verificationSchema
            }
        )
    }

    suspend fun verifyVehicleImage(
        context: Context,
        imageUri: Uri,
        expectedVehicleType: String,
        declaredPlate: String
    ): Result<VehicleAiVerificationResult> {
        return try {
            val bitmap = decodeScaledBitmap(context, imageUri)
                ?: return Result.failure(IllegalArgumentException("No se pudo leer la imagen seleccionada."))

            val prompt = buildString {
                append("Eres un inspector vehicular de emergencias viales en Guatemala (VidaQR). ")
                append("Analiza esta fotografía y determina si corresponde a un vehículo real (no captura de pantalla ni objeto ajeno). ")
                append("El usuario declaró tipo de vehículo: '$expectedVehicleType' y placa: '${declaredPlate.ifBlank { "No declarada" }}'. ")
                append("Responde estrictamente en español dentro del esquema JSON.")
            }

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }

            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("El modelo de IA devolvió una respuesta vacía."))

            val json = JSONObject(jsonText)
            val result = VehicleAiVerificationResult(
                isRealVehicle = json.optBoolean("isRealVehicle", false),
                detectedType = json.optString("detectedType", "OTRO"),
                confidencePercent = json.optInt("confidencePercent", 0).coerceIn(0, 100),
                detectedBrandModel = json.optString("detectedBrandModel", "No identificado"),
                detectedColor = json.optString("detectedColor", "No identificado"),
                visiblePlate = json.optString("visiblePlate", "No visible"),
                damageAssessment = json.optString("damageAssessment", "Sin daños visibles"),
                verificationSummary = json.optString("verificationSummary", "Análisis completado.")
            )
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI vehicle verification timed out", e)
            Result.failure(Exception("La verificación de IA tardó más de 60 segundos. Intenta de nuevo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI vehicle verification failed", e)
            Result.failure(e)
        }
    }

    private suspend fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return@withContext null
                val maxDim = 1024
                val largest = max(raw.width, raw.height)
                if (largest <= maxDim) return@withContext raw
                val scale = maxDim.toFloat() / largest.toFloat()
                Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * scale).toInt().coerceAtLeast(1),
                    (raw.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
        }

    private companion object {
        const val TAG = "VehicleAiRepo"
    }
}
