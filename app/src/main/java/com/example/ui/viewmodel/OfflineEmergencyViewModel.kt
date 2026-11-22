package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.OfflineMedicalCardEntity
import com.example.data.local.VidaQrDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Strictly local Room-backed ViewModel used on the unauthenticated / offline emergency screen.
 * Never touches Firestore so unauthenticated PERMISSION_DENIED errors cannot occur.
 */
class OfflineEmergencyViewModel(dao: VidaQrDao) : ViewModel() {
    val offlineCard: StateFlow<OfflineMedicalCardEntity?> = dao.observeOfflineCard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )
}
