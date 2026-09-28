package com.spendly.wallet.listener

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.spendly.SpendlyApplication
import com.spendly.wallet.capture.WalletNotificationCapturePipeline
import com.spendly.wallet.capture.WalletNotificationTextFields
import com.spendly.wallet.importer.WalletTransactionImportCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Adapts posted Wallet notifications and queues their imports outside the system callback. */
class SpendlyNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val capturePipeline by lazy {
        val spendly = application as SpendlyApplication
        val coordinator = WalletTransactionImportCoordinator(
            spendly.transactionRepository,
            spendly.merchantCategoryRuleRepository,
            spendly.categoryRepository,
        )
        WalletNotificationCapturePipeline(BuildVariantWalletNotificationCaptureHandler(coordinator, serviceScope))
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

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

private fun Bundle?.safeText(key: String): CharSequence? =
    runCatching { this?.getCharSequence(key) }.getOrNull()
