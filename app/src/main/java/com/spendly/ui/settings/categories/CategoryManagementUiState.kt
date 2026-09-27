package com.spendly.ui.settings.categories

import com.spendly.domain.model.Category

data class CategoryEditorState(
    val categoryId: Long? = null,
    val nameInput: String = "",
    val nameError: String? = null,
    val saveError: String? = null,
    val isSaving: Boolean = false,
)

data class CategoryManagementUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val operationError: String? = null,
    val busyCategoryId: Long? = null,
    val editor: CategoryEditorState? = null,
)
