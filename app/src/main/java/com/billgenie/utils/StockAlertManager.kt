package com.billgenie.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.billgenie.InventoryActivity
import com.billgenie.MainActivity
import com.billgenie.R
import com.billgenie.model.InventoryDisplayItem

class StockAlertManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "stock_alerts"
        private const val CHANNEL_NAME = "Stock Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for critical stock levels"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Send notification for a single critical stock item
     */
    fun sendCriticalStockAlert(item: InventoryDisplayItem) {
        if (!item.isCriticallyLow) return

        if (!areNotificationsEnabled()) {
            android.util.Log.w("StockAlertManager", "Notifications are not enabled for this app")
            return
        }

        // Create intent that goes to MainActivity first, then navigates to InventoryActivity
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("navigate_to_inventory", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stockPercentage = String.format("%.1f", item.stockPercentage)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ Critical Stock Alert")
            .setContentText("${item.ingredientName}: Only ${stockPercentage}% remaining!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${item.ingredientName} is critically low!\n" +
                        "Current: ${UnitConverter.formatDisplayText(item.currentStock, item.ingredientUnit)}\n" +
                        "Full Stock: ${UnitConverter.formatDisplayText(item.fullQuantity, item.ingredientUnit)}\n" +
                        "Stock Level: ${stockPercentage}%\n\n" +
                        "Tap to view inventory"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + item.ingredientId.toInt()
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("StockAlertManager", "Notification permission not granted", e)
        } catch (e: Exception) {
            android.util.Log.e("StockAlertManager", "Error sending notification", e)
        }
    }

    /**
     * Send summary notification for multiple critical stock items
     */
    fun sendMultipleCriticalStockAlert(criticalItems: List<InventoryDisplayItem>) {
        if (criticalItems.isEmpty()) return

        // Create intent that goes to MainActivity first, then navigates to InventoryActivity
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("navigate_to_inventory", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val itemCount = criticalItems.size
        val firstItem = criticalItems.first()
        
        val bigTextContent = StringBuilder().apply {
            append("Multiple ingredients are critically low:\n\n")
            criticalItems.take(5).forEach { item ->
                val percentage = String.format("%.1f", item.stockPercentage)
                append("• ${item.ingredientName}: ${percentage}%\n")
            }
            if (criticalItems.size > 5) {
                append("... and ${criticalItems.size - 5} more\n")
            }
            append("\nTap to view inventory")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ Critical Stock Alert")
            .setContentText("$itemCount ingredients are critically low!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setNumber(itemCount)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BASE, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("StockAlertManager", "Notification permission not granted", e)
        }
    }

    /**
     * Check a list of inventory items and send appropriate notifications
     */
    fun checkAndSendStockAlerts(inventoryItems: List<InventoryDisplayItem>) {
        val criticalItems = inventoryItems.filter { it.isCriticallyLow }
        
        when {
            criticalItems.isEmpty() -> {
                // Clear any existing notifications
                clearStockAlerts()
            }
            criticalItems.size == 1 -> {
                // Send single item notification
                sendCriticalStockAlert(criticalItems.first())
            }
            else -> {
                // Send summary notification for multiple items
                sendMultipleCriticalStockAlert(criticalItems)
            }
        }
    }

    /**
     * Clear all stock alert notifications
     */
    fun clearStockAlerts() {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Clear summary notification
            notificationManager.cancel(NOTIFICATION_ID_BASE)
            // Clear individual notifications (clear range for safety)
            for (i in NOTIFICATION_ID_BASE until NOTIFICATION_ID_BASE + 1000) {
                notificationManager.cancel(i)
            }
        } catch (e: SecurityException) {
            android.util.Log.w("StockAlertManager", "Cannot clear notifications - permission not granted", e)
        }
    }

    /**
     * Check if notifications are enabled for this app
     */
    fun areNotificationsEnabled(): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }
    }
}