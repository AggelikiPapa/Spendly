package com.spendly.wallet.parser

import com.spendly.domain.model.Money
import com.spendly.wallet.capture.CapturedWalletNotification
import java.math.BigDecimal
import java.time.Instant

/** Parses only the observed leading-EUR Wallet purchase shape; unknown shapes remain untrusted. */
class GoogleWalletNotificationParser {
    fun parse(notification: CapturedWalletNotification): WalletParseResult {
        val text = notification.text?.trim().orEmpty()
        val merchant = notification.title?.trim()?.takeIf(String::isNotEmpty)
        val occurredAt = Instant.ofEpochMilli(notification.postedAtEpochMillis)
        val leading = LEADING_CURRENCY.matchEntire(text) ?: return WalletParseResult.NotPurchase
        val currency = leading.groupValues[1]
        val amountToken = leading.groupValues[2]
        val remainder = leading.groupValues[3].trim()

        // A currency symbol alone in a promotion is not enough evidence of a purchase.
        if (!amountToken.first().isDigit() && !remainder.startsWith("with ", ignoreCase = true)) {
            return WalletParseResult.NotPurchase
        }

        fun review(issue: WalletParseIssue, amount: Money? = null, paymentMethod: String? = null, cardLast4: String? = null) =
            WalletParseResult.NeedsReview(issue, amount, merchant, paymentMethod, cardLast4, occurredAt)

        if (currency != "€") return review(WalletParseIssue.UNSUPPORTED_CURRENCY)
        val amount = parseEuroAmount(amountToken) ?: return review(WalletParseIssue.INVALID_AMOUNT)
        if (merchant == null) return review(WalletParseIssue.MISSING_MERCHANT, amount)
        if (remainder.isEmpty()) {
            return WalletParseResult.Success(ParsedWalletPurchase(amount, merchant, null, null, occurredAt))
        }

        val withMatch = WITH_PAYMENT.matchEntire(remainder)
            ?: return review(WalletParseIssue.UNRECOGNIZED_TEXT_FORMAT, amount)
        val paymentDetails = withMatch.groupValues[1].trim()
        val maskedCard = MASKED_CARD.matchEntire(paymentDetails)
            ?: return review(WalletParseIssue.INVALID_CARD_SUFFIX, amount)
        val paymentMethod = maskedCard.groupValues[1].trim().takeIf(String::isNotEmpty)
            ?: return review(WalletParseIssue.MISSING_PAYMENT_METHOD, amount, cardLast4 = maskedCard.groupValues[2])
        return WalletParseResult.Success(
            ParsedWalletPurchase(amount, merchant, paymentMethod, maskedCard.groupValues[2], occurredAt),
        )
    }

    private fun parseEuroAmount(token: String): Money? {
        if (!EUR_AMOUNT.matches(token)) return null
        return runCatching {
            val minor = BigDecimal(token.replace(",", ""))
                .movePointRight(2)
                .longValueExact()
            Money(minor, "EUR")
        }.getOrNull()
    }

    private companion object {
        val LEADING_CURRENCY = Regex("^([€$£])([^\\s]+)(?:\\s+(.*))?$")
        val EUR_AMOUNT = Regex("(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]{1,2})?")
        val WITH_PAYMENT = Regex("with\\s+(.+)", RegexOption.IGNORE_CASE)
        val MASKED_CARD = Regex("(.*?)\\s*••([0-9]{4})")
    }
}
