package com.tracky.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tracky.app.ui.components.EmptyState
import com.tracky.app.ui.components.SummaryCard
import com.tracky.app.ui.components.TransactionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    hasPermission: Boolean
) {
    val context = LocalContext.current
    val todaySpending by viewModel.todaySpending.collectAsState()
    val todayIncome by viewModel.todayIncome.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val receivedTransactions by viewModel.receivedTransactions.collectAsState()
    val permissionGranted by viewModel.permissionGranted.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val error by viewModel.error.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.checkPermission(context)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(scanResult) {
        scanResult?.let { count ->
            val message = "Found $count transaction${if (count != 1) "s" else ""}!"
            snackbarHostState.showSnackbar(message)
            viewModel.clearScanResult()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tracky",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Track your mobile money",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (hasPermission) {
                val fabElevation by animateDpAsState(
                    targetValue = if (isScanning) 8.dp else 6.dp,
                    animationSpec = tween(300),
                    label = "fabElevation"
                )
                FloatingActionButton(
                    onClick = { viewModel.requestScan(context) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = fabElevation
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Sms, contentDescription = "Scan SMS")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp // 16.dp standard + 80.dp for nav bar clearance
                )
            ) {
                // SMS Permission prompt
                if (!hasPermission) {
                    item {
                        PermissionPromptCard(
                            onGrantPermission = onRequestPermission
                        )
                    }
                }

                // Manual scan button
                if (hasPermission) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scan SMS History",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Re-scan your recent SMS to find missed transactions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { viewModel.requestScan(context) },
                                    enabled = !isScanning
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Scan")
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Cards
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Spent Today",
                            value = "-Ksh ${String.format("%.2f", todaySpending)}",
                            color = MaterialTheme.colorScheme.error,
                            icon = Icons.Default.ShoppingCart,
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        )
                        SummaryCard(
                            title = "Received Today",
                            value = "+Ksh ${String.format("%.2f", todayIncome)}",
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        )
                    }
                }

                // Subtle divider between summary and income section
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // Recent Income section
                item {
                    Text(
                        text = "Recent Income",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (receivedTransactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No income yet",
                            description = "Income transactions will appear here after scanning SMS messages.",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(
                        receivedTransactions.take(3),
                        key = { it.id },
                        contentType = { "transaction" }
                    ) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            modifier = Modifier
                        )
                    }
                }

                // Subtle divider between income and transactions section
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // Recent Transactions header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(onClick = onNavigateToTransactions) {
                            Text("View All")
                        }
                    }
                }

                // Recent transactions list
                if (recentTransactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No transactions yet",
                            description = "Transactions will appear here after scanning SMS messages.",
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(
                        recentTransactions,
                        key = { it.id },
                        contentType = { "transaction" }
                    ) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            modifier = Modifier
                        )
                    }
                }
            }

            // Subtle loading shimmer when scanning
            AnimatedVisibility(visible = isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun PermissionPromptCard(onGrantPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "SMS Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Tracky needs access to your SMS messages to automatically detect and track your mobile money transactions. No data is sent anywhere — everything stays on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(onClick = onGrantPermission) {
                Text("Grant Permission")
            }
        }
    }
}