package com.tracky.app.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tracky.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    exportLauncher: (Intent) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val reportEnabled by viewModel.reportEnabled.collectAsState()
    val reportHour by viewModel.reportHour.collectAsState()
    val reportMinute by viewModel.reportMinute.collectAsState()
    val clearState by viewModel.clearDataState.collectAsState()
    val backupState by backupViewModel.backupState.collectAsState()
    val restoreState by backupViewModel.restoreState.collectAsState()
    val backupMetadata by backupViewModel.backupMetadata.collectAsState()

    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToCsv(uri)
        }
    }

    val backupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupViewModel.exportBackup(uri)
        }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            backupViewModel.selectBackupFile(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Daily Report section
            item {
                Text(
                    text = "Daily Report",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Enable daily report",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Switch(
                                checked = reportEnabled,
                                onCheckedChange = { viewModel.setReportEnabled(it) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Report time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format("%02d:%02d", reportHour, reportMinute),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Budget section
            item {
                Text(
                    text = "Budgeting",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingRow(
                            icon = Icons.Default.Savings,
                            title = "Monthly Budgets",
                            subtitle = "Set spending limits for each category",
                            actionText = "Manage",
                            onClick = onNavigateToBudgets
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Backup section
            item {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { backupFileLauncher.launch("tracky_backup_${System.currentTimeMillis()}.json") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup")
                            }
                            OutlinedButton(
                                onClick = { restoreFileLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore")
                            }
                        }

                        if (backupState is BackupState.InProgress) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (backupState is BackupState.Success) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (backupState as BackupState.Success).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (backupState is BackupState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (backupState as BackupState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Restore metadata
            if (backupMetadata != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selected Backup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${backupMetadata!!.transactionCount} transactions • ${backupMetadata!!.budgetCount} budgets",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Exported: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(backupMetadata!!.exportedAt))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { backupViewModel.restoreBackup() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Restore This Backup")
                            }
                            if (restoreState is BackupState.Success) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = (restoreState as BackupState.Success).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (restoreState is BackupState.Error) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = (restoreState as BackupState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tracking section
            item {
                Text(
                    text = "Tracking & Reliability",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingRow(
                            icon = Icons.Default.Security,
                            title = stringResource(R.string.settings_default_sms),
                            subtitle = viewModel.defaultSmsStatus.collectAsState().value,
                            actionText = stringResource(R.string.settings_open_default_sms_settings),
                            onClick = { viewModel.openDefaultSmsSettings() }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingRow(
                            icon = Icons.Default.BatteryChargingFull,
                            title = stringResource(R.string.settings_battery_optimization),
                            subtitle = viewModel.batteryOptimizationStatus.collectAsState().value,
                            actionText = stringResource(R.string.settings_exclude_battery_optimization),
                            onClick = { viewModel.requestBatteryOptimizationExemption() }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Data section
            item {
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportFileLauncher.launch("tracky_transactions_${System.currentTimeMillis()}.csv") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export CSV")
                            }
                            OutlinedButton(
                                onClick = { viewModel.shareExportedFile(exportLauncher) },
                                modifier = Modifier.weight(1f),
                                enabled = viewModel.exportedFileForShare.collectAsState().value != null
                            ) {
                                Text("Share")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.requestClearData() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear Data")
                            }
                        }

                        val exportState by viewModel.exportState.collectAsState()
                        val exportedFile by viewModel.exportedFileForShare.collectAsState()

                        if (exportState is ExportState.Success && exportedFile != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Exported: ${exportedFile!!.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (exportState is ExportState.Error) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = (exportState as ExportState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (clearState is DataState.Confirming) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "This will permanently delete all transactions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { viewModel.cancelClearData() }) {
                                    Text("Cancel")
                                }
                                TextButton(onClick = { viewModel.confirmClearData() }) {
                                    Text("Delete All")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // About section
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tracky",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Version 1.1.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tracky automatically tracks your mobile money transactions from SMS messages and provides offline spending analytics — all without an internet connection. Your financial data never leaves your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        LaunchedEffect(clearState) {
            if (clearState is DataState.Done) {
                viewModel.dismissClearDataState()
            }
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = reportHour,
                initialMinute = reportMinute,
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setReportTime(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Select Report Time") },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClick) {
            Text(actionText)
        }
    }
}