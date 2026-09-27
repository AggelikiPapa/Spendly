package com.spendly.domain.model

data class MerchantCategoryRule(
    val id: Long,
    val merchantPattern: String,
    val categoryId: Long,
) {
    init {
        require(merchantPattern.isNotBlank()) { "Merchant pattern must not be blank" }
    }
}
