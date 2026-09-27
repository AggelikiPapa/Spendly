package com.spendly.data.repository

import com.spendly.data.local.dao.CategoryDao
import com.spendly.data.local.mapper.toDomain
import com.spendly.data.local.mapper.toEntity
import com.spendly.domain.model.Category
import com.spendly.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {
    override suspend fun insert(category: Category) = dao.insert(category.toEntity())
    override suspend fun update(category: Category) = dao.update(category.toEntity())
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override fun observeActive() = dao.observeActive().map { rows -> rows.map { it.toDomain() } }
    override fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
}
