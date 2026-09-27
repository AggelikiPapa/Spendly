package com.spendly.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spendly.data.local.dao.CategoryDao
import com.spendly.data.local.dao.MerchantCategoryRuleDao
import com.spendly.data.local.dao.MonthlyBudgetDao
import com.spendly.data.local.dao.TransactionDao
import com.spendly.data.local.entity.CategoryEntity
import com.spendly.data.local.entity.MerchantCategoryRuleEntity
import com.spendly.data.local.entity.MonthlyBudgetEntity
import com.spendly.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        MerchantCategoryRuleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun merchantCategoryRuleDao(): MerchantCategoryRuleDao
}
