package com.spendly.wallet.listener

import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.WalletNotificationCaptureHandler
import com.spendly.wallet.parser.GoogleWalletNotificationParser

/** Parse in memory without release logging or persistence. */
class BuildVariantWalletNotificationCaptureHandler : WalletNotificationCaptureHandler {
    private val parser = GoogleWalletNotificationParser()

    override fun handle(notification: CapturedWalletNotification) {
        parser.parse(notification)
    }
}
