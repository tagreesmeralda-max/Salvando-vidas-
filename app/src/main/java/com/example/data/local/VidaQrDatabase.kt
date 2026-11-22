package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineMedicalCardEntity::class, PendingIncidentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VidaQrDatabase : RoomDatabase() {
    abstract fun vidaQrDao(): VidaQrDao

    companion object {
        @Volatile
        private var INSTANCE: VidaQrDatabase? = null

        fun getInstance(context: Context): VidaQrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VidaQrDatabase::class.java,
                    "vidaqr_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
