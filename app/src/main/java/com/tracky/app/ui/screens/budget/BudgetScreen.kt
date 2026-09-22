package com.tracky.app.ui.screens.budget

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tracky.app.data.model.BudgetCalculator
import com.tracky.app.data.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()
    val editingCategory by viewModel.editingCategory.collectAsState()
    val editingLimit by viewModel.editingLimit.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Monthly Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set spending limits for each category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (categorySpending.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No budgets set",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap + to set your first budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(categorySpending) { spending ->
                BudgetProgressCard(
                    spending = spending,
                    onEdit = { viewModel.showEditDialog(spending.category, spending.budget ?: 0.0) },
                    onDelete = { viewModel.deleteBudget(spending.category) }
                )
            }
        }

        if (showDialog) {
            BudgetEditDialog(
                category = editingCategory,
                limit = editingLimit,
                onLimitChange = { viewModel.setEditingLimit(it) },
                onDismiss = { viewModel.dismissDialog() },
                onSave = { viewModel.saveBudget() }
            )
        }
    }
}

@Composable
private fun BudgetProgressCard(
    spending: BudgetCalculator.CategorySpending,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        spending.category.icon,
                        contentDescription = null,
                        tint = spending.category.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = spending.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val progress = spending.progressFraction
            val progressColor = when {
                spending.isOverBudget -> MaterialTheme.colorScheme.error
                progress > 0.7f -> Color(0xFFFF9800)
                else -> spending.category.color
            }

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ksh ${String.format("%.0f", spending.spent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (spending.budget != null) "of Ksh ${String.format("%.0f", spending.budget)}" else "No limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (spending.isOverBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Over budget by Ksh ${String.format("%.0f", spending.spent - (spending.budget ?: 0.0))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    category: Category?,
    limit: String,
    onLimitChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category != null) "Edit Budget" else "Add Budget") },
        text = {
            Column {
                if (category != null) {
                    Text("Category: ${category.displayName}")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = limit,
                    onValueChange = onLimitChange,
                    label = { Text("Monthly Limit (Ksh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}