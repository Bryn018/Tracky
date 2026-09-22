package com.tracky.app.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracky.app.util.SystemUtils

enum class OnboardingStep {
    SMS,
    NOTIFICATIONS,
    DEFAULT_SMS,
    BATTERY
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSmsGranted: () -> Unit
) {
    val currentStep = rememberSaveable { mutableIntStateOf(0) }
    val steps = OnboardingStep.entries.toTypedArray()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Tracky",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            when (steps[currentStep.intValue]) {
                OnboardingStep.SMS -> {
                    StepContent(
                        icon = Icons.Default.Sms,
                        title = "SMS Access",
                        description = "Tracky reads SMS messages to detect mobile money transactions. All data stays on your device.",
                        onEnable = {
                            onSmsGranted()
                        },
                        onSkip = { currentStep.intValue++ },
                        enableText = "Grant Permission",
                        skipText = "Skip"
                    )
                }

                OnboardingStep.NOTIFICATIONS -> {
                    StepContent(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Allow notifications so daily spending reports and transaction alerts can reach you.",
                        onEnable = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onSkip = { currentStep.intValue++ },
                        enableText = "Enable",
                        skipText = "Skip"
                    )
                }

                OnboardingStep.DEFAULT_SMS -> {
                    StepContent(
                        icon = Icons.Default.Security,
                        title = "Default SMS App",
                        description = "On Android 14+, real-time SMS tracking works best when Tracky is your default SMS app. You can change this anytime.",
                        onEnable = {
                            SystemUtils.openDefaultSmsSettings(context)
                        },
                        onSkip = { currentStep.intValue++ },
                        enableText = "Set Default",
                        skipText = "Skip"
                    )
                }

                OnboardingStep.BATTERY -> {
                    StepContent(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Battery Optimization",
                        description = "Exclude Tracky from battery optimization to keep SMS monitoring and daily reports reliable.",
                        onEnable = {
                            SystemUtils.requestIgnoreBatteryOptimizations(context)
                        },
                        onSkip = { currentStep.intValue++ },
                        enableText = "Exclude",
                        skipText = "Skip",
                        isLast = true,
                        onFinishStep = onFinish
                    )
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
    enableText: String,
    skipText: String,
    isLast: Boolean = false,
    onFinishStep: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (isLast && onFinishStep != null) {
                        onFinishStep()
                    } else {
                        onSkip()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(skipText)
            }
            Button(
                onClick = {
                    onEnable()
                    if (isLast && onFinishStep != null) {
                        onFinishStep()
                    } else {
                        onSkip()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(enableText)
            }
        }
    }
}
