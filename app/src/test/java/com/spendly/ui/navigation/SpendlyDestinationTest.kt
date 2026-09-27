package com.spendly.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SpendlyDestinationTest {
    @Test fun bottomNavigationHasOnlyPrimaryDestinations() {
        val bottom = SpendlyDestination.bottomNavigation
        assertEquals(
            listOf(SpendlyDestination.Dashboard, SpendlyDestination.Transactions, SpendlyDestination.Analytics),
            bottom,
        )
        assertFalse(bottom.contains(SpendlyDestination.Review))
        assertFalse(bottom.contains(SpendlyDestination.Settings))
    }
}
