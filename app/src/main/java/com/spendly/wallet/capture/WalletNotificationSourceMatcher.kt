package com.spendly.wallet.capture

object SupportedWalletPackages {
    // Package ID from the current Google Wallet listing on Google Play.
    const val GOOGLE_WALLET = "com.google.android.apps.walletnfcrel"

    val names: Set<String> = setOf(GOOGLE_WALLET)
}

class WalletNotificationSourceMatcher(
    private val supportedPackages: Set<String> = SupportedWalletPackages.names,
) {
    fun isSupported(packageName: String?): Boolean = packageName != null && packageName in supportedPackages
}
