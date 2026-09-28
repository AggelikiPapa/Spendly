package com.spendly

import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class RuleTestRepository : MerchantCategoryRuleRepository {
    val items = MutableStateFlow<List<MerchantCategoryRule>>(emptyList())
    var failInsert = false
    var insertCalls = 0
    override suspend fun insert(rule: MerchantCategoryRule): Long {
        insertCalls++
        if (failInsert) error("Simulated rule failure")
        val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
        items.value += rule.copy(id = id)
        return id
    }
    override suspend fun update(rule: MerchantCategoryRule): Int {
        if (items.value.none { it.id == rule.id }) return 0
        items.value = items.value.map { if (it.id == rule.id) rule else it }
        return 1
    }
    override suspend fun deleteById(id: Long): Int {
        val exists = items.value.any { it.id == id }
        items.value = items.value.filterNot { it.id == id }
        return if (exists) 1 else 0
    }
    override suspend fun getById(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun getAll() = items.value
    override fun observeAll(): Flow<List<MerchantCategoryRule>> = items
}
