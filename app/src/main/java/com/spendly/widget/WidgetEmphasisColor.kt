package com.spendly.widget

import androidx.compose.ui.graphics.Color
import com.spendly.ui.theme.AccentColor

/** Warning color is fixed and never follows the selected theme accent. */
internal object WidgetEmphasisColor {
    fun resolve(accent: AccentColor, overBudget: Boolean, darkTheme: Boolean): Color = when {
        overBudget && darkTheme -> Color(0xFFF2B8B5)
        overBudget -> Color(0xFFB3261E)
        else -> accent.palette(darkTheme).primary
    }
}
