package com.spendly.wallet.listener

import android.util.Log
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.WalletNotificationCaptureHandler
import com.spendly.wallet.importer.WalletTransactionImportCoordinator
import com.spendly.wallet.parser.WalletParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Debug-only raw-field visibility for on-device format discovery. */
class BuildVariantWalletNotificationCaptureHandler(
    private val coordinator: WalletTransactionImportCoordinator,
    private val scope: CoroutineScope,
) : WalletNotificationCaptureHandler {

    override fun handle(notification: CapturedWalletNotification) {
        Log.d(TAG, "package=${notification.packageName} key=${notification.notificationKey} postedAt=${notification.postedAtEpochMillis}")
        Log.d(TAG, "title=${notification.title} text=${notification.text}")
        Log.d(TAG, "subText=${notification.subText} bigText=${notification.bigText}")
        scope.launch {
            val import = coordinator.import(notification)
            when (val result = import.parseResult) {
                is WalletParseResult.Success -> Log.d(TAG, "parse=${result.status} purchase=${result.purchase}")
                is WalletParseResult.NeedsReview -> Log.d(TAG, "parse=${result.status} issue=${result.issue} amount=${result.amount} merchant=${result.merchant} paymentMethod=${result.paymentMethod} cardLast4=${result.cardLast4}")
                WalletParseResult.NotPurchase -> Log.d(TAG, "parse=${result.status}")
                null -> Unit
            }
            Log.d(TAG, "import=${import.outcome}")
        }
    }

    private companion object {
        const val TAG = "SpendlyWalletCapture"
    }
}
