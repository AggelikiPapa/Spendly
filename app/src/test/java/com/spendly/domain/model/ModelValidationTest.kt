package com.spendly.domain.model

import java.time.YearMonth
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelValidationTest {
    @Test
    fun monthlyBudgetRejectsNegativeLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            MonthlyBudget(YearMonth.of(2026, 9), Money(-1, "EUR"))
        }
    }

    @Test
    fun categoryRejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(id = 1, name = "  ", isSystem = true, isActive = true)
        }
    }

    @Test
    fun merchantCategoryRuleRejectsBlankPattern() {
        assertThrows(IllegalArgumentException::class.java) {
            MerchantCategoryRule(id = 1, merchantPattern = "  ", categoryId = 2)
        }
    }
}
