package com.billgenie

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.billgenie.utils.BackupReminderManager

class BackupReminderReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "backup_reminder_channel"
        private const val NOTIFICATION_ID = 1001
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel(context)
        
        // Show backup reminder notification
        showBackupReminderNotification(context)
        
        // Schedule next month's reminder
        val reminderManager = BackupReminderManager(context)
        reminderManager.rescheduleNextReminder()
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Backup Reminders"
            val descriptionText = "Monthly backup reminder notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showBackupReminderNotification(context: Context) {
        // Create intent to open MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_backup", true) // Extra to auto-open backup dialog
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("📅 Monthly Backup Reminder")
            .setContentText("It's the last day of the month! Time to backup your bills.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's the last day of the month! Don't forget to backup your restaurant bills and clear your database. Tap to open BillGenie and backup now."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(1000 * 60 * 60 * 2) // Auto-remove after 2 hours
            .addAction(
                android.R.drawable.ic_menu_save,
                "Backup Now",
                pendingIntent
            )
        
        // Show the notification
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            // Silently fail - user will see reminder next month if they grant permission
        }
    }
}