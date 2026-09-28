package com.spendly.wallet.listener

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.spendly.wallet.capture.WalletNotificationCapturePipeline
import com.spendly.wallet.capture.WalletNotificationTextFields

/** Adapts posted Wallet notifications; parsing and persistence belong to later tickets. */
class SpendlyNotificationListenerService : NotificationListenerService() {
    private val capturePipeline by lazy {
        WalletNotificationCapturePipeline(BuildVariantWalletNotificationCaptureHandler())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        capturePipeline.capture(sbn.packageName, sbn.key, sbn.postTime) {
            val extras = runCatching { sbn.notification?.extras }.getOrNull()
            WalletNotificationTextFields(
                title = extras.safeText(Notification.EXTRA_TITLE),
                text = extras.safeText(Notification.EXTRA_TEXT),
                subText = extras.safeText(Notification.EXTRA_SUB_TEXT),
                bigText = extras.safeText(Notification.EXTRA_BIG_TEXT),
            )
        }
    }
}

private fun Bundle?.safeText(key: String): CharSequence? =
    runCatching { this?.getCharSequence(key) }.getOrNull()
