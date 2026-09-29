package com.spendly.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AccentStore {
    val selected: StateFlow<AccentColor>
    fun setAccent(accent: AccentColor): Boolean
}

class SharedPreferencesAccentStore(context: Context) : AccentStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableSelected = MutableStateFlow(readSelected(preferences))
    override val selected: StateFlow<AccentColor> = mutableSelected

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == KEY) mutableSelected.value = readSelected(prefs)
    }

    init { preferences.registerOnSharedPreferenceChangeListener(listener) }

    override fun setAccent(accent: AccentColor): Boolean =
        preferences.edit().putString(KEY, accent.name).commit().also { saved ->
            if (saved) mutableSelected.value = accent
        }

    private fun readSelected(prefs: SharedPreferences): AccentColor =
        AccentColor.fromStored(runCatching { prefs.getString(KEY, null) }.getOrNull())

    companion object {
        internal const val PREFERENCES = "appearance"
        internal const val KEY = "accent_color"
    }
}
