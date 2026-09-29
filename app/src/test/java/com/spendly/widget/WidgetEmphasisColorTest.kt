package com.spendly.widget

import androidx.compose.ui.graphics.Color
import com.spendly.ui.theme.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WidgetEmphasisColorTest {
    @Test fun selectedAccentSetsWidgetEmphasisInBothModes() {
        AccentColor.entries.forEach { accent ->
            assertEquals(accent.light.primary, WidgetEmphasisColor.resolve(accent, false, false))
            assertEquals(accent.dark.primary, WidgetEmphasisColor.resolve(accent, false, true))
        }
    }

    @Test fun overBudgetUsesFixedWarningColors() {
        AccentColor.entries.forEach { accent ->
            assertEquals(Color(0xFFB3261E), WidgetEmphasisColor.resolve(accent, true, false))
            assertEquals(Color(0xFFF2B8B5), WidgetEmphasisColor.resolve(accent, true, true))
            assertNotEquals(accent.light.primary, WidgetEmphasisColor.resolve(accent, true, false))
        }
    }
}
