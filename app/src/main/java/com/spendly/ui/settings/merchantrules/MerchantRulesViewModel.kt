package com.spendly.ui.settings.merchantrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Category
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MerchantRuleRow(val rule: MerchantCategoryRule, val categoryName: String?, val isCategoryInactive: Boolean)
data class MerchantRuleEditor(val ruleId: Long, val merchant: String, val categoryId: Long?, val error: String? = null)
data class MerchantRulesUiState(
    val isLoading: Boolean = true,
    val rows: List<MerchantRuleRow> = emptyList(),
    val activeCategories: List<Category> = emptyList(),
    val editor: MerchantRuleEditor? = null,
    val deleteRuleId: Long? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

class MerchantRulesViewModel(
    private val rules: MerchantCategoryRuleRepository,
    private val categories: CategoryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MerchantRulesUiState())
    val uiState: StateFlow<MerchantRulesUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(rules.observeAll(), categories.observeAll()) { allRules, allCategories ->
                val byId = allCategories.associateBy { it.id }
                val rows = allRules.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.merchantPattern }).map { rule ->
                    val category = byId[rule.categoryId]
                    MerchantRuleRow(rule, category?.name, category?.isActive == false)
                }
                rows to allCategories.filter { it.isActive }.sortedBy { it.name.lowercase() }
            }.catch {
                mutableUiState.update { it.copy(isLoading = false, error = "Merchant rules could not be loaded.") }
            }.collect { (rows, activeCategories) ->
                mutableUiState.update { it.copy(isLoading = false, rows = rows, activeCategories = activeCategories, error = null) }
            }
        }
    }

    fun openEditor(id: Long) {
        val state = uiState.value
        if (state.isLoading || state.isSaving) return
        val row = state.rows.firstOrNull { it.rule.id == id } ?: return
        mutableUiState.update { it.copy(editor = MerchantRuleEditor(id, row.rule.merchantPattern,
            row.rule.categoryId.takeIf { categoryId -> state.activeCategories.any { category -> category.id == categoryId } }), error = null) }
    }

    fun selectCategory(id: Long) = mutableUiState.update { state ->
        if (state.isSaving || state.activeCategories.none { it.id == id }) state
        else state.copy(editor = state.editor?.copy(categoryId = id, error = null))
    }

    fun dismissEditor() = mutableUiState.update { if (it.isSaving) it else it.copy(editor = null) }

    fun saveEditor() {
        val state = uiState.value
        val editor = state.editor ?: return
        if (state.isSaving) return
        val categoryId = editor.categoryId
        if (categoryId == null || state.activeCategories.none { it.id == categoryId }) {
            mutableUiState.update { it.copy(editor = editor.copy(error = "Choose an active category.")) }
            return
        }
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val current = rules.getById(editor.ruleId) ?: throw IllegalStateException("Rule no longer exists")
                if (categories.getById(categoryId)?.isActive != true || rules.update(current.copy(categoryId = categoryId)) == 0) {
                    throw IllegalStateException("Rule or category no longer exists")
                }
                mutableUiState.update { it.copy(isSaving = false, editor = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(isSaving = false, editor = it.editor?.copy(error = "Could not save the merchant rule. Please try again.")) }
            }
        }
    }

    fun requestDelete(id: Long) = mutableUiState.update { state ->
        if (state.isSaving || state.rows.none { it.rule.id == id }) state else state.copy(deleteRuleId = id, error = null)
    }

    fun cancelDelete() = mutableUiState.update { if (it.isSaving) it else it.copy(deleteRuleId = null) }

    fun confirmDelete() {
        val id = uiState.value.deleteRuleId ?: return
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                if (rules.deleteById(id) == 0) throw IllegalStateException("Rule no longer exists")
                mutableUiState.update { it.copy(isSaving = false, deleteRuleId = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(isSaving = false, error = "Could not delete the merchant rule. Please try again.") }
            }
        }
    }
}
