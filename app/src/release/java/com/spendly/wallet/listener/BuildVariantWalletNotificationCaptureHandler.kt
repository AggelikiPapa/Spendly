package com.spendly.wallet.listener

import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.WalletNotificationCaptureHandler

/** Raw notification fields never enter release logs. */
class BuildVariantWalletNotificationCaptureHandler : WalletNotificationCaptureHandler {
    override fun handle(notification: CapturedWalletNotification) = Unit
}
