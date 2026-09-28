package com.spendly.wallet.importer

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.TransactionRepository
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletDuplicateDetectorTest {
    private val time = Instant.parse("2026-09-14T12:00:00Z")
    private val repository = FakeTransactions()
    private val detector = WalletDuplicateDetector(repository)

    private fun transaction(
        merchant: String? = "GPK MARKET IKE",
        amount: Money = Money(295, "EUR"),
        occurredAt: Instant = time,
        source: TransactionSource = TransactionSource.GOOGLE_WALLET,
        reference: String? = null,
        status: ImportStatus = ImportStatus.CONFIRMED,
        categoryId: Long? = null,
    ) = Transaction(
        id = 0,
        amount = amount,
        type = TransactionType.EXPENSE,
        merchant = merchant,
        description = null,
        categoryId = categoryId,
        occurredAt = occurredAt,
        source = source,
        importStatus = status,
        externalReference = reference,
        rawSourceText = null,
        notes = null,
        createdAt = time,
        updatedAt = time,
    )

    @Test fun exactReferenceWinsBeforeFallbackEvenWhenDetailsDiffer() = runTest {
        repository.rows += transaction(merchant = "Different", amount = Money(999, "EUR"), occurredAt = time.minusSeconds(600), reference = "key")

        assertEquals(
            DuplicateCheckResult.DUPLICATE_BY_EXTERNAL_REFERENCE,
            detector.check(transaction(reference = "key")),
        )
        assertEquals(0, repository.rangeQueries)
    }

    @Test fun differentReferenceIsNotAutomaticallyDuplicateButStillUsesFallback() = runTest {
        repository.rows += transaction(reference = "first", occurredAt = time.minusSeconds(300))
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction(reference = "second")))
        assertEquals(1, repository.rangeQueries)

        repository.rows += transaction(reference = "first", occurredAt = time.minusSeconds(80))
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction(reference = "second")))
    }

    @Test fun missingAndBlankReferencesUseNormalizedMerchantHeuristic() = runTest {
        repository.rows += transaction(merchant = "  GPK   MARKET IKE  ", occurredAt = time.minusSeconds(80))
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction(merchant = "gpk market ike")))
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction(reference = "  ")))
        assertEquals(0, repository.referenceQueries)
    }

    @Test fun fallbackWindowIsInclusiveAtExactlyTwoMinutes() = runTest {
        repository.rows += transaction(occurredAt = time.plus(WalletDuplicateDetector.DUPLICATE_WINDOW))
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction()))
        repository.rows.clear()
        repository.rows += transaction(occurredAt = time.minus(WalletDuplicateDetector.DUPLICATE_WINDOW))
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction()))
        repository.rows.clear()
        repository.rows += transaction(occurredAt = time.plus(WalletDuplicateDetector.DUPLICATE_WINDOW).plusMillis(1))
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction()))
        repository.rows.clear()
        repository.rows += transaction(occurredAt = time.minusSeconds(300))
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction()))
    }

    @Test fun amountCurrencyAndMerchantMustAllMatchExactlyAfterNarrowNormalization() = runTest {
        repository.rows += transaction()
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction(amount = Money(296, "EUR"))))
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction(amount = Money(295, "USD"))))
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction(merchant = "GPK MARKET II")))
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction(merchant = null)))
    }

    @Test fun manualTransactionNeverBlocksWalletImport() = runTest {
        repository.rows += transaction(source = TransactionSource.MANUAL, reference = "key")
        assertEquals(DuplicateCheckResult.UNIQUE, detector.check(transaction(reference = "key")))
    }

    @Test fun reviewTransactionBlocksRedeliveryAndCategoryIsIgnored() = runTest {
        repository.rows += transaction(status = ImportStatus.NEEDS_REVIEW, categoryId = 42, reference = "key")
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_EXTERNAL_REFERENCE, detector.check(transaction(reference = "key")))
        assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction()))
    }

    @Test fun merchantNormalizationUsesRootLocale() = runTest {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            repository.rows += transaction(merchant = "IKEA")
            assertEquals(DuplicateCheckResult.DUPLICATE_BY_HEURISTIC, detector.check(transaction(merchant = "ikea")))
        } finally {
            Locale.setDefault(previous)
        }
    }

    private class FakeTransactions : TransactionRepository {
        val rows = mutableListOf<Transaction>()
        var referenceQueries = 0
        var rangeQueries = 0

        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? = error("Unused")
        override suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction? {
            referenceQueries++
            return rows.firstOrNull { it.source == source && it.externalReference == externalReference }
        }
        override suspend fun getBySourceInTimeRange(
            source: TransactionSource,
            startInclusive: Instant,
            endInclusive: Instant,
        ): List<Transaction> {
            rangeQueries++
            return rows.filter { it.source == source && it.occurredAt >= startInclusive && it.occurredAt <= endInclusive }
        }
        override fun observeAll(): Flow<List<Transaction>> = emptyFlow()
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = emptyFlow()
    }
}
