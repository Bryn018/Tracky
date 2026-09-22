package com.tracky.app.ui.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.tracky.app.ui.screens.analytics.AnalyticsScreen
import com.tracky.app.ui.screens.analytics.AnalyticsViewModel
import com.tracky.app.ui.screens.budget.BudgetScreen
import com.tracky.app.ui.screens.home.HomeScreen
import com.tracky.app.ui.screens.home.HomeViewModel
import com.tracky.app.ui.screens.settings.SettingsScreen
import com.tracky.app.ui.screens.settings.SettingsViewModel
import com.tracky.app.ui.screens.transactions.TransactionsScreen
import com.tracky.app.ui.screens.transactions.TransactionsViewModel
import com.tracky.app.ui.screens.transactions.TransactionDetailScreen

sealed class Screen(val route: String) {
    data object HOME : Screen("home")
    data object TRANSACTIONS : Screen("transactions")
    data object ANALYTICS : Screen("analytics")
    data object SETTINGS : Screen("settings")
    data object BUDGET : Screen("budget")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.HOME.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.TRANSACTIONS.route, "Transactions", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    BottomNavItem(Screen.ANALYTICS.route, "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    BottomNavItem(Screen.SETTINGS.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    hasSmsPermission: Boolean,
    onRequestPermission: () -> Unit,
    exportLauncher: (Intent) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.HOME.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(350))
            }
        ) {
            composable(Screen.HOME.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { navController.navigate(Screen.TRANSACTIONS.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.ANALYTICS.route) },
                    onNavigateToSettings = { navController.navigate(Screen.SETTINGS.route) },
                    onRequestPermission = onRequestPermission,
                    hasPermission = hasSmsPermission
                )
            }

            composable(Screen.TRANSACTIONS.route) {
                val viewModel: TransactionsViewModel = hiltViewModel()
                TransactionsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onTransactionClick = { transactionId ->
                        navController.navigate("transaction_detail/$transactionId")
                    }
                )
            }

            composable(
                route = "transaction_detail/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                TransactionDetailScreen(
                    transactionId = transactionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ANALYTICS.route) {
                val viewModel: AnalyticsViewModel = hiltViewModel()
                AnalyticsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SETTINGS.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBudgets = { navController.navigate(Screen.BUDGET.route) },
                    exportLauncher = exportLauncher
                )
            }

            composable(Screen.BUDGET.route) {
                BudgetScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}