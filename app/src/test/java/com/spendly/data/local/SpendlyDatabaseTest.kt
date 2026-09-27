package com.spendly.data.local

import androidx.room.Room
import com.spendly.data.local.database.DefaultCategorySeeder
import com.spendly.data.local.database.SpendlyDatabase
import com.spendly.data.repository.RoomCategoryRepository
import com.spendly.data.repository.RoomMerchantCategoryRuleRepository
import com.spendly.data.repository.RoomMonthlyBudgetRepository
import com.spendly.data.repository.RoomTransactionRepository
import com.spendly.domain.model.Category
import com.spendly.domain.model.DefaultCategories
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SpendlyDatabaseTest {
    private lateinit var database: SpendlyDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            SpendlyDatabase::class.java,
        ).addCallback(DefaultCategorySeeder).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun transactionRoundTripPreservesMoneyEnumsAndTimestamps() = runBlocking {
        val repository = RoomTransactionRepository(database.transactionDao())
        val original = transaction(
            amount = Money(-1_234, "eur"),
            type = TransactionType.TRANSFER,
            source = TransactionSource.GOOGLE_WALLET,
            importStatus = ImportStatus.NEEDS_REVIEW,
        )

        val id = repository.insert(original)
        val stored = repository.getById(id)

        assertEquals(original.copy(id = id), stored)
        assertEquals("EUR", stored?.amount?.currencyCode)
        assertEquals(original.occurredAt, stored?.occurredAt)
        assertEquals(original.createdAt, stored?.createdAt)
        assertEquals(original.updatedAt, stored?.updatedAt)
    }

    @Test
    fun transactionEnumsRoundTripForEveryDefinedValue() = runBlocking {
        val repository = RoomTransactionRepository(database.transactionDao())
        for (type in TransactionType.entries) {
            val id = repository.insert(transaction(type = type))
            assertEquals(type, repository.getById(id)?.type)
        }
        for (source in TransactionSource.entries) {
            val id = repository.insert(transaction(source = source))
            assertEquals(source, repository.getById(id)?.source)
        }
        for (status in ImportStatus.entries) {
            val id = repository.insert(transaction(importStatus = status))
            assertEquals(status, repository.getById(id)?.importStatus)
        }
    }

    @Test
    fun transactionsAreObservedNewestFirst() = runBlocking {
        val repository = RoomTransactionRepository(database.transactionDao())
        val oldestId = repository.insert(transaction(occurredAt = Instant.parse("2026-09-01T10:00:00Z")))
        val newestId = repository.insert(transaction(occurredAt = Instant.parse("2026-09-03T10:00:00Z")))
        val middleId = repository.insert(transaction(occurredAt = Instant.parse("2026-09-02T10:00:00Z")))

        assertEquals(listOf(newestId, middleId, oldestId), repository.observeAll().first().map { it.id })
    }

    @Test
    fun transactionCanBeUpdatedAndDeleted() = runBlocking {
        val repository = RoomTransactionRepository(database.transactionDao())
        val id = repository.insert(transaction())
        val updated = transaction(id = id, amount = Money(7_500, "USD"), notes = "Corrected")

        assertEquals(1, repository.update(updated))
        assertEquals(updated, repository.getById(id))
        assertEquals(1, repository.deleteById(id))
        assertNull(repository.getById(id))
    }

    @Test
    fun rangeQueryIncludesStartAndExcludesEnd() = runBlocking {
        val repository = RoomTransactionRepository(database.transactionDao())
        val start = Instant.parse("2026-09-01T00:00:00Z")
        val end = Instant.parse("2026-10-01T00:00:00Z")
        repository.insert(transaction(occurredAt = start.minusMillis(1)))
        val includedId = repository.insert(transaction(occurredAt = start))
        repository.insert(transaction(occurredAt = end))

        assertEquals(listOf(includedId), repository.observeInRange(start, end).first().map { it.id })
    }

    @Test
    fun categoryCanBePersistedAndObservedByActiveState() = runBlocking {
        val repository = RoomCategoryRepository(database.categoryDao())
        val id = repository.insert(Category(0, "Custom", isSystem = false, isActive = false))

        assertEquals(Category(id, "Custom", false, false), repository.getById(id))
        assertFalse(repository.observeActive().first().any { it.id == id })
        assertTrue(repository.observeAll().first().any { it.id == id })
        assertEquals(1, repository.update(Category(id, "Custom", false, true)))
        assertTrue(repository.observeActive().first().any { it.id == id })
    }

    @Test
    fun monthlyBudgetIsRetrievedAndReplacedByMonth() = runBlocking {
        val repository = RoomMonthlyBudgetRepository(database.monthlyBudgetDao())
        val month = YearMonth.of(2026, 9)
        val otherMonth = YearMonth.of(2026, 10)
        val first = MonthlyBudget(month, Money(50_000, "EUR"))
        val replacement = MonthlyBudget(month, Money(60_000, "EUR"))

        repository.upsert(first)
        assertEquals(first, repository.getByMonth(month))
        assertEquals(first, repository.observeByMonth(month).first())
        assertNull(repository.getByMonth(otherMonth))
        repository.upsert(replacement)
        assertEquals(replacement, repository.getByMonth(month))
    }

    @Test
    fun merchantCategoryRuleCanBePersistedUpdatedAndDeleted() = runBlocking {
        val categoryId = database.categoryDao().insert(
            com.spendly.data.local.entity.CategoryEntity(name = "Custom", isSystem = false, isActive = true),
        )
        val repository = RoomMerchantCategoryRuleRepository(database.merchantCategoryRuleDao())
        val id = repository.insert(MerchantCategoryRule(0, "Market", categoryId))

        assertEquals(MerchantCategoryRule(id, "Market", categoryId), repository.getById(id))
        assertEquals(1, repository.update(MerchantCategoryRule(id, "Shop", categoryId)))
        assertEquals("Shop", repository.observeAll().first().single().merchantPattern)
        assertEquals(1, repository.deleteById(id))
        assertNull(repository.getById(id))
    }

    @Test
    fun deletingCategoryKeepsTransactionHistoryAndClearsItsCategory() = runBlocking {
        val categoryId = database.categoryDao().insert(
            com.spendly.data.local.entity.CategoryEntity(name = "Custom", isSystem = false, isActive = true),
        )
        val repository = RoomTransactionRepository(database.transactionDao())
        val transactionId = repository.insert(transaction(categoryId = categoryId))

        database.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = ?", arrayOf(categoryId))

        assertEquals(null, repository.getById(transactionId)?.categoryId)
        assertEquals(transactionId, repository.getById(transactionId)?.id)
    }

    @Test
    fun defaultCategoriesAreSeededOnlyWhenDatabaseIsCreated() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "spendly-seed-test.db"
        context.deleteDatabase(name)
        try {
            fun open() = Room.databaseBuilder(context, SpendlyDatabase::class.java, name)
                .addCallback(DefaultCategorySeeder)
                .build()

            val first = open()
            try {
                val categories = first.categoryDao().observeAll().first()
                assertEquals(DefaultCategories.names.toSet(), categories.map { it.name }.toSet())
                assertTrue(categories.all { it.isSystem && it.isActive })
            } finally {
                first.close()
            }
            val second = open()
            try {
                assertEquals(DefaultCategories.names.size, second.categoryDao().observeAll().first().size)
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun transaction(
        id: Long = 0,
        amount: Money = Money(1_234, "EUR"),
        type: TransactionType = TransactionType.EXPENSE,
        source: TransactionSource = TransactionSource.MANUAL,
        importStatus: ImportStatus = ImportStatus.CONFIRMED,
        occurredAt: Instant = Instant.parse("2026-09-15T12:34:56.789Z"),
        categoryId: Long? = null,
        notes: String? = null,
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        merchant = "Merchant",
        description = "Description",
        categoryId = categoryId,
        occurredAt = occurredAt,
        source = source,
        importStatus = importStatus,
        externalReference = "reference",
        rawSourceText = "raw text",
        notes = notes,
        createdAt = Instant.parse("2026-09-15T12:35:00.123Z"),
        updatedAt = Instant.parse("2026-09-15T12:36:00.456Z"),
    )
}
