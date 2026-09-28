package com.spendly.wallet.capture

/** Android-free data for a later parser; never persisted by the capture pipeline. */
data class CapturedWalletNotification(
    val packageName: String,
    val notificationKey: String?,
    val postedAtEpochMillis: Long,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
)
