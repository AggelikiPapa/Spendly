package com.spendly.wallet.capture

/** Text values before Android CharSequence spans are removed. */
data class WalletNotificationTextFields(
    val title: CharSequence? = null,
    val text: CharSequence? = null,
    val subText: CharSequence? = null,
    val bigText: CharSequence? = null,
)

fun interface WalletNotificationCaptureHandler {
    fun handle(notification: CapturedWalletNotification)
}

/** Filters by source before reading text, then hands an Android-free model to the handler. */
class WalletNotificationCapturePipeline(
    private val handler: WalletNotificationCaptureHandler,
    private val sourceMatcher: WalletNotificationSourceMatcher = WalletNotificationSourceMatcher(),
) {
    fun capture(
        packageName: String?,
        notificationKey: String?,
        postedAtEpochMillis: Long,
        readTextFields: () -> WalletNotificationTextFields,
    ) {
        if (!sourceMatcher.isSupported(packageName)) return
        val fields = readTextFields()
        handler.handle(
            CapturedWalletNotification(
                packageName = requireNotNull(packageName),
                notificationKey = notificationKey,
                postedAtEpochMillis = postedAtEpochMillis,
                title = fields.title?.toString(),
                text = fields.text?.toString(),
                subText = fields.subText?.toString(),
                bigText = fields.bigText?.toString(),
            ),
        )
    }
}
