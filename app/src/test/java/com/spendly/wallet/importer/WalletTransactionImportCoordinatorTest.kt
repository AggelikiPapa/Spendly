package com.spendly.wallet.importer

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Category
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.TransactionRepository
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import com.spendly.ui.dashboard.BudgetProgressCalculator
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.SupportedWalletPackages
import com.spendly.wallet.parser.WalletParseResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletTransactionImportCoordinatorTest {
    private val importTime = Instant.parse("2026-09-15T12:00:00Z")
    private val purchaseTime = Instant.parse("2026-09-14T10:30:00Z")
    private val repository = FakeTransactionRepository()
    private val rules = FakeRules()
    private val categories = FakeCategories()
    private val coordinator = WalletTransactionImportCoordinator(
        repository,
        rules,
        categories,
        clock = Clock.fixed(importTime, ZoneOffset.UTC),
    )

    private fun notification(
        title: String? = "GPK MARKET IKE",
        text: String? = "€2.95 with Ticket Restaurant® ••5311",
        key: String? = "wallet-notification-key",
    ) = CapturedWalletNotification(
        packageName = SupportedWalletPackages.GOOGLE_WALLET,
        notificationKey = key,
        postedAtEpochMillis = purchaseTime.toEpochMilli(),
        title = title,
        text = text,
        subText = null,
        bigText = null,
    )

    @Test fun successfulPurchaseInsertsOneConfirmedUncategorizedExpense() = runTest {
        val capture = notification()

        val result = coordinator.import(capture)

        assertEquals(WalletImportOutcome.IMPORTED, result.outcome)
        assertTrue(result.parseResult is WalletParseResult.Success)
        assertEquals(1, repository.insertCalls)
        val saved = repository.inserted.single()
        assertEquals(0L, saved.id)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(TransactionSource.GOOGLE_WALLET, saved.source)
        assertEquals(ImportStatus.CONFIRMED, saved.importStatus)
        assertNull(saved.categoryId)
        assertEquals("GPK MARKET IKE", saved.merchant)
        assertEquals(Money(295, "EUR"), saved.amount)
        assertEquals(purchaseTime, saved.occurredAt)
        assertEquals(importTime, saved.createdAt)
        assertEquals(importTime, saved.updatedAt)
        assertEquals(capture.notificationKey, saved.externalReference)
        assertEquals(capture.text, saved.rawSourceText)
        assertNull(saved.description)
        assertNull(saved.notes)
    }

    @Test fun insertedExpenseAppearsInRepositoryFlowAndMonthlySpending() = runTest {
        coordinator.import(notification())

        val observed = repository.observeAll().first().single()
        assertEquals(1L, observed.id)
        assertTrue(observed.type.countsTowardMonthlyBudget)
        val month = YearMonth.of(2026, 9)
        val progress = BudgetProgressCalculator.calculate(
            MonthlyBudget(month, Money(10_000, "EUR")),
            repository.observeAll().first(),
            month,
            LocalDate.of(2026, 9, 15),
            ZoneOffset.UTC,
        )
        assertEquals(Money(295, "EUR"), progress.spent)
    }

    @Test fun confirmedImportUsesExistingRuleBeforeInsertion() = runTest {
        rules.items = listOf(MerchantCategoryRule(1, "  gpk   market ike ", 7))
        categories.items = listOf(Category(7, "Groceries", false, true))

        assertEquals(WalletImportOutcome.IMPORTED, coordinator.import(notification()).outcome)
        assertEquals(7L, repository.inserted.single().categoryId)
        assertEquals(ImportStatus.CONFIRMED, repository.inserted.single().importStatus)
    }

    @Test fun reviewableImportCanReceiveCategoryWithoutChangingStatus() = runTest {
        rules.items = listOf(MerchantCategoryRule(1, "GPK MARKET IKE", 7))
        categories.items = listOf(Category(7, "Groceries", false, true))

        assertEquals(WalletImportOutcome.STORED_FOR_REVIEW, coordinator.import(notification(text = "€2.95 with Visa ••123")).outcome)
        assertEquals(7L, repository.inserted.single().categoryId)
        assertEquals(ImportStatus.NEEDS_REVIEW, repository.inserted.single().importStatus)
    }

    @Test fun notPurchaseNeverInserts() = runTest {
        val result = coordinator.import(notification(text = "Your pass is ready"))

        assertEquals(WalletImportOutcome.IGNORED_NOT_PURCHASE, result.outcome)
        assertEquals(WalletParseResult.NotPurchase, result.parseResult)
        assertEquals(0, repository.insertCalls)
        assertEquals(0, repository.referenceLookupCalls)
        assertEquals(0, repository.rangeLookupCalls)
    }

    @Test fun partialPurchaseWithAmountIsStoredForLaterReview() = runTest {
        val result = coordinator.import(notification(title = " ", key = null))

        assertEquals(WalletImportOutcome.STORED_FOR_REVIEW, result.outcome)
        val saved = repository.inserted.single()
        assertEquals(ImportStatus.NEEDS_REVIEW, saved.importStatus)
        assertEquals(TransactionSource.GOOGLE_WALLET, saved.source)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(Money(295, "EUR"), saved.amount)
        assertNull(saved.merchant)
        assertNull(saved.categoryId)
        assertNull(saved.externalReference)
    }

    @Test fun malformedCardDetailsWithValidAmountRemainReviewable() = runTest {
        val result = coordinator.import(notification(text = "€2.95 with Visa ••123"))

        assertEquals(WalletImportOutcome.STORED_FOR_REVIEW, result.outcome)
        assertEquals(ImportStatus.NEEDS_REVIEW, repository.inserted.single().importStatus)
        assertEquals("GPK MARKET IKE", repository.inserted.single().merchant)
    }

    @Test fun invalidOrUnsupportedAmountNeverGetsAPlaceholderTransaction() = runTest {
        assertEquals(
            WalletImportOutcome.SKIPPED_INSUFFICIENT_DATA,
            coordinator.import(notification(text = "€2.xx with Visa ••1234")).outcome,
        )
        assertEquals(
            WalletImportOutcome.SKIPPED_INSUFFICIENT_DATA,
            coordinator.import(notification(text = "$2.95 with Visa ••1234")).outcome,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test fun insertFailureReturnsFailureWithoutClaimingAnImport() = runTest {
        repository.failInsert = true

        val result = coordinator.import(notification())

        assertEquals(WalletImportOutcome.FAILED, result.outcome)
        assertEquals(1, repository.insertCalls)
        assertTrue(repository.inserted.isEmpty())
    }

    @Test fun repeatedDeliveryIsSkippedByReference() = runTest {
        rules.items = listOf(MerchantCategoryRule(1, "GPK MARKET IKE", 7))
        categories.items = listOf(Category(7, "Groceries", false, true))
        val capture = notification()
        assertEquals(WalletImportOutcome.IMPORTED, coordinator.import(capture).outcome)
        assertEquals(WalletImportOutcome.SKIPPED_DUPLICATE_REFERENCE, coordinator.import(capture).outcome)

        assertEquals(1, repository.insertCalls)
        assertEquals(1, repository.observeAll().first().size)
        assertEquals(1, rules.getAllCalls)
    }

    @Test fun reviewableRedeliveryIsAlsoSkipped() = runTest {
        val capture = notification(title = " ")
        assertEquals(WalletImportOutcome.STORED_FOR_REVIEW, coordinator.import(capture).outcome)
        assertEquals(WalletImportOutcome.SKIPPED_DUPLICATE_REFERENCE, coordinator.import(capture).outcome)
        assertEquals(1, repository.insertCalls)
    }

    @Test fun missingReferenceUsesHeuristicInImportPipeline() = runTest {
        val capture = notification(key = null)
        assertEquals(WalletImportOutcome.IMPORTED, coordinator.import(capture).outcome)
        assertEquals(WalletImportOutcome.SKIPPED_DUPLICATE_HEURISTIC, coordinator.import(capture).outcome)
        assertEquals(1, repository.insertCalls)
    }

    @Test fun lookupFailureDoesNotInsertOrCrash() = runTest {
        repository.failLookup = true
        assertEquals(WalletImportOutcome.FAILED, coordinator.import(notification()).outcome)
        assertEquals(0, repository.insertCalls)
    }

    @Test fun ruleRepositoryFailureDoesNotInsertOrCrash() = runTest {
        rules.failRead = true
        assertEquals(WalletImportOutcome.FAILED, coordinator.import(notification()).outcome)
        assertEquals(0, repository.insertCalls)
    }

    @Test fun simultaneousRedeliveryInsertsAtMostOnce() = runTest {
        repository.suspendDuringLookup = true
        val capture = notification()
        val outcomes = coroutineScope {
            listOf(
                async { coordinator.import(capture).outcome },
                async { coordinator.import(capture).outcome },
            ).map { it.await() }
        }
        assertEquals(1, outcomes.count { it == WalletImportOutcome.IMPORTED })
        assertEquals(1, outcomes.count { it == WalletImportOutcome.SKIPPED_DUPLICATE_REFERENCE })
        assertEquals(1, repository.insertCalls)
    }

    private class FakeTransactionRepository : TransactionRepository {
        val inserted = mutableListOf<Transaction>()
        val rows = MutableStateFlow<List<Transaction>>(emptyList())
        var insertCalls = 0
        var failInsert = false
        var failLookup = false
        var suspendDuringLookup = false
        var referenceLookupCalls = 0
        var rangeLookupCalls = 0

        override suspend fun insert(transaction: Transaction): Long {
            insertCalls++
            if (failInsert) error("Simulated repository failure")
            inserted += transaction
            val id = insertCalls.toLong()
            rows.value = rows.value + transaction.copy(id = id)
            return id
        }

        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? = rows.value.firstOrNull { it.id == id }
        override suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction? {
            referenceLookupCalls++
            if (suspendDuringLookup) yield()
            if (failLookup) error("Simulated lookup failure")
            return rows.value.firstOrNull { it.source == source && it.externalReference == externalReference }
        }
        override suspend fun getBySourceInTimeRange(
            source: TransactionSource,
            startInclusive: Instant,
            endInclusive: Instant,
        ): List<Transaction> {
            rangeLookupCalls++
            if (suspendDuringLookup) yield()
            if (failLookup) error("Simulated lookup failure")
            return rows.value.filter { it.source == source && it.occurredAt >= startInclusive && it.occurredAt <= endInclusive }
        }
        override fun observeAll(): Flow<List<Transaction>> = rows
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> =
            rows.map { list -> list.filter { it.occurredAt >= startInclusive && it.occurredAt < endExclusive } }
    }

    private class FakeRules : MerchantCategoryRuleRepository {
        var items = emptyList<MerchantCategoryRule>()
        var getAllCalls = 0
        var failRead = false
        override suspend fun getAll(): List<MerchantCategoryRule> {
            getAllCalls++
            if (failRead) error("Simulated rule lookup failure")
            return items
        }
        override suspend fun insert(rule: MerchantCategoryRule): Long = error("Unused")
        override suspend fun update(rule: MerchantCategoryRule): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): MerchantCategoryRule? = error("Unused")
        override fun observeAll(): Flow<List<MerchantCategoryRule>> = emptyFlow()
    }

    private class FakeCategories : CategoryRepository {
        var items = emptyList<Category>()
        override suspend fun getById(id: Long): Category? = items.firstOrNull { it.id == id }
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override fun observeAll(): Flow<List<Category>> = emptyFlow()
        override fun observeActive(): Flow<List<Category>> = emptyFlow()
    }
}
