package com.tracky.app.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    val transaction by viewModel.transaction.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val deleted by viewModel.deleted.collectAsState()

    LaunchedEffect(saved) {
        if (saved) onNavigateBack()
    }

    LaunchedEffect(deleted) {
        if (deleted) onNavigateBack()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val tx = transaction

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (tx == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tx.type == "INCOMING")
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (tx.type == "INCOMING") "+${String.format("%.2f", tx.amount)}" else "-${String.format("%.2f", tx.amount)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "INCOMING")
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (tx.type == "INCOMING") "Received" else "Sent",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (tx.type == "INCOMING")
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryPicker(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.setCategory(it) }
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow("Channel", tx.channel, Icons.Default.AccountBalance)
                            DetailRow("Contact", tx.contact ?: "Unknown", Icons.Default.Person)
                            DetailRow("Date", SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(tx.timestamp)), Icons.Default.CalendarToday)
                            tx.balance?.let {
                                DetailRow("Balance After", String.format("%.2f", it), Icons.Default.AccountBalanceWallet)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Original Message",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = tx.messageBody,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.setNotes(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a note...") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Button(
                        onClick = { viewModel.save() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes")
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete()
                        showDeleteDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                Icon(
                    selectedCategory.icon,
                    contentDescription = null,
                    tint = selectedCategory.color
                )
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Category.selectable.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(category.icon, contentDescription = null, tint = category.color)
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}