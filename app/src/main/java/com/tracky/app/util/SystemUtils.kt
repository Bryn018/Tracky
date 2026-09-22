package com.tracky.app.util

import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object SystemUtils {

    fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            val myPackage = context.packageName
            val defaultSmsPackage = Settings.Secure.getString(
                context.contentResolver,
                "sms_default_application"
            )
            myPackage == defaultSmsPackage
        }
    }

    fun openDefaultSmsSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_SMS)?.let { intent ->
                context.startActivity(intent)
                return
            }
        }
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        context.startActivity(intent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
