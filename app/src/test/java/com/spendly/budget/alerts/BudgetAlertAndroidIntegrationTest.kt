package com.spendly.budget.alerts

import android.app.Application
import android.content.Context
import android.content.Intent
import com.spendly.MainActivity
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BudgetAlertAndroidIntegrationTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun clearPreferences() {
        context.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun deliveredThresholdsAndEnablementSurviveStoreReload() {
        val first = SharedPreferencesBudgetAlertStore(context)
        val september = YearMonth.of(2026, 9)
        assertFalse(first.isEnabled())
        assertTrue(first.setEnabled(true))
        assertTrue(first.markDelivered(september, setOf(BudgetThreshold.SEVENTY, BudgetThreshold.EIGHTY)))
        val reloaded = SharedPreferencesBudgetAlertStore(context)
        assertTrue(reloaded.isEnabled())
        assertEquals(setOf(BudgetThreshold.SEVENTY, BudgetThreshold.EIGHTY), reloaded.delivered(september))
        assertTrue(reloaded.delivered(september.plusMonths(1)).isEmpty())
    }

    @Test fun tapIntentStartsDashboardInCleanActivityTask() {
        val intent = AndroidBudgetAlertSender.dashboardIntent(context)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }
}
