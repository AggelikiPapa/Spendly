package com.spendly.domain.repository

import com.spendly.domain.model.MerchantCategoryRule
import kotlinx.coroutines.flow.Flow

interface MerchantCategoryRuleRepository {
    suspend fun insert(rule: MerchantCategoryRule): Long
    suspend fun update(rule: MerchantCategoryRule): Int
    suspend fun deleteById(id: Long): Int
    suspend fun getById(id: Long): MerchantCategoryRule?
    fun observeAll(): Flow<List<MerchantCategoryRule>>
}
