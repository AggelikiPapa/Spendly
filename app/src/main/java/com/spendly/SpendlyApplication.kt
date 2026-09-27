package com.spendly

import android.app.Application
import com.spendly.data.local.database.SpendlyDatabaseProvider
import com.spendly.data.repository.RoomCategoryRepository
import com.spendly.data.repository.RoomTransactionRepository
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

class SpendlyApplication : Application() {
    private val database by lazy { SpendlyDatabaseProvider.get(this) }

    val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(database.transactionDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(database.categoryDao())
    }
}
