package com.spendly.wallet.parser

import com.spendly.domain.model.Money
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.SupportedWalletPackages
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleWalletNotificationParserTest {
    private val parser = GoogleWalletNotificationParser()

    private fun notification(
        title: String? = "Shop",
        text: String? = "€2.95 with Visa ••1234",
        postedAt: Long = 1_725_000_000_123L,
    ) = CapturedWalletNotification(
        packageName = SupportedWalletPackages.GOOGLE_WALLET,
        notificationKey = "key",
        postedAtEpochMillis = postedAt,
        title = title,
        text = text,
        subText = null,
        bigText = null,
    )

    private fun success(title: String? = "Shop", text: String): ParsedWalletPurchase {
        val result = parser.parse(notification(title, text))
        assertTrue("Expected success, got $result", result is WalletParseResult.Success)
        assertEquals(WalletParseStatus.SUCCESS, result.status)
        return (result as WalletParseResult.Success).purchase
    }

    private fun review(title: String? = "Shop", text: String): WalletParseResult.NeedsReview {
        val result = parser.parse(notification(title, text))
        assertTrue("Expected review, got $result", result is WalletParseResult.NeedsReview)
        assertEquals(WalletParseStatus.NEEDS_REVIEW, result.status)
        return result as WalletParseResult.NeedsReview
    }

    @Test fun observedRealNfcPurchaseParsesExactly() {
        val result = parser.parse(notification(title = "GPK MARKET IKE", text = "€2.95 with Ticket Restaurant® ••5311"))
        assertEquals(WalletParseStatus.SUCCESS, result.status)
        val purchase = (result as WalletParseResult.Success).purchase
        assertEquals("GPK MARKET IKE", purchase.merchant)
        assertEquals(295L, purchase.amount.amountMinor)
        assertEquals("EUR", purchase.amount.currencyCode)
        assertEquals("Ticket Restaurant®", purchase.paymentMethod)
        assertEquals("5311", purchase.cardLast4)
        assertEquals(Instant.ofEpochMilli(1_725_000_000_123L), purchase.occurredAt)
    }

    @Test fun otherPaymentMethodLabelsAreNotHardcoded() {
        val visa = success(text = "€12.40 with Visa ••1234")
        val mastercard = success(text = "€18.00 with Mastercard ••9876")
        assertEquals(Money(1_240, "EUR"), visa.amount)
        assertEquals("Visa", visa.paymentMethod)
        assertEquals("1234", visa.cardLast4)
        assertEquals(Money(1_800, "EUR"), mastercard.amount)
        assertEquals("Mastercard", mastercard.paymentMethod)
        assertEquals("9876", mastercard.cardLast4)
    }

    @Test fun wholeEurosAndThousandsAreExact() {
        assertEquals(Money(10_000, "EUR"), success(text = "€100 with Visa ••1234").amount)
        assertEquals(Money(125_000, "EUR"), success(text = "€1,250.00 with Visa ••1234").amount)
        assertEquals(Money(5, "EUR"), success(text = "€0.05 with Visa ••1234").amount)
    }

    @Test fun merchantAndPaymentMethodAreTrimmed() {
        val purchase = success("  GPK MARKET IKE  ", "  €2.95   with    Ticket Restaurant®    ••5311  ")
        assertEquals("GPK MARKET IKE", purchase.merchant)
        assertEquals("Ticket Restaurant®", purchase.paymentMethod)
    }

    @Test fun missingMerchantRequiresReviewButKeepsAmount() {
        for (title in listOf(null, "  ")) {
            val result = review(title, "€2.95 with Visa ••1234")
            assertEquals(WalletParseIssue.MISSING_MERCHANT, result.issue)
            assertEquals(Money(295, "EUR"), result.amount)
            assertNull(result.merchant)
        }
    }

    @Test fun missingTextAndUnrelatedWalletContentAreNotPurchases() {
        assertSame(WalletParseResult.NotPurchase, parser.parse(notification(text = null)))
        assertSame(WalletParseResult.NotPurchase, parser.parse(notification(text = "")))
        assertSame(WalletParseResult.NotPurchase, parser.parse(notification(text = "Your pass is ready")))
        assertSame(WalletParseResult.NotPurchase, parser.parse(notification(text = "Earn rewards with Wallet")))
        assertSame(WalletParseResult.NotPurchase, parser.parse(notification(text = "Get €2 off your next trip")))
    }

    @Test fun malformedAndAmbiguousAmountsRequireReviewWithoutRounding() {
        assertEquals(WalletParseIssue.INVALID_AMOUNT, review(text = "€2.xx with Visa ••1234").issue)
        assertEquals(WalletParseIssue.INVALID_AMOUNT, review(text = "€2.999 with Visa ••1234").issue)
        assertEquals(WalletParseIssue.INVALID_AMOUNT, review(text = "€1.250,00 with Visa ••1234").issue)
        assertEquals(WalletParseIssue.INVALID_AMOUNT, review(text = "€999999999999999999999 with Visa ••1234").issue)
    }

    @Test fun unsupportedCurrencyRequiresReview() {
        val result = review(text = "$2.95 with Visa ••1234")
        assertEquals(WalletParseIssue.UNSUPPORTED_CURRENCY, result.issue)
        assertNull(result.amount)
    }

    @Test fun missingOrMalformedCardSuffixRequiresReview() {
        assertEquals(WalletParseIssue.INVALID_CARD_SUFFIX, review(text = "€2.95 with Visa").issue)
        assertEquals(WalletParseIssue.INVALID_CARD_SUFFIX, review(text = "€2.95 with Visa ••123").issue)
        assertEquals(WalletParseIssue.INVALID_CARD_SUFFIX, review(text = "€2.95 with Visa ••12345").issue)
        assertEquals(WalletParseIssue.INVALID_CARD_SUFFIX, review(text = "€2.95 with Visa **1234").issue)
    }

    @Test fun missingPaymentMethodRequiresReview() {
        val result = review(text = "€2.95 with ••1234")
        assertEquals(WalletParseIssue.MISSING_PAYMENT_METHOD, result.issue)
        assertEquals("1234", result.cardLast4)
    }

    @Test fun amountWithoutMetadataSucceedsButUnexpectedSuffixRequiresReview() {
        val purchase = success(text = "€2.95")
        assertNull(purchase.paymentMethod)
        assertNull(purchase.cardLast4)
        assertEquals(WalletParseIssue.UNRECOGNIZED_TEXT_FORMAT, review(text = "€2.95 at Shop").issue)
    }

    @Test fun captureTimestampIsUsedAndParsingIsDeterministic() {
        val capture = notification(postedAt = 0L)
        val first = parser.parse(capture)
        assertEquals(first, parser.parse(capture))
        assertEquals(Instant.EPOCH, (first as WalletParseResult.Success).purchase.occurredAt)
    }
}
