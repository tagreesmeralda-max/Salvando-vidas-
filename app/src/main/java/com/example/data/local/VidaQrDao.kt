package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VidaQrDao {
    @Query("SELECT * FROM offline_medical_cards WHERE id = 1 LIMIT 1")
    fun observeOfflineCard(): Flow<OfflineMedicalCardEntity?>

    @Query("SELECT * FROM offline_medical_cards WHERE id = 1 LIMIT 1")
    suspend fun getOfflineCardOnce(): OfflineMedicalCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineCard(card: OfflineMedicalCardEntity)

    @Query("SELECT * FROM pending_incidents ORDER BY createdAtMillis DESC")
    fun observePendingIncidents(): Flow<List<PendingIncidentEntity>>

    @Query("SELECT * FROM pending_incidents ORDER BY createdAtMillis ASC")
    suspend fun getPendingIncidentsOnce(): List<PendingIncidentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingIncident(incident: PendingIncidentEntity): Long

    @Query("DELETE FROM pending_incidents WHERE localId = :localId")
    suspend fun deletePendingIncident(localId: Long)
}
