package com.spendly.wallet.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletNotificationCapturePipelineTest {
    private val matcher = WalletNotificationSourceMatcher()

    @Test fun onlyTheGoogleWalletPackageIsSupported() {
        assertTrue(matcher.isSupported(SupportedWalletPackages.GOOGLE_WALLET))
        assertFalse(matcher.isSupported("com.google.android.gms"))
        assertFalse(matcher.isSupported("com.google.android.apps.walletnfcrel.other"))
        assertFalse(matcher.isSupported("example.app"))
        assertFalse(matcher.isSupported(null))
    }

    @Test fun unrelatedNotificationsAreNotReadOrForwarded() {
        val captured = mutableListOf<CapturedWalletNotification>()
        var readCount = 0
        val pipeline = WalletNotificationCapturePipeline(captured::add)

        pipeline.capture("com.google.android.gms", "other-key", 123L) {
            readCount++
            WalletNotificationTextFields(title = "Unrelated private content")
        }
        pipeline.capture(null, null, 124L) {
            readCount++
            WalletNotificationTextFields()
        }

        assertEquals(0, readCount)
        assertTrue(captured.isEmpty())
    }

    @Test fun supportedNotificationForwardsOneAndroidFreeCaptureWithConvertedText() {
        val captured = mutableListOf<CapturedWalletNotification>()
        var readCount = 0
        val pipeline = WalletNotificationCapturePipeline(captured::add)

        pipeline.capture(SupportedWalletPackages.GOOGLE_WALLET, "wallet-key", 987654321L) {
            readCount++
            WalletNotificationTextFields(
                title = StringBuilder("Purchase title"),
                text = StringBuilder("Purchase text"),
                subText = StringBuilder("Card detail"),
                bigText = StringBuilder("Expanded text"),
            )
        }

        assertEquals(1, readCount)
        assertEquals(
            listOf(
                CapturedWalletNotification(
                    packageName = SupportedWalletPackages.GOOGLE_WALLET,
                    notificationKey = "wallet-key",
                    postedAtEpochMillis = 987654321L,
                    title = "Purchase title",
                    text = "Purchase text",
                    subText = "Card detail",
                    bigText = "Expanded text",
                ),
            ),
            captured,
        )
    }

    @Test fun missingFieldsRemainNullAndDoNotPreventCapture() {
        val captured = mutableListOf<CapturedWalletNotification>()
        val pipeline = WalletNotificationCapturePipeline(captured::add)

        pipeline.capture(SupportedWalletPackages.GOOGLE_WALLET, null, 0L) {
            WalletNotificationTextFields(text = "Only text")
        }
        pipeline.capture(SupportedWalletPackages.GOOGLE_WALLET, null, 1L) {
            WalletNotificationTextFields(title = "Only title")
        }
        pipeline.capture(SupportedWalletPackages.GOOGLE_WALLET, null, 2L) {
            WalletNotificationTextFields()
        }

        assertEquals(3, captured.size)
        assertNull(captured[0].title)
        assertEquals("Only text", captured[0].text)
        assertNull(captured[1].text)
        assertEquals("Only title", captured[1].title)
        assertNull(captured[2].title)
        assertNull(captured[2].text)
        assertNull(captured[2].subText)
        assertNull(captured[2].bigText)
    }

    @Test fun repeatedPostsAreForwardedIndependently() {
        val captured = mutableListOf<CapturedWalletNotification>()
        val pipeline = WalletNotificationCapturePipeline(captured::add)
        repeat(2) {
            pipeline.capture(SupportedWalletPackages.GOOGLE_WALLET, "same-key", 5L) {
                WalletNotificationTextFields()
            }
        }

        assertEquals(2, captured.size)
    }
}
