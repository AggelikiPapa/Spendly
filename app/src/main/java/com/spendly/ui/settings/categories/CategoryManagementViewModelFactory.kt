package com.spendly.ui.settings.categories

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository

fun categoryManagementViewModelFactory(repository: CategoryRepository) = viewModelFactory {
    initializer { CategoryManagementViewModel(repository) }
}
