package com.spendly.wallet.importer

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.TransactionRepository
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.parser.GoogleWalletNotificationParser
import com.spendly.wallet.parser.WalletParseResult
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class WalletImportOutcome {
    IMPORTED,
    STORED_FOR_REVIEW,
    IGNORED_NOT_PURCHASE,
    SKIPPED_INSUFFICIENT_DATA,
    SKIPPED_DUPLICATE_REFERENCE,
    SKIPPED_DUPLICATE_HEURISTIC,
    FAILED,
}

data class WalletImportResult(
    val outcome: WalletImportOutcome,
    val parseResult: WalletParseResult?,
)

/** Maps parsed Wallet purchases to the repository, serializing duplicate checks with inserts. */
class WalletTransactionImportCoordinator(
    private val transactions: TransactionRepository,
    rules: MerchantCategoryRuleRepository,
    categories: CategoryRepository,
    private val parser: GoogleWalletNotificationParser = GoogleWalletNotificationParser(),
    private val clock: Clock = Clock.systemUTC(),
) {
    private val duplicateDetector = WalletDuplicateDetector(transactions)
    private val categoryMatcher = MerchantCategoryMatcher(rules, categories)
    private val importMutex = Mutex()

    suspend fun import(notification: CapturedWalletNotification): WalletImportResult {
        var parsed: WalletParseResult? = null
        return try {
            when (val result = parser.parse(notification).also { parsed = it }) {
                is WalletParseResult.Success -> {
                    val purchase = result.purchase
                    insertIfUnique(
                        transaction(
                            notification = notification,
                            amount = purchase.amount,
                            merchant = purchase.merchant,
                            occurredAt = purchase.occurredAt,
                            importStatus = ImportStatus.CONFIRMED,
                        ),
                        WalletImportOutcome.IMPORTED,
                    )
                }
                is WalletParseResult.NeedsReview -> {
                    val amount = result.amount ?: return WalletImportResult(
                        WalletImportOutcome.SKIPPED_INSUFFICIENT_DATA,
                        result,
                    )
                    insertIfUnique(
                        transaction(
                            notification = notification,
                            amount = amount,
                            merchant = result.merchant,
                            occurredAt = result.occurredAt,
                            importStatus = ImportStatus.NEEDS_REVIEW,
                        ),
                        WalletImportOutcome.STORED_FOR_REVIEW,
                    )
                }
                WalletParseResult.NotPurchase -> WalletImportOutcome.IGNORED_NOT_PURCHASE
            }.let { WalletImportResult(it, parsed) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            WalletImportResult(WalletImportOutcome.FAILED, parsed)
        }
    }

    private suspend fun insertIfUnique(candidate: Transaction, importedOutcome: WalletImportOutcome): WalletImportOutcome =
        importMutex.withLock {
            when (duplicateDetector.check(candidate)) {
                DuplicateCheckResult.UNIQUE -> {
                    val categoryId = categoryMatcher.categoryIdFor(candidate.merchant)
                    transactions.insert(candidate.copy(categoryId = categoryId))
                    importedOutcome
                }
                DuplicateCheckResult.DUPLICATE_BY_EXTERNAL_REFERENCE -> WalletImportOutcome.SKIPPED_DUPLICATE_REFERENCE
                DuplicateCheckResult.DUPLICATE_BY_HEURISTIC -> WalletImportOutcome.SKIPPED_DUPLICATE_HEURISTIC
            }
        }

    private fun transaction(
        notification: CapturedWalletNotification,
        amount: Money,
        merchant: String?,
        occurredAt: Instant,
        importStatus: ImportStatus,
    ): Transaction {
        val now = clock.instant()
        return Transaction(
            id = 0,
            amount = amount,
            type = TransactionType.EXPENSE,
            merchant = merchant,
            description = null,
            categoryId = null,
            occurredAt = occurredAt,
            source = TransactionSource.GOOGLE_WALLET,
            importStatus = importStatus,
            externalReference = notification.notificationKey?.takeIf { it.isNotBlank() },
            rawSourceText = notification.text,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
