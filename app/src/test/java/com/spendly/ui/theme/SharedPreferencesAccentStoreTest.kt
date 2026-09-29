package com.spendly.ui.theme

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SharedPreferencesAccentStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun clearPreferences() {
        context.getSharedPreferences(SharedPreferencesAccentStore.PREFERENCES, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun defaultIsTeal() {
        assertEquals(AccentColor.Teal, SharedPreferencesAccentStore(context).selected.value)
    }

    @Test fun changePersistsAndRestoresAfterRecreation() {
        val first = SharedPreferencesAccentStore(context)
        assertTrue(first.setAccent(AccentColor.Purple))
        assertEquals(AccentColor.Purple, first.selected.value)
        assertEquals(AccentColor.Purple, SharedPreferencesAccentStore(context).selected.value)
    }

    @Test fun invalidSavedValueFallsBackToTeal() {
        context.getSharedPreferences(SharedPreferencesAccentStore.PREFERENCES, Context.MODE_PRIVATE)
            .edit().putString(SharedPreferencesAccentStore.KEY, "unknown-color").commit()
        assertEquals(AccentColor.Teal, SharedPreferencesAccentStore(context).selected.value)
        context.getSharedPreferences(SharedPreferencesAccentStore.PREFERENCES, Context.MODE_PRIVATE)
            .edit().putInt(SharedPreferencesAccentStore.KEY, 123).commit()
        assertEquals(AccentColor.Teal, SharedPreferencesAccentStore(context).selected.value)
    }
}
