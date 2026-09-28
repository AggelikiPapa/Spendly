package com.spendly.ui.settings.wallet

data class GoogleWalletTrackingUiState(
    val isChecking: Boolean = true,
    val notificationAccessGranted: Boolean = false,
    val error: String? = null,
)
