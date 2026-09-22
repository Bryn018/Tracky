package com.tracky.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.rememberNavController
import com.tracky.app.ui.navigation.MainScreen
import com.tracky.app.ui.theme.TrackyTheme
import dagger.hilt.android.AndroidEntryPoint
import com.tracky.app.ui.screens.onboarding.OnboardingScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var smsPermissionGranted by mutableStateOf(false)
    private var notificationPermissionGranted by mutableStateOf(false)
    private var onboardingCompleted by mutableStateOf(false)

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val wasGranted = permissions[Manifest.permission.READ_SMS] == true
        smsPermissionGranted = wasGranted
        if (wasGranted) {
            checkAndRequestNotificationPermission()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* handled by SettingsViewModel */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkSmsPermission()
        checkNotificationPermission()

        // Auto-request SMS permission on first launch if not already granted
        if (!smsPermissionGranted) {
            requestSmsPermission()
        }

        setContent {
            TrackyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingCompleted) {
                        val navController = rememberNavController()
                        MainScreen(
                            navController = navController,
                            hasSmsPermission = smsPermissionGranted,
                            onRequestPermission = { requestSmsPermission() },
                            exportLauncher = { exportLauncher.launch(it) }
                        )
                    } else {
                        OnboardingScreen(
                            onFinish = { onboardingCompleted = true },
                            onSmsGranted = { requestSmsPermission() }
                        )
                    }
                }
            }
        }
    }

    private fun checkSmsPermission() {
        smsPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationPermissionGranted = true
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationPermissionGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestSmsPermission() {
        smsPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )
    }
}
