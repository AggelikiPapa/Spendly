package com.spendly.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.spendly.R

data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

/** Fixed, contrast-conscious Material primary tones for both system appearances. */
enum class AccentColor(
    @StringRes val labelRes: Int,
    val light: AccentPalette,
    val dark: AccentPalette,
) {
    Teal(R.string.accent_teal,
        AccentPalette(Color(0xFF006C60), Color.White, Color(0xFF9EF2DE), Color(0xFF00201B)),
        AccentPalette(Color(0xFF82D6C2), Color(0xFF00382F), Color(0xFF005046), Color(0xFF9EF2DE))),
    Green(R.string.accent_green,
        AccentPalette(Color(0xFF386A20), Color.White, Color(0xFFB9F398), Color(0xFF082100)),
        AccentPalette(Color(0xFF9ED67F), Color(0xFF113800), Color(0xFF245000), Color(0xFFB9F398))),
    Blue(R.string.accent_blue,
        AccentPalette(Color(0xFF005AC1), Color.White, Color(0xFFD8E2FF), Color(0xFF001A41)),
        AccentPalette(Color(0xFFADC7FF), Color(0xFF002F65), Color(0xFF00458F), Color(0xFFD8E2FF))),
    Purple(R.string.accent_purple,
        AccentPalette(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D)),
        AccentPalette(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF))),
    Orange(R.string.accent_orange,
        AccentPalette(Color(0xFF925600), Color.White, Color(0xFFFFDDB5), Color(0xFF2F1500)),
        AccentPalette(Color(0xFFFFB86C), Color(0xFF4D2800), Color(0xFF6F3D00), Color(0xFFFFDDB5))),
    Pink(R.string.accent_pink,
        AccentPalette(Color(0xFFA13A65), Color.White, Color(0xFFFFD9E3), Color(0xFF3E001E)),
        AccentPalette(Color(0xFFFFB1CB), Color(0xFF611132), Color(0xFF81284B), Color(0xFFFFD9E3)));

    fun palette(darkTheme: Boolean): AccentPalette = if (darkTheme) dark else light

    companion object {
        fun fromStored(value: String?): AccentColor = entries.firstOrNull { it.name == value } ?: Teal
    }
}
