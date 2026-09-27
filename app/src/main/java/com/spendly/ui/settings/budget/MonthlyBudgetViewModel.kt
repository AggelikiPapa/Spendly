package com.spendly.ui.settings.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.MoneyInputParser
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Money
import com.spendly.domain.repository.MonthlyBudgetRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MonthlyBudgetViewModel(
    private val repository: MonthlyBudgetRepository,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    val month: YearMonth = YearMonth.now(clock)
    private val mutableUiState = MutableStateFlow<MonthlyBudgetUiState>(MonthlyBudgetUiState.Loading)
    val uiState: StateFlow<MonthlyBudgetUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.observeByMonth(month).catch {
                mutableUiState.value = MonthlyBudgetUiState.LoadError("Monthly budget could not be loaded.")
            }.collect { budget ->
                mutableUiState.update { current ->
                    val ready = current as? MonthlyBudgetUiState.Ready
                    val input = if (ready?.hasUserEdited == true) ready.amountInput else budget?.limit?.toInput().orEmpty()
                    MonthlyBudgetUiState.Ready(
                        month = month,
                        currentLimit = budget?.limit,
                        amountInput = input,
                        amountError = ready?.amountError,
                        saveError = ready?.saveError,
                        saveStatus = ready?.saveStatus ?: SaveStatus.Idle,
                        hasUserEdited = ready?.hasUserEdited ?: false,
                    )
                }
            }
        }
    }

    fun onAmountChanged(value: String) {
        mutableUiState.update { current ->
            val ready = current as? MonthlyBudgetUiState.Ready ?: return@update current
            if (ready.saveStatus == SaveStatus.Saving) current else ready.copy(
                amountInput = value,
                amountError = null,
                saveError = null,
                saveStatus = SaveStatus.Idle,
                hasUserEdited = true,
            )
        }
    }

    fun save() {
        val ready = uiState.value as? MonthlyBudgetUiState.Ready ?: return
        if (ready.saveStatus == SaveStatus.Saving) return
        val amount = MoneyInputParser.parseNonNegative(ready.amountInput, "EUR", 2)
        if (amount == null) {
            mutableUiState.update { current ->
                (current as? MonthlyBudgetUiState.Ready)?.copy(
                    amountError = "Enter a valid amount of zero or more (up to 2 decimals).",
                    saveError = null,
                    saveStatus = SaveStatus.Idle,
                ) ?: current
            }
            return
        }
        mutableUiState.update { current ->
            (current as? MonthlyBudgetUiState.Ready)?.copy(
                amountError = null, saveError = null, saveStatus = SaveStatus.Saving,
            ) ?: current
        }
        viewModelScope.launch {
            try {
                repository.upsert(MonthlyBudget(month, amount))
                mutableUiState.update { current ->
                    (current as? MonthlyBudgetUiState.Ready)?.copy(
                        currentLimit = amount,
                        saveStatus = SaveStatus.Saved,
                        hasUserEdited = false,
                    ) ?: current
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { current ->
                    (current as? MonthlyBudgetUiState.Ready)?.copy(
                        saveStatus = SaveStatus.Idle,
                        saveError = "Could not save the monthly budget. Please try again.",
                    ) ?: current
                }
            }
        }
    }

    private fun Money.toInput(): String = BigDecimal.valueOf(amountMinor, 2).toPlainString()
}
