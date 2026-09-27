package com.spendly.data.repository

import com.spendly.data.local.dao.TransactionDao
import com.spendly.data.local.mapper.toDomain
import com.spendly.data.local.mapper.toEntity
import com.spendly.domain.model.Transaction
import com.spendly.domain.repository.TransactionRepository
import java.time.Instant
import kotlinx.coroutines.flow.map

class RoomTransactionRepository(private val dao: TransactionDao) : TransactionRepository {
    override suspend fun insert(transaction: Transaction) = dao.insert(transaction.toEntity())
    override suspend fun update(transaction: Transaction) = dao.update(transaction.toEntity())
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    override fun observeInRange(startInclusive: Instant, endExclusive: Instant) =
        dao.observeInRange(startInclusive.toEpochMilli(), endExclusive.toEpochMilli())
            .map { rows -> rows.map { it.toDomain() } }
}
