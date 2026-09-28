package com.spendly.wallet.listener

import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.WalletNotificationCaptureHandler
import com.spendly.wallet.importer.WalletTransactionImportCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Import without release logging of notification or purchase details. */
class BuildVariantWalletNotificationCaptureHandler(
    private val coordinator: WalletTransactionImportCoordinator,
    private val scope: CoroutineScope,
) : WalletNotificationCaptureHandler {

    override fun handle(notification: CapturedWalletNotification) {
        scope.launch { coordinator.import(notification) }
    }
}
