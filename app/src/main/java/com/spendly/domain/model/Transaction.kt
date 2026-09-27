package com.spendly.domain.model

import java.time.Instant

data class Transaction(
    val id: Long,
    val amount: Money,
    val type: TransactionType,
    val merchant: String?,
    val description: String?,
    val categoryId: Long?,
    val occurredAt: Instant,
    val source: TransactionSource,
    val importStatus: ImportStatus,
    val externalReference: String?,
    val rawSourceText: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
