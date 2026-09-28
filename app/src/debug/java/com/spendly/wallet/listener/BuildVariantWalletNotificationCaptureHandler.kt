package com.spendly.wallet.listener

import android.util.Log
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.WalletNotificationCaptureHandler

/** Debug-only raw-field visibility for on-device format discovery. */
class BuildVariantWalletNotificationCaptureHandler : WalletNotificationCaptureHandler {
    override fun handle(notification: CapturedWalletNotification) {
        Log.d(TAG, "package=${notification.packageName} key=${notification.notificationKey} postedAt=${notification.postedAtEpochMillis}")
        Log.d(TAG, "title=${notification.title} text=${notification.text}")
        Log.d(TAG, "subText=${notification.subText} bigText=${notification.bigText}")
    }

    private companion object {
        const val TAG = "SpendlyWalletCapture"
    }
}
