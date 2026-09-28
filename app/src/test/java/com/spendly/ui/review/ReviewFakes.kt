package com.spendly.ui.review

import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

internal fun reviewTransaction(
    id: Long = 1,
    at: Instant = Instant.parse("2026-09-14T10:30:00Z"),
    status: ImportStatus = ImportStatus.NEEDS_REVIEW,
    source: TransactionSource = TransactionSource.GOOGLE_WALLET,
    merchant: String? = "Market",
    categoryId: Long? = null,
) = Transaction(
    id = id,
    amount = Money(295, "EUR"),
    type = TransactionType.EXPENSE,
    merchant = merchant,
    description = null,
    categoryId = categoryId,
    occurredAt = at,
    source = source,
    importStatus = status,
    externalReference = "notification-key",
    rawSourceText = "€2.95 with Visa ••1234",
    notes = null,
    createdAt = Instant.parse("2026-09-14T11:00:00Z"),
    updatedAt = Instant.parse("2026-09-14T11:00:00Z"),
)

internal class FakeReviewTransactions : TransactionRepository {
    val items = MutableStateFlow<List<Transaction>>(emptyList())
    var updateCalls = 0
    var failUpdate = false
    var failGet = false
    var failObserve = false
    var updateGate: CompletableDeferred<Unit>? = null

    override suspend fun insert(transaction: Transaction): Long = error("Unused")
    override suspend fun update(transaction: Transaction): Int {
        updateCalls++
        updateGate?.await()
        if (failUpdate) error("Simulated update failure")
        val found = items.value.any { it.id == transaction.id }
        if (found) items.value = items.value.map { if (it.id == transaction.id) transaction else it }
        return if (found) 1 else 0
    }
    override suspend fun deleteById(id: Long): Int = error("Unused")
    override suspend fun getById(id: Long): Transaction? {
        if (failGet) error("Simulated load failure")
        return items.value.firstOrNull { it.id == id }
    }
    override suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction? = error("Unused")
    override suspend fun getBySourceInTimeRange(source: TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
    override fun observeAll(): Flow<List<Transaction>> = if (failObserve) flow { throw IllegalStateException("Simulated load failure") } else items
    override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = emptyFlow()
}

internal class FakeReviewCategories : CategoryRepository {
    val items = MutableStateFlow(listOf(Category(1, "Groceries", true, true)))
    override suspend fun insert(category: Category): Long = error("Unused")
    override suspend fun update(category: Category): Int = error("Unused")
    override suspend fun getById(id: Long): Category? = items.value.firstOrNull { it.id == id }
    override fun observeActive(): Flow<List<Category>> = items
    override fun observeAll(): Flow<List<Category>> = items
}
