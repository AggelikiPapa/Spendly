package com.spendly.data.repository

import com.spendly.data.local.dao.MerchantCategoryRuleDao
import com.spendly.data.local.mapper.toDomain
import com.spendly.data.local.mapper.toEntity
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import kotlinx.coroutines.flow.map

class RoomMerchantCategoryRuleRepository(private val dao: MerchantCategoryRuleDao) :
    MerchantCategoryRuleRepository {
    override suspend fun insert(rule: MerchantCategoryRule) = dao.insert(rule.toEntity())
    override suspend fun update(rule: MerchantCategoryRule) = dao.update(rule.toEntity())
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
}
