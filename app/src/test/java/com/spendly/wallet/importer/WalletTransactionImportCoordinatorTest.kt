package com.spendly.wallet.importer

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.dashboard.BudgetProgressCalculator
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.capture.SupportedWalletPackages
import com.spendly.wallet.parser.WalletParseResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletTransactionImportCoordinatorTest {
    private val importTime = Instant.parse("2026-09-15T12:00:00Z")
    private val purchaseTime = Instant.parse("2026-09-14T10:30:00Z")
    private val repository = FakeTransactionRepository()
    private val coordinator = WalletTransactionImportCoordinator(
        repository,
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

    @Test fun notPurchaseNeverInserts() = runTest {
        val result = coordinator.import(notification(text = "Your pass is ready"))

        assertEquals(WalletImportOutcome.IGNORED_NOT_PURCHASE, result.outcome)
        assertEquals(WalletParseResult.NotPurchase, result.parseResult)
        assertEquals(0, repository.insertCalls)
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

    @Test fun repeatedDeliveryRemainsUndeduplicatedForApp014() = runTest {
        val capture = notification()
        coordinator.import(capture)
        coordinator.import(capture)

        assertEquals(2, repository.insertCalls)
        assertEquals(2, repository.observeAll().first().size)
    }

    private class FakeTransactionRepository : TransactionRepository {
        val inserted = mutableListOf<Transaction>()
        val rows = MutableStateFlow<List<Transaction>>(emptyList())
        var insertCalls = 0
        var failInsert = false

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
        override fun observeAll(): Flow<List<Transaction>> = rows
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> =
            rows.map { list -> list.filter { it.occurredAt >= startInclusive && it.occurredAt < endExclusive } }
    }
}
