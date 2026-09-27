package com.spendly.domain.model

data class Category(
    val id: Long,
    val name: String,
    val isSystem: Boolean,
    val isActive: Boolean,
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank" }
    }
}
