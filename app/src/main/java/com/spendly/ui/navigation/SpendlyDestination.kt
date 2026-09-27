package com.spendly.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.spendly.R

enum class SpendlyDestination(
    val route: String,
    @param:StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    Dashboard("dashboard", R.string.dashboard, Icons.Outlined.Dashboard),
    Transactions("transactions", R.string.transactions, Icons.AutoMirrored.Outlined.ReceiptLong),
    Review("review", R.string.review, Icons.Outlined.RateReview),
    Analytics("analytics", R.string.analytics, Icons.Outlined.Analytics),
    Settings("settings", R.string.settings, Icons.Outlined.Settings),

    ;

    companion object {
        val bottomNavigation: List<SpendlyDestination> = listOf(Dashboard, Transactions, Analytics)
    }
}
