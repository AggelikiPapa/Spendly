package com.spendly.wallet.importer

import java.util.Locale

/** Exact merchant identity for local rules and Wallet duplicate comparisons. */
object MerchantNormalizer {
    fun normalize(merchant: String?): String? = merchant
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.lowercase(Locale.ROOT)
        ?.replace(Regex("\\s+"), " ")
}
