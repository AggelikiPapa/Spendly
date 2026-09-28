package com.spendly.ui.settings.wallet

import androidx.lifecycle.ViewModel
import com.spendly.wallet.listener.NotificationAccessGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoogleWalletTrackingViewModel(private val access: NotificationAccessGateway) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GoogleWalletTrackingUiState())
    val uiState: StateFlow<GoogleWalletTrackingUiState> = mutableUiState

    init { refreshAccess() }

    fun refreshAccess() {
        mutableUiState.value = mutableUiState.value.copy(isChecking = true, error = null)
        mutableUiState.value = try {
            GoogleWalletTrackingUiState(notificationAccessGranted = access.isAccessGranted(), isChecking = false)
        } catch (_: Exception) {
            GoogleWalletTrackingUiState(isChecking = false, error = "Notification access could not be checked.")
        }
    }

    fun openSystemSettings() {
        val opened = try { access.openSettings() } catch (_: Exception) { false }
        if (!opened) {
            mutableUiState.value = mutableUiState.value.copy(
                error = "Notification access settings could not be opened on this device.",
            )
        }
    }
}
