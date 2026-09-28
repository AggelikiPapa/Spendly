package com.spendly.wallet.parser

import com.spendly.domain.model.Money
import java.time.Instant

enum class WalletParseStatus { SUCCESS, NEEDS_REVIEW, NOT_A_PURCHASE }

enum class WalletParseIssue {
    MISSING_MERCHANT,
    INVALID_AMOUNT,
    UNSUPPORTED_CURRENCY,
    MISSING_PAYMENT_METHOD,
    INVALID_CARD_SUFFIX,
    UNRECOGNIZED_TEXT_FORMAT,
}

data class ParsedWalletPurchase(
    val amount: Money,
    val merchant: String,
    val paymentMethod: String?,
    val cardLast4: String?,
    val occurredAt: Instant,
)

sealed interface WalletParseResult {
    val status: WalletParseStatus

    data class Success(val purchase: ParsedWalletPurchase) : WalletParseResult {
        override val status = WalletParseStatus.SUCCESS
    }

    data class NeedsReview(
        val issue: WalletParseIssue,
        val amount: Money?,
        val merchant: String?,
        val paymentMethod: String?,
        val cardLast4: String?,
        val occurredAt: Instant,
    ) : WalletParseResult {
        override val status = WalletParseStatus.NEEDS_REVIEW
    }

    data object NotPurchase : WalletParseResult {
        override val status = WalletParseStatus.NOT_A_PURCHASE
    }
}
