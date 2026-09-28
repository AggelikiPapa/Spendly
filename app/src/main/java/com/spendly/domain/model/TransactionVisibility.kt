package com.spendly.domain.model

/** Shared status rules for normal spending views and the Wallet review queue. */
object TransactionVisibility {
    fun inHistory(transaction: Transaction): Boolean = transaction.importStatus == ImportStatus.CONFIRMED

    fun needsWalletReview(transaction: Transaction): Boolean =
        transaction.source == TransactionSource.GOOGLE_WALLET && transaction.importStatus == ImportStatus.NEEDS_REVIEW
}
