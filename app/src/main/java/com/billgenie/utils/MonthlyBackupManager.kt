package com.billgenie.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.billgenie.database.BillGenieDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MonthlyBackupManager(
    private val context: Context,
    private val database: BillGenieDatabase
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
    
    private val prefs: SharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
    private val LAST_BACKUP_DATE_KEY = "last_backup_date"
    
    /**
     * Export bills since last backup and clean them from database
     */
    suspend fun performIncrementalBackup(): BackupResult = withContext(Dispatchers.IO) {
        val lastBackupDate = getLastBackupDate()
        val currentDateTime = dateTimeFormat.format(Date())
        
        // Get bills since last backup (or all bills if first backup)
        val bills = if (lastBackupDate != null) {
            database.billDao().getBillsByDateRange(lastBackupDate, currentDateTime)
        } else {
            // First backup - get all bills
            database.billDao().getAllActiveBills()
        }
        
        if (bills.isEmpty()) {
            return@withContext BackupResult(
                success = false,
                filePath = "",
                billCount = 0,
                message = "No bills to backup since last backup",
                isFirstBackup = lastBackupDate == null
            )
        }
        
        // Export bills to CSV
        val csvFile = exportIncrementalBills(bills, lastBackupDate, currentDateTime)
        
        // Clean bills from database after successful export
        cleanBillsFromDatabase(bills)
        
        // Update last backup date
        saveLastBackupDate(currentDateTime)
        
        return@withContext BackupResult(
            success = true,
            filePath = csvFile,
            billCount = bills.size,
            message = "Backup completed successfully",
            isFirstBackup = lastBackupDate == null
        )
    }
    
    /**
     * Export current month bills and clean database
     */
    suspend fun performCurrentMonthBackup(): String = withContext(Dispatchers.IO) {
        val result = performIncrementalBackup()
        return@withContext result.filePath
    }
    
    /**
     * Get the last backup date from SharedPreferences
     */
    private fun getLastBackupDate(): String? {
        return prefs.getString(LAST_BACKUP_DATE_KEY, null)
    }
    
    /**
     * Save the last backup date to SharedPreferences
     */
    private fun saveLastBackupDate(date: String) {
        prefs.edit().putString(LAST_BACKUP_DATE_KEY, date).apply()
    }
    
    /**
     * Get information about pending bills for backup
     */
    suspend fun getBackupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        val lastBackupDate = getLastBackupDate()
        val currentDateTime = dateTimeFormat.format(Date())
        
        val pendingBills = if (lastBackupDate != null) {
            database.billDao().getBillsByDateRange(lastBackupDate, currentDateTime)
        } else {
            database.billDao().getAllActiveBills()
        }
        
        val totalAmount = pendingBills.sumOf { it.totalAmount }
        
        return@withContext BackupInfo(
            lastBackupDate = lastBackupDate,
            pendingBillCount = pendingBills.size,
            pendingAmount = totalAmount,
            isFirstBackup = lastBackupDate == null
        )
    }
    
    /**
     * Export bills for a specific month to CSV file
     */
    private suspend fun exportMonthlyBills(year: Int, month: Int, startDate: String, endDate: String): String {
        val bills = database.billDao().getBillsByDateRange(startDate, endDate)
        
        // Use app's external files directory (no permissions needed)
        val appDir = File(context.getExternalFilesDir(null), "BillGenie_Backups")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        // Create CSV file
        val fileName = "bills_${year}_${String.format("%02d", month)}.csv"
        val csvFile = File(appDir, fileName)
        
        FileWriter(csvFile).use { writer ->
            // Write CSV header
            writer.append("Bill ID,Date,Customer Name,Customer Phone,Total Amount,Items\n")
            
            // Write bill data
            for (bill in bills) {
                val billItems = database.billItemDao().getItemsForBill(bill.id)
                val itemsString = billItems.joinToString(";") { 
                    "${it.itemName}(${it.quantity}x${it.itemPrice})" 
                }
                
                writer.append("${bill.id},")
                writer.append("${bill.dateCreated},")
                writer.append("\"${bill.customerName}\",")
                writer.append("\"${bill.customerPhone}\",")
                writer.append("${bill.totalAmount},")
                writer.append("\"$itemsString\"\n")
            }
        }
        
        return csvFile.absolutePath
    }
    
    /**
     * Export bills for incremental backup
     */
    private suspend fun exportIncrementalBills(bills: List<com.billgenie.model.Bill>, lastBackupDate: String?, currentDateTime: String): String {
        // Use app's external files directory (no permissions needed)
        val appDir = File(context.getExternalFilesDir(null), "BillGenie_Backups")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        // Create CSV file with timestamp
        val timestamp = fileNameFormat.format(Date())
        val backupType = if (lastBackupDate == null) "initial" else "incremental"
        val fileName = "bills_${backupType}_backup_${timestamp}.csv"
        val csvFile = File(appDir, fileName)
        
        FileWriter(csvFile).use { writer ->
            // Write CSV header with backup info
            writer.append("# BillGenie Backup Export\n")
            writer.append("# Backup Type: ${if (lastBackupDate == null) "Initial (All Bills)" else "Incremental"}\n")
            writer.append("# Last Backup: ${lastBackupDate ?: "Never"}\n")
            writer.append("# Current Backup: $currentDateTime\n")
            writer.append("# Total Bills: ${bills.size}\n")
            writer.append("#\n")
            writer.append("Bill ID,Date,Customer Name,Customer Phone,Total Amount,Items\n")
            
            // Write bill data
            for (bill in bills) {
                val billItems = database.billItemDao().getItemsForBill(bill.id)
                val itemsString = billItems.joinToString(";") { 
                    "${it.itemName}(${it.quantity}x${it.itemPrice})" 
                }
                
                writer.append("${bill.id},")
                writer.append("${bill.dateCreated},")
                writer.append("\"${bill.customerName}\",")
                writer.append("\"${bill.customerPhone}\",")
                writer.append("${bill.totalAmount},")
                writer.append("\"$itemsString\"\n")
            }
        }
        
        return csvFile.absolutePath
    }
    
    /**
     * Clean bills from database for the specified date range
     */
    private suspend fun cleanMonthlyBills(startDate: String, endDate: String) {
        // Get bills to delete
        val billsToDelete = database.billDao().getBillsByDateRange(startDate, endDate)
        
        // Delete bill items first (foreign key constraint)
        for (bill in billsToDelete) {
            database.billItemDao().deleteItemsForBill(bill.id)
        }
        
        // Delete bills
        database.billDao().deleteBillsByDateRange(startDate, endDate)
    }
    
    /**
     * Clean specific bills from database
     */
    private suspend fun cleanBillsFromDatabase(bills: List<com.billgenie.model.Bill>) {
        // Delete bill items first (foreign key constraint)
        for (bill in bills) {
            database.billItemDao().deleteItemsForBill(bill.id)
        }
        
        // Delete bills
        for (bill in bills) {
            database.billDao().deleteBill(bill)
        }
    }
    
    /**
     * Get statistics about bills in database
     */
    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        val totalBills = database.billDao().getTotalBillsCount()
        val totalMenuItems = database.menuItemDao().getTotalCount()
        val oldestBill = database.billDao().getOldestBillDate()
        val newestBill = database.billDao().getNewestBillDate()
        
        return@withContext DatabaseStats(
            totalBills = totalBills,
            totalMenuItems = totalMenuItems,
            oldestBillDate = oldestBill,
            newestBillDate = newestBill
        )
    }
    
    /**
     * Check if backup is needed (if bills are older than 1 month)
     */
    suspend fun isBackupNeeded(): Boolean = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1) // Go back 1 month
        val oneMonthAgo = dateFormat.format(calendar.time)
        
        val oldBillsCount = database.billDao().getBillsCountBeforeDate(oneMonthAgo)
        return@withContext oldBillsCount > 0
    }
    
    /**
     * Calculate total sales for the current month till date
     */
    suspend fun getCurrentMonthSales(): SalesData = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        
        // Create month string in format "yyyy-MM"
        val monthString = String.format("%04d-%02d", currentYear, currentMonth)
        
        // Create month start date
        calendar.set(currentYear, currentMonth - 1, 1, 0, 0, 0)
        val monthStart = dateFormat.format(calendar.time)
        
        // Current date
        val today = dateFormat.format(Date())
        
        // Query sales_records table instead of bills table
        val salesRecords = database.salesDao().getSalesByMonth(monthString)
        val totalSales = salesRecords.sumOf { it.finalTotal }
        val billCount = salesRecords.size
        
        return@withContext SalesData(
            totalAmount = totalSales,
            billCount = billCount,
            startDate = monthStart,
            endDate = today,
            period = "Current Month (${monthFormat.format(Date())})"
        )
    }
    
    /**
     * Calculate total sales for the current day till now
     */
    suspend fun getCurrentDaySales(): SalesData = withContext(Dispatchers.IO) {
        val today = dateFormat.format(Date())
        
        // Query sales_records table instead of bills table
        val salesRecords = database.salesDao().getSalesByDate(today)
        val totalSales = salesRecords.sumOf { it.finalTotal }
        val billCount = salesRecords.size
        
        return@withContext SalesData(
            totalAmount = totalSales,
            billCount = billCount,
            startDate = today,
            endDate = today,
            period = "Today ($today)"
        )
    }
    
    /**
     * Get sales data for a specific month
     */
    suspend fun getMonthlySales(year: Int, month: Int): SalesData = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val monthStart = dateFormat.format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val monthEnd = dateFormat.format(calendar.time)
        
        val bills = database.billDao().getBillsByDateRange(monthStart, monthEnd)
        val totalSales = bills.sumOf { it.totalAmount }
        val billCount = bills.size
        
        return@withContext SalesData(
            totalAmount = totalSales,
            billCount = billCount,
            startDate = monthStart,
            endDate = monthEnd,
            period = "${monthFormat.format(calendar.time)}"
        )
    }
}

data class DatabaseStats(
    val totalBills: Int,
    val totalMenuItems: Int,
    val oldestBillDate: String?,
    val newestBillDate: String?
)

data class SalesData(
    val totalAmount: Double,
    val billCount: Int,
    val startDate: String,
    val endDate: String,
    val period: String
)

data class BackupResult(
    val success: Boolean,
    val filePath: String,
    val billCount: Int,
    val message: String,
    val isFirstBackup: Boolean
)

data class BackupInfo(
    val lastBackupDate: String?,
    val pendingBillCount: Int,
    val pendingAmount: Double,
    val isFirstBackup: Boolean
)