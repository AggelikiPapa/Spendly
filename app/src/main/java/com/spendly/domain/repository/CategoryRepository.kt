package com.spendly.domain.repository

import com.spendly.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category): Int
    suspend fun getById(id: Long): Category?
    fun observeActive(): Flow<List<Category>>
    fun observeAll(): Flow<List<Category>>
}
