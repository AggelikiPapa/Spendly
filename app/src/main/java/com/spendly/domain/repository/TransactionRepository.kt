package com.spendly.domain.repository

import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction): Int
    suspend fun deleteById(id: Long): Int
    suspend fun getById(id: Long): Transaction?
    suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction?
    suspend fun getBySourceInTimeRange(
        source: TransactionSource,
        startInclusive: Instant,
        endInclusive: Instant,
    ): List<Transaction>
    fun observeAll(): Flow<List<Transaction>>
    fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>>
}
