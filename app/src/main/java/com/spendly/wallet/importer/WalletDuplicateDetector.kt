package com.spendly.wallet.importer

import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.repository.TransactionRepository
import java.time.Duration
import java.util.Locale

enum class DuplicateCheckResult {
    UNIQUE,
    DUPLICATE_BY_EXTERNAL_REFERENCE,
    DUPLICATE_BY_HEURISTIC,
}

/** Uses exact Wallet references first, then a deliberately narrow purchase comparison. */
class WalletDuplicateDetector(private val transactions: TransactionRepository) {
    suspend fun check(candidate: Transaction): DuplicateCheckResult {
        require(candidate.source == TransactionSource.GOOGLE_WALLET)
        val reference = candidate.externalReference?.takeIf { it.isNotBlank() }
        if (reference != null && transactions.getBySourceAndExternalReference(TransactionSource.GOOGLE_WALLET, reference) != null) {
            return DuplicateCheckResult.DUPLICATE_BY_EXTERNAL_REFERENCE
        }

        val merchant = normalizeMerchant(candidate.merchant) ?: return DuplicateCheckResult.UNIQUE
        val nearby = transactions.getBySourceInTimeRange(
            TransactionSource.GOOGLE_WALLET,
            candidate.occurredAt.minus(DUPLICATE_WINDOW),
            candidate.occurredAt.plus(DUPLICATE_WINDOW),
        )
        return if (nearby.any { existing ->
                existing.amount == candidate.amount && normalizeMerchant(existing.merchant) == merchant
            }
        ) DuplicateCheckResult.DUPLICATE_BY_HEURISTIC else DuplicateCheckResult.UNIQUE
    }

    companion object {
        val DUPLICATE_WINDOW: Duration = Duration.ofMinutes(2)

        internal fun normalizeMerchant(merchant: String?): String? = merchant
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase(Locale.ROOT)
            ?.replace(Regex("\\s+"), " ")
    }
}
