package com.spendly.ui.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Category
import com.spendly.domain.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryManagementViewModel(private val repository: CategoryRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.observeAll().catch {
                mutableUiState.update { state ->
                    state.copy(isLoading = false, loadError = "Categories could not be loaded.")
                }
            }.collect { categories ->
                mutableUiState.update { state ->
                    state.copy(categories = categories, isLoading = false, loadError = null)
                }
            }
        }
    }

    fun startAdd() {
        if (isBusy() || !hasLoadedCategories()) return
        mutableUiState.update { it.copy(editor = CategoryEditorState(), operationError = null) }
    }

    fun startRename(id: Long) {
        if (isBusy() || !hasLoadedCategories()) return
        val category = uiState.value.categories.firstOrNull { it.id == id && !it.isSystem } ?: return
        mutableUiState.update {
            it.copy(editor = CategoryEditorState(categoryId = id, nameInput = category.name), operationError = null)
        }
    }

    fun changeName(value: String) {
        mutableUiState.update { state ->
            val editor = state.editor ?: return@update state
            if (editor.isSaving) state else state.copy(
                editor = editor.copy(nameInput = value, nameError = null, saveError = null),
            )
        }
    }

    fun dismissEditor() {
        mutableUiState.update { state ->
            if (state.editor?.isSaving == true) state else state.copy(editor = null)
        }
    }

    fun saveEditor() {
        val state = uiState.value
        val editor = state.editor ?: return
        if (isBusy() || !hasLoadedCategories() || editor.isSaving) return
        val existing = editor.categoryId?.let { id -> state.categories.firstOrNull { it.id == id } }
        if (editor.categoryId != null && (existing == null || existing.isSystem)) {
            mutableUiState.update { it.copy(editor = null, operationError = "Category could not be found.") }
            return
        }
        val validation = CategoryNameValidator.validate(editor.nameInput, state.categories, editor.categoryId)
        if (validation.error != null) {
            mutableUiState.update { it.copy(editor = editor.copy(nameError = validation.error)) }
            return
        }
        val name = validation.name ?: return
        mutableUiState.update { it.copy(editor = editor.copy(isSaving = true, nameError = null, saveError = null)) }
        viewModelScope.launch {
            try {
                if (existing == null) {
                    repository.insert(Category(id = 0, name = name, isSystem = false, isActive = true))
                } else if (repository.update(existing.copy(name = name)) == 0) {
                    throw IllegalStateException("Category no longer exists")
                }
                mutableUiState.update { it.copy(editor = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { current ->
                    current.copy(editor = current.editor?.copy(
                        isSaving = false,
                        saveError = "Could not save the category. Please try again.",
                    ))
                }
            }
        }
    }

    fun toggleActive(id: Long) {
        val state = uiState.value
        if (isBusy() || !hasLoadedCategories()) return
        val category = state.categories.firstOrNull { it.id == id } ?: return
        mutableUiState.update { it.copy(busyCategoryId = id, operationError = null) }
        viewModelScope.launch {
            try {
                if (repository.update(category.copy(isActive = !category.isActive)) == 0) {
                    throw IllegalStateException("Category no longer exists")
                }
                mutableUiState.update { it.copy(busyCategoryId = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(busyCategoryId = null, operationError = "Could not change the category. Please try again.")
                }
            }
        }
    }

    private fun isBusy(): Boolean = uiState.value.busyCategoryId != null || uiState.value.editor?.isSaving == true

    private fun hasLoadedCategories(): Boolean = !uiState.value.isLoading && uiState.value.loadError == null
}
