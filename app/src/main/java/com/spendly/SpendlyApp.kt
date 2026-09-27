package com.spendly

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendly.ui.analytics.AnalyticsScreen
import com.spendly.ui.dashboard.DashboardScreen
import com.spendly.ui.navigation.SpendlyDestination
import com.spendly.ui.navigation.SpendlyRoutes
import com.spendly.ui.review.ReviewScreen
import com.spendly.ui.settings.SettingsScreen
import com.spendly.ui.transactions.TransactionsScreen
import com.spendly.ui.transactions.TransactionsViewModel
import com.spendly.ui.transactions.transactionsViewModelFactory
import com.spendly.ui.transactions.add.AddTransactionScreen
import com.spendly.ui.transactions.add.AddTransactionViewModel
import com.spendly.ui.transactions.add.addTransactionViewModelFactory
import com.spendly.ui.transactions.edit.EditTransactionScreen
import com.spendly.ui.transactions.edit.EditTransactionViewModel
import com.spendly.ui.transactions.edit.editTransactionViewModelFactory

@Composable
fun SpendlyApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != SpendlyRoutes.AddTransaction && currentRoute != SpendlyRoutes.EditTransaction) {
                NavigationBar {
                    SpendlyDestination.entries.forEach { destination ->
                        val label = stringResource(destination.labelResId)
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SpendlyDestination.Dashboard.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(SpendlyDestination.Dashboard.route) { DashboardScreen(innerPadding) }
            composable(SpendlyDestination.Transactions.route) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val viewModel: TransactionsViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = transactionsViewModelFactory(
                        application.transactionRepository,
                        application.categoryRepository,
                    ),
                )
                TransactionsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onAddTransaction = {
                        navController.navigate(SpendlyRoutes.AddTransaction) { launchSingleTop = true }
                    },
                    onOpenTransaction = { id -> navController.navigate(SpendlyRoutes.editTransaction(id)) },
                )
            }
            composable(SpendlyDestination.Review.route) { ReviewScreen(innerPadding) }
            composable(SpendlyDestination.Analytics.route) { AnalyticsScreen(innerPadding) }
            composable(SpendlyDestination.Settings.route) { SettingsScreen(innerPadding) }
            composable(SpendlyRoutes.AddTransaction) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val viewModel: AddTransactionViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = addTransactionViewModelFactory(
                        application.transactionRepository,
                        application.categoryRepository,
                    ),
                )
                AddTransactionScreen(viewModel, innerPadding) { navController.popBackStack() }
            }
            composable(
                route = SpendlyRoutes.EditTransaction,
                arguments = listOf(navArgument(SpendlyRoutes.TransactionId) { type = NavType.LongType }),
            ) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val id = requireNotNull(entry.arguments?.getLong(SpendlyRoutes.TransactionId))
                val viewModel: EditTransactionViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = editTransactionViewModelFactory(
                        id,
                        application.transactionRepository,
                        application.categoryRepository,
                    ),
                )
                EditTransactionScreen(viewModel, innerPadding) { navController.popBackStack() }
            }
        }
    }
}
