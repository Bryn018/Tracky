package com.tracky.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val dailySummary by viewModel.dailySummary.collectAsState()
    val weeklyTotal by viewModel.weeklyTotal.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val topChannel by viewModel.topChannel.collectAsState()
    val averageDaily by viewModel.averageDaily.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val topContacts by viewModel.topContacts.collectAsState()
    val weeklyTrend by viewModel.weeklyTrend.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Balance overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Ksh ${String.format("%.2f", totalBalance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Ksh ${String.format("%.0f", monthlyIncome)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Spent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Ksh ${String.format("%.0f", monthlyTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Summary row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsSummaryCard(
                        title = "Week",
                        value = "Ksh ${String.format("%.0f", weeklyTotal)}",
                        icon = Icons.Default.CalendarMonth,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsSummaryCard(
                        title = "Month",
                        value = "Ksh ${String.format("%.0f", monthlyTotal)}",
                        icon = Icons.Default.TrendingDown,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsSummaryCard(
                        title = "Avg/Day",
                        value = "Ksh ${String.format("%.0f", averageDaily)}",
                        icon = Icons.Default.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly trend chart
            item {
                Text(
                    text = "7-Day Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (weeklyTrend.isNotEmpty()) {
                    WeeklyTrendChart(weeklyTrend)
                }
            }

            // Category breakdown
            item {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (categoryBreakdown.isEmpty()) {
                item {
                    Text(
                        text = "No category data yet. Start categorizing transactions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(categoryBreakdown) { item ->
                    CategoryBreakdownRow(item)
                }
            }

            // Top contacts
            item {
                Text(
                    text = "Top Contacts This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (topContacts.isEmpty()) {
                item {
                    Text(
                        text = "No data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(topContacts) { contact ->
                    ContactRow(contact)
                }
            }

            // Daily breakdown
            item {
                Text(
                    text = "Daily Breakdown (Last 30 days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (dailySummary.isEmpty()) {
                item {
                    Text(
                        text = "No data available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(dailySummary.take(30)) { summary ->
                    summary?.let { DailyBreakdownBar(it) }
                }
            }

            // Top channel
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Top Spending Channel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = topChannel.ifEmpty { "N/A" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Most used channel this month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTrendChart(trend: List<TrendPoint>) {
    val maxAmount = trend.maxOfOrNull { it.amount } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            trend.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${String.format("%.0f", point.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(if (maxAmount > 0) ((point.amount / maxAmount) * 80).dp else 0.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = point.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(item: CategoryAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.category.icon,
                contentDescription = null,
                tint = item.category.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Ksh ${String.format("%.0f", item.totalSpent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { item.percentageOfTotal },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = item.category.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.transactionCount} transactions • ${String.format("%.0f", item.percentageOfTotal * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ContactAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${contact.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Ksh ${String.format("%.0f", contact.totalSpent)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyBreakdownBar(summary: DailySummaryEntity) {
    val maxOutgoing = 1000.0
    val barFraction = (summary.totalOutgoing / maxOutgoing).coerceIn(0.0, 1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = summary.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ksh ${String.format("%.0f", summary.totalOutgoing)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction.toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}