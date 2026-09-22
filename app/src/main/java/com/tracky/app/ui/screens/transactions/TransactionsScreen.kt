package com.tracky.app.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tracky.app.ui.components.EmptyState
import com.tracky.app.ui.components.TransactionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        singleLine = true
                    )
                }

                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = filterType == "INCOMING",
                            onClick = { viewModel.setFilter("INCOMING") },
                            label = { Text("Incoming") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        FilterChip(
                            selected = filterType == "OUTGOING",
                            onClick = { viewModel.setFilter("OUTGOING") },
                            label = { Text("Outgoing") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No transactions found",
                            description = if (searchQuery.isNotBlank() || filterType != null) {
                                "Try adjusting your search or filter."
                            } else {
                                "Start by granting SMS permission and scanning your messages."
                            },
                            modifier = Modifier.padding(vertical = 48.dp)
                        )
                    }
                } else {
                    items(transactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}