package com.tracky.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Utility for scheduling and cancelling daily report alarms.
 */
object DailyReportScheduler {

    private const val TAG = "DailyReportScheduler"
    private const val ALARM_REQUEST_CODE = 1001

    /**
     * Schedules a daily alarm at the specified hour and minute.
     * Uses [AlarmManager.setAlarmClock] for exact timing (shows in system UI),
     * falling back to [AlarmManager.setRepeating] if needed.
     */
    fun scheduleReport(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReportAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use setAlarmClock for exact timing — shows in system UI
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                calendar.timeInMillis,
                pendingIntent
            )
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Daily report scheduled with setAlarmClock at $hour:$minute")
        } else {
            // Fallback to repeating alarm on older devices
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Daily report scheduled with setRepeating at $hour:$minute")
        }
    }

    /**
     * Cancels any existing daily report alarm.
     */
    fun cancelReport(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReportAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Daily report alarm cancelled")
    }
}
