package com.spendly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.spendly.data.local.entity.MerchantCategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryRuleDao {
    @Insert
    suspend fun insert(rule: MerchantCategoryRuleEntity): Long

    @Update
    suspend fun update(rule: MerchantCategoryRuleEntity): Int

    @Query("DELETE FROM merchant_category_rules WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM merchant_category_rules WHERE id = :id")
    suspend fun getById(id: Long): MerchantCategoryRuleEntity?

    @Query("SELECT * FROM merchant_category_rules ORDER BY id")
    suspend fun getAll(): List<MerchantCategoryRuleEntity>

    @Query("SELECT * FROM merchant_category_rules ORDER BY id")
    fun observeAll(): Flow<List<MerchantCategoryRuleEntity>>
}
