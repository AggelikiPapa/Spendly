package com.spendly.ui.transactions

import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.model.TransactionVisibility
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Uncategorized : CategoryFilter
    data class Specific(val id: Long) : CategoryFilter
}

data class TransactionFilters(
    val searchQuery: String = "",
    val category: CategoryFilter = CategoryFilter.All,
    val type: TransactionType? = null,
    val source: TransactionSource? = null,
) {
    val hasActiveFilters: Boolean get() = searchQuery.isNotBlank() || category != CategoryFilter.All ||
        type != null || source != null
}

/** Pure filtering used after the selected month's repository emission. */
internal object TransactionHistoryFilter {
    private val whitespace = Regex("\\s+")

    fun apply(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId,
        filters: TransactionFilters,
    ): List<Transaction> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        val query = normalize(filters.searchQuery)
        return transactions.asSequence()
            .filter { TransactionVisibility.inHistory(it) && it.occurredAt >= start && it.occurredAt < end }
            .filter { transaction ->
                when (val category = filters.category) {
                    CategoryFilter.All -> true
                    CategoryFilter.Uncategorized -> transaction.categoryId == null
                    is CategoryFilter.Specific -> transaction.categoryId == category.id
                }
            }
            .filter { filters.type == null || it.type == filters.type }
            .filter { filters.source == null || it.source == filters.source }
            .filter { transaction ->
                query.isEmpty() || sequenceOf(transaction.merchant, transaction.description, transaction.notes)
                    .filterNotNull().any { normalize(it).contains(query) }
            }
            .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id })
            .toList()
    }

    fun normalize(text: String): String = text.trim().lowercase(Locale.ROOT).replace(whitespace, " ")
}
