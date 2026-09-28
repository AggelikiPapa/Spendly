package com.spendly.wallet.importer

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.TransactionRepository
import com.spendly.wallet.capture.CapturedWalletNotification
import com.spendly.wallet.parser.GoogleWalletNotificationParser
import com.spendly.wallet.parser.WalletParseResult
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException

enum class WalletImportOutcome {
    IMPORTED,
    STORED_FOR_REVIEW,
    IGNORED_NOT_PURCHASE,
    SKIPPED_INSUFFICIENT_DATA,
    FAILED,
}

data class WalletImportResult(
    val outcome: WalletImportOutcome,
    val parseResult: WalletParseResult?,
)

/** Maps parsed Wallet purchases to the existing transaction repository without deduplication. */
class WalletTransactionImportCoordinator(
    private val transactions: TransactionRepository,
    private val parser: GoogleWalletNotificationParser = GoogleWalletNotificationParser(),
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun import(notification: CapturedWalletNotification): WalletImportResult {
        var parsed: WalletParseResult? = null
        return try {
            when (val result = parser.parse(notification).also { parsed = it }) {
                is WalletParseResult.Success -> {
                    val purchase = result.purchase
                    transactions.insert(
                        transaction(
                            notification = notification,
                            amount = purchase.amount,
                            merchant = purchase.merchant,
                            occurredAt = purchase.occurredAt,
                            importStatus = ImportStatus.CONFIRMED,
                        ),
                    )
                    WalletImportOutcome.IMPORTED
                }
                is WalletParseResult.NeedsReview -> {
                    val amount = result.amount ?: return WalletImportResult(
                        WalletImportOutcome.SKIPPED_INSUFFICIENT_DATA,
                        result,
                    )
                    transactions.insert(
                        transaction(
                            notification = notification,
                            amount = amount,
                            merchant = result.merchant,
                            occurredAt = result.occurredAt,
                            importStatus = ImportStatus.NEEDS_REVIEW,
                        ),
                    )
                    WalletImportOutcome.STORED_FOR_REVIEW
                }
                WalletParseResult.NotPurchase -> WalletImportOutcome.IGNORED_NOT_PURCHASE
            }.let { WalletImportResult(it, parsed) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            WalletImportResult(WalletImportOutcome.FAILED, parsed)
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
            externalReference = notification.notificationKey,
            rawSourceText = notification.text,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
