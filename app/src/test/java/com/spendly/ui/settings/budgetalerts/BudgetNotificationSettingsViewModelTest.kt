package com.spendly.ui.settings.budgetalerts

import com.spendly.budget.alerts.BudgetAlertPermission
import com.spendly.budget.alerts.BudgetAlertStore
import com.spendly.budget.alerts.BudgetThreshold
import java.time.YearMonth
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetNotificationSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeStore()
    private val permission = FakePermission()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun missingPermissionShowsSafeStatusAndDoesNotEnable() = runTest {
        permission.granted = false
        val vm = BudgetNotificationSettingsViewModel(store, permission)
        advanceUntilIdle()
        assertEquals(BudgetNotificationStatus.PERMISSION_REQUIRED, vm.uiState.value.status)
        vm.enable()
        assertFalse(store.isEnabled())
        permission.granted = true
        vm.refresh()
        assertEquals(BudgetNotificationStatus.DISABLED, vm.uiState.value.status)
        vm.enable()
        assertTrue(store.isEnabled())
        assertEquals(BudgetNotificationStatus.ENABLED, vm.uiState.value.status)
        vm.setPaceEnabled(true)
        assertTrue(store.isPaceEnabled())
        assertTrue(vm.uiState.value.paceEnabled)
        vm.disable()
        assertEquals(BudgetNotificationStatus.DISABLED, vm.uiState.value.status)
        vm.setPaceEnabled(false)
        assertTrue(store.isPaceEnabled()) // Disabled master does not silently change the pace preference.
    }

    private class FakeStore : BudgetAlertStore {
        private val state = MutableStateFlow(false)
        private val paceState = MutableStateFlow(false)
        override val enabled = state
        override val paceEnabled = paceState
        override fun isEnabled() = state.value
        override fun setEnabled(enabled: Boolean): Boolean { state.value = enabled; return true }
        override fun isPaceEnabled() = paceState.value
        override fun setPaceEnabled(enabled: Boolean): Boolean { paceState.value = enabled; return true }
        override fun delivered(month: YearMonth): Set<BudgetThreshold> = emptySet()
        override fun markDelivered(month: YearMonth, thresholds: Set<BudgetThreshold>) = true
        override fun lastPaceAlertDate(): LocalDate? = null
        override fun markPaceAlertDate(date: LocalDate) = true
    }

    private class FakePermission : BudgetAlertPermission {
        var granted = true
        override fun canPost() = granted
    }
}
