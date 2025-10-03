package com.billgenie

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityMainBinding
import com.billgenie.utils.MonthlyBackupManager
import com.billgenie.utils.BackupReminderManager
import com.billgenie.utils.RoleManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var backupManager: MonthlyBackupManager
    private lateinit var reminderManager: BackupReminderManager
    private val NOTIFICATION_PERMISSION_CODE = 1002
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check authentication first
        if (!isUserLoggedIn()) {
            redirectToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBackupManager()
        setupReminderManager()
        setupToolbar()
        setupClickListeners()
        initializeDatabase()
        applyRoleBasedRestrictions()
        checkBackupNeeded()
        handleNotificationIntent()
    }
    
    private fun setupBackupManager() {
        val database = BillGenieDatabase.getDatabase(this)
        backupManager = MonthlyBackupManager(this, database)
    }
    
    private fun setupReminderManager() {
        reminderManager = BackupReminderManager(this)
        // Setup monthly reminder if notification permission is granted
        if (checkNotificationPermission()) {
            if (!reminderManager.isReminderEnabled()) {
                setupMonthlyReminder()
            }
        }
    }
    
    private fun handleNotificationIntent() {
        // Clear any existing backup reminder notifications when app opens
        reminderManager.clearAllNotifications()
        
        // Check if app was opened from backup reminder notification
        if (intent.getBooleanExtra("open_backup", false)) {
            // Auto-open backup dialog
            performMonthlyBackup()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "BillGenie - Restaurant Manager"
        
        // Ensure overflow menu is visible
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        
        // Force invalidate options menu to ensure it's created
        invalidateOptionsMenu()
    }
    
    private fun setupClickListeners() {
        binding.cardOrdersBilling.setOnClickListener {
            val intent = Intent(this, OrdersBillingActivity::class.java)
            startActivity(intent)
        }
        
        binding.cardMenuPricing.setOnClickListener {
            val intent = Intent(this, MenuPricingActivity::class.java)
            startActivity(intent)
        }
        
        binding.cardSettings.setOnClickListener {
            showAppSettings()
        }
        
        binding.cardReports.setOnClickListener {
            performMonthlyBackup()
        }
    }
    
    private fun initializeDatabase() {
        // Database will be created automatically when first accessed
        // No complex initialization needed for simplified menu system
    }
    
    private fun applyRoleBasedRestrictions() {
        val userRole = RoleManager.getCurrentUserRole(this)
        
        // Update toolbar title to show user role
        supportActionBar?.title = "BillGenie - ${RoleManager.getRoleDisplayName(userRole)}"
        
        // Apply restrictions based on role
        when (userRole) {
            RoleManager.ROLE_STAFF -> applyStaffRestrictions()
            RoleManager.ROLE_MANAGER -> applyManagerRestrictions()
            RoleManager.ROLE_ADMIN -> applyAdminRestrictions()
        }
    }
    
    private fun applyStaffRestrictions() {
        // Staff can only access Orders & Billing
        // Hide other cards
        binding.cardMenuPricing.visibility = android.view.View.GONE
        binding.cardReports.visibility = android.view.View.GONE
        binding.cardSettings.visibility = android.view.View.GONE
    }
    
    private fun applyManagerRestrictions() {
        // Manager can access Orders & Billing and Menu Management
        // Hide backup and settings
        binding.cardReports.visibility = android.view.View.GONE
        binding.cardSettings.visibility = android.view.View.GONE
    }
    
    private fun applyAdminRestrictions() {
        // Admin has access to everything - no restrictions
        // All cards remain visible
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Apply role-based menu restrictions
        val userRole = RoleManager.getCurrentUserRole(this)
        when (userRole) {
            RoleManager.ROLE_STAFF -> {
                // Staff can see basic menu items but might have limited actions
                // Keep all menu items visible for now
            }
            RoleManager.ROLE_MANAGER -> {
                // Manager can see all menu items
            }
            RoleManager.ROLE_ADMIN -> {
                // Admin can see all menu items
            }
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showAppInfo()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun checkBackupNeeded() {
        lifecycleScope.launch {
            try {
                if (backupManager.isBackupNeeded()) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Backup Needed")
                        .setMessage("You have bills older than 1 month. Would you like to backup and clean the database?")
                        .setPositiveButton("Backup Now") { _, _ ->
                            performMonthlyBackup()
                        }
                        .setNegativeButton("Later", null)
                        .show()
                }
            } catch (e: Exception) {
                // Silently ignore - backup check is optional
            }
        }
    }

    // Quick backup menu for toolbar (most common actions)
    private fun showQuickBackupMenu() {
        val options = arrayOf(
            "Backup & Delete Bills",
            "View Last Backup Info",
            "See All Sales Reports"
        )

        AlertDialog.Builder(this)
            .setTitle("Quick Backup & Sales")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> lifecycleScope.launch { backupManager.performIncrementalBackup() }
                    1 -> showBackupInfo()
                    2 -> performMonthlyBackup() // Opens full menu for all reports
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Full backup menu for Monthly Backup card (all options)
    private fun performMonthlyBackup() {
        // Check if user has permission to access backup functionality
        if (!RoleManager.canAccessBackup(this)) {
            Toast.makeText(this, "Access denied. Administrator privileges required.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Show options dialog for different backup/sales operations
        val options = arrayOf(
            "📊 Current Month Sales",
            "📈 Today's Sales", 
            "💾 Backup Since Last Time",
            "� Last Backup Information",
            "�🔔 Backup Reminders"
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Backup & Sales Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCurrentMonthSales()
                    1 -> showTodaySales()
                    2 -> lifecycleScope.launch { backupManager.performIncrementalBackup() }
                    3 -> showBackupInfo()
                    4 -> showReminderSettings()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCurrentMonthSales() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Calculating month sales...", Toast.LENGTH_SHORT).show()
                val salesData = backupManager.getCurrentMonthSales()
                
                val message = buildString {
                    append("💰 Sales Report - ${salesData.period}\n\n")
                    append("📊 Total Sales: ₹${String.format("%.2f", salesData.totalAmount)}\n")
                    append("🧾 Total Bills: ${salesData.billCount}\n")
                    append("📅 Period: ${salesData.startDate} to ${salesData.endDate}\n\n")
                    
                    if (salesData.billCount > 0) {
                        val avgPerBill = salesData.totalAmount / salesData.billCount
                        append("📈 Average per Bill: ₹${String.format("%.2f", avgPerBill)}")
                    } else {
                        append("ℹ️ No sales recorded for this period")
                    }
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Current Month Sales")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Failed to calculate sales: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun showTodaySales() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Calculating today's sales...", Toast.LENGTH_SHORT).show()
                val salesData = backupManager.getCurrentDaySales()
                
                val message = buildString {
                    append("💰 Sales Report - ${salesData.period}\n\n")
                    append("📊 Total Sales: ₹${String.format("%.2f", salesData.totalAmount)}\n")
                    append("🧾 Total Bills: ${salesData.billCount}\n")
                    append("📅 Date: ${salesData.startDate}\n\n")
                    
                    if (salesData.billCount > 0) {
                        val avgPerBill = salesData.totalAmount / salesData.billCount
                        append("📈 Average per Bill: ₹${String.format("%.2f", avgPerBill)}")
                    } else {
                        append("ℹ️ No sales recorded today")
                    }
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Today's Sales")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Failed to calculate today's sales: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun showBackupInfo() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Checking backup status...", Toast.LENGTH_SHORT).show()
                val backupInfo = backupManager.getBackupInfo()
                
                val message = buildString {
                    if (backupInfo.isFirstBackup) {
                        append("🔄 First Time Backup\n\n")
                        append("📊 Bills to backup: ${backupInfo.pendingBillCount}\n")
                        append("💰 Total amount: ₹${String.format("%.2f", backupInfo.pendingAmount)}\n\n")
                        append("This will backup all your bills and clean the database.")
                    } else {
                        append("🔄 Incremental Backup\n\n")
                        append("📅 Last backup: ${backupInfo.lastBackupDate}\n")
                        append("📊 New bills since then: ${backupInfo.pendingBillCount}\n")
                        append("💰 New amount: ₹${String.format("%.2f", backupInfo.pendingAmount)}\n\n")
                        if (backupInfo.pendingBillCount > 0) {
                            append("This will backup bills since your last backup and clean them from database.")
                        } else {
                            append("No new bills to backup.")
                        }
                    }
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Backup Information")
                    .setMessage(message)
                    .setPositiveButton(if (backupInfo.pendingBillCount > 0) "Backup Now" else "OK") { _, _ ->
                        if (backupInfo.pendingBillCount > 0) {
                            performActualBackup()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                    
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Failed to check backup status: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun performActualBackup() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Backup")
            .setMessage("This will backup bills to CSV and remove them from the database. Continue?")
            .setPositiveButton("Backup & Clean") { _, _ ->
                lifecycleScope.launch {
                    try {
                        Toast.makeText(this@MainActivity, "Creating backup...", Toast.LENGTH_SHORT).show()
                        val result = backupManager.performIncrementalBackup()
                        
                        if (result.success) {
                            val backupType = if (result.isFirstBackup) "Initial" else "Incremental"
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("Backup Completed")
                                .setMessage("$backupType backup completed!\n\n📊 Bills backed up: ${result.billCount}\n📁 File saved to:\n${result.filePath}")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("Backup Info")
                                .setMessage(result.message)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    } catch (e: Exception) {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Backup Failed")
                            .setMessage("Error: ${e.message}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatabaseStats() {
        lifecycleScope.launch {
            try {
                val stats = backupManager.getDatabaseStats()
                val message = buildString {
                    append("Database Statistics:\n\n")
                    append("📊 Total Bills: ${stats.totalBills}\n")
                    append("🍽️ Menu Items: ${stats.totalMenuItems}\n")
                    append("📅 Date Range: ${stats.oldestBillDate ?: "No bills"} to ${stats.newestBillDate ?: "No bills"}")
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Database Info")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error loading stats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showReminderSettings() {
        val isEnabled = reminderManager.isReminderEnabled()
        val nextReminderDate = if (isEnabled) reminderManager.getNextReminderDateString() else "Not scheduled"
        val debugInfo = reminderManager.getDebugInfo()
        
        val message = buildString {
            append("🔔 Monthly Backup Reminder Settings\n\n")
            append("Status: ${if (isEnabled) "Enabled" else "Disabled"}\n")
            append("Next reminder: $nextReminderDate\n\n")
            append("When enabled, you'll receive a notification at 10 PM on the last day of every month to remind you to backup your bills.\n\n")
            append(debugInfo)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Backup Reminders")
            .setMessage(message)
            .setPositiveButton(if (isEnabled) "Disable" else "Enable") { _, _ ->
                if (isEnabled) {
                    reminderManager.cancelMonthlyReminder()
                    Toast.makeText(this, "Monthly reminders disabled", Toast.LENGTH_SHORT).show()
                } else {
                    setupMonthlyReminder()
                }
            }
            .setNeutralButton("Test Now") { _, _ ->
                testNotification()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setupMonthlyReminder() {
        if (checkNotificationPermission()) {
            reminderManager.scheduleMonthlyReminder()
            val nextDate = reminderManager.getNextReminderDateString()
            Toast.makeText(this, "Monthly reminder enabled! Next: $nextDate", Toast.LENGTH_LONG).show()
        } else {
            requestNotificationPermission()
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications allowed by default on older versions
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMonthlyReminder()
            } else {
                Toast.makeText(this, "Notification permission required for reminders", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun testNotification() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Test Notification")
            .setMessage("This will manually trigger the backup reminder notification to test if notifications are working properly.")
            .setPositiveButton("Send Notification") { _, _ ->
                if (checkNotificationPermission()) {
                    // Manually trigger the notification
                    val receiver = BackupReminderReceiver()
                    receiver.onReceive(this, android.content.Intent())
                    Toast.makeText(this, "Test notification sent! Check your notification panel.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Please enable notification permission first", Toast.LENGTH_SHORT).show()
                    requestNotificationPermission()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAppSettings() {
        // Check if user has permission to access app settings
        if (!RoleManager.canAccessAppInformation(this)) {
            Toast.makeText(this, "Access denied. Manager or Admin privileges required.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Show settings options dialog with role-based options
        val settingsOptions = if (RoleManager.isAdmin(this)) {
            arrayOf(
                "👥 User Management",
                "📊 Database Statistics", 
                "🧹 Clear Notifications",
                "📋 App Information"
            )
        } else {
            arrayOf(
                "📊 Database Statistics", 
                "🧹 Clear Notifications",
                "📋 App Information"
            )
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Settings")
            .setItems(settingsOptions) { _, which ->
                if (RoleManager.isAdmin(this)) {
                    when (which) {
                        0 -> showUserManagement()
                        1 -> showDatabaseStats()
                        2 -> clearAllNotifications()
                        3 -> showAppInfo()
                    }
                } else {
                    when (which) {
                        0 -> showDatabaseStats()
                        1 -> clearAllNotifications()
                        2 -> showAppInfo()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showUserManagement() {
        val userOptions = arrayOf(
            "➕ Register New User",
            "👥 View All Users"
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle("User Management")
            .setItems(userOptions) { _, which ->
                when (which) {
                    0 -> {
                        // Open RegisterActivity
                        val intent = Intent(this, RegisterActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> showAllUsers()
                }
            }
            .setNegativeButton("Back", null)
            .show()
    }
    
    private fun showAllUsers() {
        // This could be enhanced to show a list of all users
        Toast.makeText(this, "User list feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearAllNotifications() {
        reminderManager.clearAllNotifications()
        Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun showAppInfo() {
        // Check if user has permission to access app information
        if (!RoleManager.canAccessAppInformation(this)) {
            Toast.makeText(this, "Access denied. Manager or Admin privileges required.", Toast.LENGTH_LONG).show()
            return
        }
        
        val message = buildString {
            append("🍽️ BillGenie Restaurant Manager\n\n")
            append("📱 Version: 1.0\n")
            append("�‍💻 Developed by: Nandu\n")
            append("�📅 Built: September 2025\n\n")
            append("Features:\n")
            append("• Menu & pricing management\n")
            append("• Bill generation\n")
            append("• Incremental backup system\n")
            append("• Monthly backup reminders\n")
            append("• Sales reporting\n\n")
            append("💡 Tip: Use 'Backup & Sales' regularly to keep your database clean and maintain business records.")
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("About BillGenie")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        LoginActivity.logout(this)
        redirectToLogin()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}