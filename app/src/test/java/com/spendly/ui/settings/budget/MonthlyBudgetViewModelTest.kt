package com.spendly.ui.settings.budget

import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Money
import com.spendly.domain.repository.MonthlyBudgetRepository
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyBudgetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeBudgets()
    private val month = YearMonth.of(2026, 9)
    private val clock = Clock.fixed(Instant.parse("2026-09-15T09:30:00Z"), ZoneId.of("Europe/Athens"))

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun noExistingBudgetLoadsCurrentMonthWithEmptyInput() = runTest {
        val vm = newViewModel()
        assertEquals(MonthlyBudgetUiState.Loading, vm.uiState.value)
        advanceUntilIdle()
        val state = vm.uiState.value as MonthlyBudgetUiState.Ready
        assertEquals(month, state.month)
        assertEquals(listOf(month), repository.observedMonths)
        assertNull(state.currentLimit)
        assertEquals("", state.amountInput)
        assertTrue(repository.upserts.isEmpty())
    }

    @Test fun existingBudgetPrepopulatesAndUpdatesReactively() = runTest {
        repository.budget.value = MonthlyBudget(month, Money(100_050, "EUR"))
        val vm = newViewModel()
        advanceUntilIdle()
        assertEquals("1000.50", (vm.uiState.value as MonthlyBudgetUiState.Ready).amountInput)
        repository.budget.value = MonthlyBudget(month, Money(200_000, "EUR"))
        advanceUntilIdle()
        val state = vm.uiState.value as MonthlyBudgetUiState.Ready
        assertEquals("2000.00", state.amountInput)
        assertEquals(Money(200_000, "EUR"), state.currentLimit)
    }

    @Test fun validInputCreatesOrUpsertsCurrentMonthInEur() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.onAmountChanged("1000.50")
        vm.save()
        advanceUntilIdle()
        assertEquals(MonthlyBudget(month, Money(100_050, "EUR")), repository.upserts.single())
        assertEquals(SaveStatus.Saved, (vm.uiState.value as MonthlyBudgetUiState.Ready).saveStatus)

        vm.onAmountChanged("1200")
        vm.save()
        advanceUntilIdle()
        assertEquals(2, repository.upserts.size)
        assertEquals(month, repository.upserts.last().yearMonth)
        assertEquals(Money(120_000, "EUR"), repository.budget.value?.limit)
    }

    @Test fun zeroIsAllowedByDomainWhileInvalidAndNegativeAreRejected() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        for (input in listOf("", "-1", "abc", "1.234")) {
            vm.onAmountChanged(input)
            vm.save()
            assertNotNull((vm.uiState.value as MonthlyBudgetUiState.Ready).amountError)
        }
        assertTrue(repository.upserts.isEmpty())
        vm.onAmountChanged("0")
        vm.save()
        advanceUntilIdle()
        assertEquals(Money(0, "EUR"), repository.upserts.single().limit)
    }

    @Test fun duplicateSaveWhilePendingUpsertsOnlyOnce() = runTest {
        repository.gate = CompletableDeferred()
        val vm = newViewModel()
        advanceUntilIdle()
        vm.onAmountChanged("100")
        vm.save()
        vm.save()
        runCurrent()
        vm.save()
        assertEquals(1, repository.upsertCalls)
        repository.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.upserts.size)
    }

    @Test fun repositoryFailureShowsNontechnicalErrorAndRetainsInput() = runTest {
        repository.failure = IllegalStateException("database internals")
        val vm = newViewModel()
        advanceUntilIdle()
        vm.onAmountChanged("250.00")
        vm.save()
        advanceUntilIdle()
        val state = vm.uiState.value as MonthlyBudgetUiState.Ready
        assertEquals(SaveStatus.Idle, state.saveStatus)
        assertEquals("250.00", state.amountInput)
        assertNotNull(state.saveError)
        assertFalse(state.saveError!!.contains("database internals"))
    }

    private fun newViewModel() = MonthlyBudgetViewModel(repository, clock)

    private class FakeBudgets : MonthlyBudgetRepository {
        val budget = MutableStateFlow<MonthlyBudget?>(null)
        val observedMonths = mutableListOf<YearMonth>()
        val upserts = mutableListOf<MonthlyBudget>()
        var upsertCalls = 0
        var gate: CompletableDeferred<Unit>? = null
        var failure: Exception? = null
        override suspend fun upsert(budget: MonthlyBudget) {
            upsertCalls++
            gate?.await()
            failure?.let { throw it }
            upserts += budget
            this.budget.value = budget
        }
        override suspend fun getByMonth(yearMonth: YearMonth): MonthlyBudget? = error("Unused")
        override fun observeByMonth(yearMonth: YearMonth): Flow<MonthlyBudget?> {
            observedMonths += yearMonth
            return budget
        }
    }
}
