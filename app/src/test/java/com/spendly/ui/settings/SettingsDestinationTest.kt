package com.spendly.ui.settings

import com.spendly.ui.navigation.SpendlyDestination
import com.spendly.ui.navigation.SpendlyRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDestinationTest {
    @Test fun walletTrackingIsASettingsDestinationWithItsOwnRoute() {
        assertTrue(SettingsDestination.entries.contains(SettingsDestination.GoogleWalletTracking))
        assertEquals(SpendlyRoutes.GoogleWalletTracking, SettingsDestination.GoogleWalletTracking.route)
        assertEquals(4, SettingsDestination.entries.map { it.route }.distinct().size)
        assertEquals(SpendlyRoutes.MerchantRules, SettingsDestination.MerchantRules.route)
        assertFalse(SpendlyDestination.bottomNavigation.any { it.route == SpendlyRoutes.GoogleWalletTracking })
    }
}
