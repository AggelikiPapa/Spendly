package com.spendly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.spendly.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE source = :source AND externalReference = :externalReference LIMIT 1")
    suspend fun getBySourceAndExternalReference(source: String, externalReference: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE source = :source AND occurredAt >= :startInclusive AND occurredAt <= :endInclusive")
    suspend fun getBySourceInTimeRange(source: String, startInclusive: Long, endInclusive: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt >= :startInclusive AND occurredAt < :endExclusive ORDER BY occurredAt DESC, id DESC")
    fun observeInRange(startInclusive: Long, endExclusive: Long): Flow<List<TransactionEntity>>
}
