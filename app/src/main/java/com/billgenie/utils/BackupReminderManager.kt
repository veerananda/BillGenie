package com.billgenie.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.billgenie.BackupReminderReceiver
import java.util.*

class BackupReminderManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    private val REMINDER_ENABLED_KEY = "reminder_enabled"
    private val REMINDER_REQUEST_CODE = 12345
    
    /**
     * Schedule monthly backup reminder for 10 PM on last day of every month
     */
    fun scheduleMonthlyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BackupReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Calculate next reminder time (10 PM on last day of current month)
        val nextReminderTime = getNextReminderTime()
        
        // Schedule the alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextReminderTime,
                pendingIntent
            )
            
            // Save reminder enabled state
            prefs.edit().putBoolean(REMINDER_ENABLED_KEY, true).apply()
            
        } catch (e: SecurityException) {
            // Handle case where exact alarm permission is not granted
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextReminderTime,
                pendingIntent
            )
        }
    }
    
    /**
     * Cancel the monthly reminder
     */
    fun cancelMonthlyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BackupReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        prefs.edit().putBoolean(REMINDER_ENABLED_KEY, false).apply()
    }
    
    /**
     * Check if reminder is currently enabled
     */
    fun isReminderEnabled(): Boolean {
        return prefs.getBoolean(REMINDER_ENABLED_KEY, false)
    }
    
    /**
     * Calculate the next reminder time (10 PM on last day of current month)
     */
    private fun getNextReminderTime(): Long {
        val calendar = Calendar.getInstance()
        
        // Set to last day of current month at 10 PM
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, lastDayOfMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 22) // 10 PM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If the time has already passed this month, move to next month
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.MONTH, 1)
            val nextMonthLastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, nextMonthLastDay)
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * Get the next reminder date as a readable string
     */
    fun getNextReminderDateString(): String {
        val nextTime = getNextReminderTime()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nextTime
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        return "$day/${String.format("%02d", month)}/$year at 10:00 PM"
    }
    
    /**
     * Reschedule the reminder (useful after a reminder is triggered)
     */
    fun rescheduleNextReminder() {
        if (isReminderEnabled()) {
            scheduleMonthlyReminder()
        }
    }
    
    /**
     * Get detailed debug information about the reminder system
     */
    fun getDebugInfo(): String {
        val currentTime = System.currentTimeMillis()
        val nextReminderTime = getNextReminderTime()
        val calendar = Calendar.getInstance()
        
        return buildString {
            append("🔍 Reminder Debug Info:\n\n")
            append("Current time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(currentTime))}\n")
            append("Reminder enabled: ${isReminderEnabled()}\n")
            append("Next reminder: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(nextReminderTime))}\n")
            append("Time until next: ${(nextReminderTime - currentTime) / (1000 * 60 * 60)} hours\n\n")
            
            // Check if today is the last day of the month
            calendar.timeInMillis = currentTime
            val today = calendar.get(Calendar.DAY_OF_MONTH)
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            
            append("Today's date: $today\n")
            append("Last day of month: $lastDayOfMonth\n")
            append("Current hour: $currentHour\n")
            append("Is last day of month: ${today == lastDayOfMonth}\n")
            append("Is past 10 PM: ${currentHour >= 22}\n")
            
            if (today == lastDayOfMonth && currentHour >= 22) {
                append("\n⚠️ Notification should have triggered!\n")
                append("If you don't see it, check:\n")
                append("• App notifications enabled\n")
                append("• Do Not Disturb settings\n")
                append("• Notification permission granted")
            }
        }
    }
    
    /**
     * Clear all backup reminder notifications
     */
    fun clearAllNotifications() {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(1001) // Cancel backup reminder notification
            notificationManager.cancelAll() // Clear all notifications from this app
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}