package com.spendly.ui.settings.categories

import com.spendly.domain.model.Category
import java.util.Locale

data class CategoryNameValidation(val name: String?, val error: String?)

object CategoryNameValidator {
    fun validate(input: String, categories: List<Category>, editingId: Long? = null): CategoryNameValidation {
        val name = input.trim()
        if (name.isEmpty()) return CategoryNameValidation(null, "Enter a category name.")
        val duplicate = categories.any { category ->
            category.id != editingId && category.name.trim().lowercase(Locale.ROOT) == name.lowercase(Locale.ROOT)
        }
        if (duplicate) return CategoryNameValidation(null, "A category with this name already exists.")
        return CategoryNameValidation(name, null)
    }
}
