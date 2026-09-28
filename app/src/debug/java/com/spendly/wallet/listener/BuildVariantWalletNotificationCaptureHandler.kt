package com.spendly.wallet.listener

import android.util.Log
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.WalletNotificationCaptureHandler
import com.spendly.wallet.parser.GoogleWalletNotificationParser
import com.spendly.wallet.parser.WalletParseResult

/** Debug-only raw-field visibility for on-device format discovery. */
class BuildVariantWalletNotificationCaptureHandler : WalletNotificationCaptureHandler {
    private val parser = GoogleWalletNotificationParser()

    override fun handle(notification: CapturedWalletNotification) {
        Log.d(TAG, "package=${notification.packageName} key=${notification.notificationKey} postedAt=${notification.postedAtEpochMillis}")
        Log.d(TAG, "title=${notification.title} text=${notification.text}")
        Log.d(TAG, "subText=${notification.subText} bigText=${notification.bigText}")
        when (val result = parser.parse(notification)) {
            is WalletParseResult.Success -> Log.d(TAG, "parse=${result.status} purchase=${result.purchase}")
            is WalletParseResult.NeedsReview -> Log.d(TAG, "parse=${result.status} issue=${result.issue} amount=${result.amount} merchant=${result.merchant} paymentMethod=${result.paymentMethod} cardLast4=${result.cardLast4}")
            WalletParseResult.NotPurchase -> Log.d(TAG, "parse=${result.status}")
        }
    }

    private companion object {
        const val TAG = "SpendlyWalletCapture"
    }
}
