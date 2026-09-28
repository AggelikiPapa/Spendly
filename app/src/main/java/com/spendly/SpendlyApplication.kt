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
import com.spendly.budget.alerts.AndroidBudgetAlertPermission
import com.spendly.budget.alerts.AndroidBudgetAlertSender
import com.spendly.budget.alerts.BudgetAlertCoordinator
import com.spendly.budget.alerts.SharedPreferencesBudgetAlertStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SpendlyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { SpendlyDatabaseProvider.get(this) }

    val budgetAlertStore by lazy { SharedPreferencesBudgetAlertStore(this) }
    val budgetAlertPermission by lazy { AndroidBudgetAlertPermission(this) }
    private val budgetAlertCoordinator by lazy {
        BudgetAlertCoordinator(transactionRepository, monthlyBudgetRepository, budgetAlertStore,
            budgetAlertPermission, AndroidBudgetAlertSender(this))
    }

    override fun onCreate() {
        super.onCreate()
        budgetAlertCoordinator.start(applicationScope)
    }

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
