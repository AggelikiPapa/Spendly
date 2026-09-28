package com.spendly

import android.app.Application
import com.spendly.data.local.database.SpendlyDatabaseProvider
import com.spendly.data.repository.RoomCategoryRepository
import com.spendly.data.repository.RoomMonthlyBudgetRepository
import com.spendly.data.repository.RoomMerchantCategoryRuleRepository
import com.spendly.data.repository.RoomTransactionRepository
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.wallet.listener.NotificationAccessGateway
import com.spendly.wallet.listener.NotificationAccessManager
import com.spendly.wallet.importer.SaveMerchantCategoryRuleUseCase

class SpendlyApplication : Application() {
    private val database by lazy { SpendlyDatabaseProvider.get(this) }

    val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(database.transactionDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(database.categoryDao())
    }

    val merchantCategoryRuleRepository: MerchantCategoryRuleRepository by lazy {
        RoomMerchantCategoryRuleRepository(database.merchantCategoryRuleDao())
    }

    val saveMerchantCategoryRuleUseCase by lazy {
        SaveMerchantCategoryRuleUseCase(merchantCategoryRuleRepository, categoryRepository)
    }

    val monthlyBudgetRepository: MonthlyBudgetRepository by lazy {
        RoomMonthlyBudgetRepository(database.monthlyBudgetDao())
    }

    val notificationAccessGateway: NotificationAccessGateway by lazy {
        NotificationAccessManager(this)
    }
}
