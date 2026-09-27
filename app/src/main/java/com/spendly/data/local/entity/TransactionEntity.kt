package com.spendly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("occurredAt"), Index("categoryId"), Index("source")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currencyCode: String,
    val type: String,
    val merchant: String?,
    val description: String?,
    val categoryId: Long?,
    val occurredAt: Long,
    val source: String,
    val importStatus: String,
    val externalReference: String?,
    val rawSourceText: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
