package com.spendly

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.spendly.ui.dashboard.DashboardViewModel
import com.spendly.ui.dashboard.dashboardViewModelFactory
import com.spendly.ui.navigation.SpendlyDestination
import com.spendly.ui.navigation.SpendlyRoutes
import com.spendly.ui.review.ReviewScreen
import com.spendly.ui.settings.SettingsScreen
import com.spendly.ui.settings.categories.CategoryManagementScreen
import com.spendly.ui.settings.categories.CategoryManagementViewModel
import com.spendly.ui.settings.categories.categoryManagementViewModelFactory
import com.spendly.ui.settings.budget.MonthlyBudgetScreen
import com.spendly.ui.settings.budget.MonthlyBudgetViewModel
import com.spendly.ui.settings.budget.monthlyBudgetViewModelFactory
import com.spendly.ui.settings.wallet.GoogleWalletTrackingScreen
import com.spendly.ui.settings.wallet.GoogleWalletTrackingViewModel
import com.spendly.ui.settings.wallet.googleWalletTrackingViewModelFactory
import com.spendly.ui.transactions.TransactionsScreen
import com.spendly.ui.transactions.TransactionsViewModel
import com.spendly.ui.transactions.transactionsViewModelFactory
import com.spendly.ui.transactions.add.AddTransactionScreen
import com.spendly.ui.transactions.add.AddTransactionViewModel
import com.spendly.ui.transactions.add.addTransactionViewModelFactory
import com.spendly.ui.transactions.edit.EditTransactionScreen
import com.spendly.ui.transactions.edit.EditTransactionViewModel
import com.spendly.ui.transactions.edit.editTransactionViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendlyApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        topBar = {
            when (currentRoute) {
                SpendlyDestination.Dashboard.route -> TopAppBar(
                    title = { Text(stringResource(R.string.dashboard)) },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(SpendlyDestination.Settings.route) { launchSingleTop = true }
                        }) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    },
                )
                SpendlyDestination.Settings.route, SpendlyDestination.Review.route -> TopAppBar(
                    title = {
                        Text(stringResource(
                            if (currentRoute == SpendlyDestination.Settings.route) R.string.settings else R.string.review,
                        ))
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (SpendlyDestination.bottomNavigation.any { it.route == currentRoute }) {
                NavigationBar {
                    SpendlyDestination.bottomNavigation.forEach { destination ->
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
            composable(SpendlyDestination.Dashboard.route) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val viewModel: DashboardViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = dashboardViewModelFactory(
                        application.transactionRepository,
                        application.monthlyBudgetRepository,
                        application.categoryRepository,
                    ),
                )
                DashboardScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding,
                    onConfigureBudget = {
                        navController.navigate(SpendlyRoutes.MonthlyBudget) { launchSingleTop = true }
                    },
                    onSeeAllTransactions = {
                        navController.navigate(SpendlyDestination.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
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
                    onReviewTransactions = {
                        navController.navigate(SpendlyDestination.Review.route) { launchSingleTop = true }
                    },
                )
            }
            composable(SpendlyDestination.Review.route) { ReviewScreen(innerPadding) }
            composable(SpendlyDestination.Analytics.route) { AnalyticsScreen(innerPadding) }
            composable(SpendlyDestination.Settings.route) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val walletViewModel: GoogleWalletTrackingViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = googleWalletTrackingViewModelFactory(application.notificationAccessGateway),
                )
                SettingsScreen(
                    walletViewModel = walletViewModel,
                    contentPadding = innerPadding,
                    onOpenDestination = { destination ->
                        navController.navigate(destination.route) { launchSingleTop = true }
                    },
                )
            }
            composable(SpendlyRoutes.GoogleWalletTracking) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val viewModel: GoogleWalletTrackingViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = googleWalletTrackingViewModelFactory(application.notificationAccessGateway),
                )
                GoogleWalletTrackingScreen(viewModel, innerPadding) { navController.popBackStack() }
            }
            composable(SpendlyRoutes.MonthlyBudget) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val viewModel: MonthlyBudgetViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = monthlyBudgetViewModelFactory(application.monthlyBudgetRepository),
                )
                MonthlyBudgetScreen(viewModel, innerPadding) { navController.popBackStack() }
            }
            composable(SpendlyRoutes.Categories) { entry ->
                val application = LocalContext.current.applicationContext as SpendlyApplication
                val viewModel: CategoryManagementViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = categoryManagementViewModelFactory(application.categoryRepository),
                )
                CategoryManagementScreen(viewModel, innerPadding) { navController.popBackStack() }
            }
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
