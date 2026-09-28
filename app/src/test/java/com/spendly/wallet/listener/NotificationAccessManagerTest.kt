package com.spendly.wallet.listener

import android.content.ComponentName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationAccessManagerTest {
    @Test fun api26EnabledListenerListMatchesExactComponent() {
        val listener = ComponentName("com.spendly", "com.spendly.wallet.listener.SpendlyNotificationListenerService")
        val other = ComponentName("example.other", "example.other.Listener")
        assertTrue(enabledListenerComponentsContains("${other.flattenToString()}:${listener.flattenToString()}", listener))
        assertFalse(enabledListenerComponentsContains(other.flattenToString(), listener))
        assertFalse(enabledListenerComponentsContains(null, listener))
    }
}
