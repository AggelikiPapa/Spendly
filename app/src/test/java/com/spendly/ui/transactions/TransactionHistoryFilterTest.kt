package com.spendly.ui.transactions

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHistoryFilterTest {
    private val month = YearMonth.of(2026, 9)
    private val zone = ZoneId.of("Europe/Athens")

    @Test fun searchNormalizesCaseAndWhitespaceAcrossMerchantDescriptionAndNotes() {
        val rows = listOf(
            transaction(1, merchant = "WOLT   Market"),
            transaction(2, merchant = null, description = "Wolt order"),
            transaction(3, merchant = null, notes = "Paid at WOLT"),
            transaction(4, merchant = "Shop"),
        )
        assertEquals(listOf(3L, 2L, 1L), search(rows, "  WoLt  ").map { it.id })
        assertEquals(listOf(1L), search(rows, " wolt    market ").map { it.id })
        assertTrue(search(rows, "not here").isEmpty())
        assertEquals("wolt market", TransactionHistoryFilter.normalize("  WOLT    Market  "))
    }

    @Test fun categoryTypeAndSourceFiltersCombineWithAndLogic() {
        val rows = listOf(
            transaction(1, merchant = "Wolt", categoryId = 1, source = TransactionSource.GOOGLE_WALLET),
            transaction(2, merchant = "Wolt", categoryId = 1, source = TransactionSource.MANUAL),
            transaction(3, merchant = "Wolt", categoryId = 2, source = TransactionSource.GOOGLE_WALLET),
            transaction(4, merchant = "Wolt", categoryId = 1, source = TransactionSource.GOOGLE_WALLET,
                type = TransactionType.INCOME),
            transaction(5, merchant = "Wolt", categoryId = null, source = TransactionSource.MANUAL),
        )
        val filters = TransactionFilters("WOLT", CategoryFilter.Specific(1), TransactionType.EXPENSE,
            TransactionSource.GOOGLE_WALLET)
        assertEquals(listOf(1L), apply(rows, filters).map { it.id })
        assertEquals(listOf(5L), apply(rows, TransactionFilters(category = CategoryFilter.Uncategorized)).map { it.id })
        assertEquals(listOf(4L), apply(rows, TransactionFilters(type = TransactionType.INCOME)).map { it.id })
        assertEquals(listOf(5L, 2L), apply(rows, TransactionFilters(source = TransactionSource.MANUAL,
            category = CategoryFilter.All)).map { it.id })
        assertEquals(listOf(4L, 3L, 1L), apply(rows,
            TransactionFilters(source = TransactionSource.GOOGLE_WALLET)).map { it.id })
    }

    @Test fun allTypesAreAvailableAndClearFiltersRestoresMonthList() {
        val rows = listOf(
            transaction(1, type = TransactionType.EXPENSE),
            transaction(2, type = TransactionType.INCOME),
            transaction(3, type = TransactionType.TRANSFER),
        )
        assertEquals(listOf(1L), apply(rows, TransactionFilters(type = TransactionType.EXPENSE)).map { it.id })
        assertEquals(listOf(2L), apply(rows, TransactionFilters(type = TransactionType.INCOME)).map { it.id })
        assertEquals(listOf(3L), apply(rows, TransactionFilters(type = TransactionType.TRANSFER)).map { it.id })
        assertEquals(listOf(3L, 2L, 1L), apply(rows, TransactionFilters()).map { it.id })
    }

    @Test fun halfOpenLocalMonthAndVisibilityPreserveNewestFirstOrder() {
        val rows = listOf(
            transaction(1, at = "2026-08-31T22:30:00Z"), // Sep 1 locally
            transaction(2, at = "2026-09-20T10:00:00Z"),
            transaction(3, at = "2026-09-20T10:00:00Z"),
            transaction(4, at = "2026-09-30T21:30:00Z"), // Oct 1 locally
            transaction(5, at = "2026-08-30T10:00:00Z"),
            transaction(6, status = ImportStatus.NEEDS_REVIEW),
            transaction(7, status = ImportStatus.IGNORED),
        )
        assertEquals(listOf(3L, 2L, 1L), apply(rows, TransactionFilters()).map { it.id })
    }

    private fun search(rows: List<Transaction>, query: String) = apply(rows, TransactionFilters(searchQuery = query))
    private fun apply(rows: List<Transaction>, filters: TransactionFilters) =
        TransactionHistoryFilter.apply(rows, month, zone, filters)

    private fun transaction(
        id: Long,
        merchant: String? = "Shop",
        description: String? = null,
        notes: String? = null,
        categoryId: Long? = 1,
        type: TransactionType = TransactionType.EXPENSE,
        source: TransactionSource = TransactionSource.MANUAL,
        status: ImportStatus = ImportStatus.CONFIRMED,
        at: String = "2026-09-10T10:00:00Z",
    ): Transaction {
        val instant = Instant.parse(at)
        return Transaction(id, Money(1_240, "EUR"), type, merchant, description, categoryId,
            instant, source, status, null, null, notes, instant, instant)
    }
}
