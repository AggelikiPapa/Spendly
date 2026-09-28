package com.spendly.ui.settings.wallet

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.wallet.listener.NotificationAccessGateway

fun googleWalletTrackingViewModelFactory(access: NotificationAccessGateway) = viewModelFactory {
    initializer { GoogleWalletTrackingViewModel(access) }
}
