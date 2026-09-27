package com.spendly.data.local.mapper

import com.spendly.data.local.entity.CategoryEntity
import com.spendly.data.local.entity.MerchantCategoryRuleEntity
import com.spendly.data.local.entity.MonthlyBudgetEntity
import com.spendly.data.local.entity.TransactionEntity
import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth

internal fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amountMinor = amount.amountMinor,
    currencyCode = amount.currencyCode,
    type = type.name,
    merchant = merchant,
    description = description,
    categoryId = categoryId,
    occurredAt = occurredAt.toEpochMilli(),
    source = source.name,
    importStatus = importStatus.name,
    externalReference = externalReference,
    rawSourceText = rawSourceText,
    notes = notes,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = Money(amountMinor, currencyCode),
    type = TransactionType.valueOf(type),
    merchant = merchant,
    description = description,
    categoryId = categoryId,
    occurredAt = Instant.ofEpochMilli(occurredAt),
    source = TransactionSource.valueOf(source),
    importStatus = ImportStatus.valueOf(importStatus),
    externalReference = externalReference,
    rawSourceText = rawSourceText,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Category.toEntity() = CategoryEntity(id, name, isSystem, isActive)

internal fun CategoryEntity.toDomain() = Category(id, name, isSystem, isActive)

internal fun MonthlyBudget.toEntity() = MonthlyBudgetEntity(
    yearMonth = yearMonth.toString(),
    limitMinor = limit.amountMinor,
    currencyCode = limit.currencyCode,
)

internal fun MonthlyBudgetEntity.toDomain() = MonthlyBudget(
    yearMonth = YearMonth.parse(yearMonth),
    limit = Money(limitMinor, currencyCode),
)

internal fun MerchantCategoryRule.toEntity() =
    MerchantCategoryRuleEntity(id, merchantPattern, categoryId)

internal fun MerchantCategoryRuleEntity.toDomain() =
    MerchantCategoryRule(id, merchantPattern, categoryId)
