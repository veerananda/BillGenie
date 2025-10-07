package com.billgenie

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.adapter.UserManagementAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityMainBinding
import com.billgenie.model.User
import com.billgenie.utils.ReportsAnalyticsManager
import com.billgenie.utils.BackupReminderManager
import com.billgenie.utils.RoleManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: BillGenieDatabase
    private lateinit var reportsManager: ReportsAnalyticsManager
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
        handleNotificationIntent()
    }
    
    private fun setupBackupManager() {
        val database = BillGenieDatabase.getDatabase(this)
        reportsManager = ReportsAnalyticsManager(this, database)
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
        
        // Check if app was opened from stock alert notification
        if (intent.getBooleanExtra("navigate_to_inventory", false)) {
            // Clear the flag to prevent repeated processing
            intent.removeExtra("navigate_to_inventory")
            
            // Auto-navigate to inventory page
            val inventoryIntent = Intent(this, InventoryActivity::class.java)
            inventoryIntent.putExtra("opened_from_notification", true)
            startActivity(inventoryIntent)
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
        
        binding.cardIngredients.setOnClickListener {
            val intent = Intent(this, IngredientsActivity::class.java)
            startActivity(intent)
        }
        
        binding.cardInventory.setOnClickListener {
            val intent = Intent(this, InventoryActivity::class.java)
            startActivity(intent)
        }
        
        binding.cardSettings.setOnClickListener {
            showAppSettings()
        }
        
        binding.cardReports.setOnClickListener {
            showReportsAnalyticsDialog()
        }
    }
    
    private fun initializeDatabase() {
        database = BillGenieDatabase.getDatabase(this)
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
        // Navigate to the new UserManagementActivity
        val intent = Intent(this, UserManagementActivity::class.java)
        startActivity(intent)
    }
    
    private fun getCurrentUser(users: List<User>): User? {
        val currentUsername = getSharedPreferences("user_session", MODE_PRIVATE)
            .getString("username", null)
        android.util.Log.d("UserManagement", "Current username from session: $currentUsername")
        val foundUser = users.find { it.username == currentUsername }
        android.util.Log.d("UserManagement", "Found current user: ${foundUser?.fullName} (${foundUser?.role})")
        return foundUser
    }
    
    private fun showAllUsers() {
        lifecycleScope.launch {
            try {
                // Get all users from database (both active and inactive)
                val users = database.userDao().getAllUsersOnce()
                android.util.Log.d("UserManagement", "Retrieved ${users.size} users from database")
                runOnUiThread {
                    showUserListDialog(users)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserManagement", "Error loading users", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showUserListDialog(users: List<User>) {
        android.util.Log.d("UserManagement", "showUserListDialog called with ${users.size} users")
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_list, null)
        val rvUsers = dialogView.findViewById<RecyclerView>(R.id.rvUsers)
        val tvUserCount = dialogView.findViewById<TextView>(R.id.tvUserCount)
        val llEmptyState = dialogView.findViewById<LinearLayout>(R.id.llEmptyState)
        val btnRefresh = dialogView.findViewById<Button>(R.id.btnRefresh)
        val btnAddUser = dialogView.findViewById<Button>(R.id.btnAddUser)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        
        // Get current user
        val currentUser = getCurrentUser(users)
        
        // Setup RecyclerView
        rvUsers.layoutManager = LinearLayoutManager(this)
        val adapter = UserManagementAdapter(
            users = users,
            currentUser = currentUser ?: users.first(), // Fallback to first user if current user not found
            onToggleStatusClick = { user -> handleToggleUserStatus(user) },
            onEditUserClick = { user -> handleEditUser(user) },
            onDeleteUserClick = { user -> handleDeleteUser(user) }
        )
        rvUsers.adapter = adapter
        
        // Update UI based on user count
        if (users.isEmpty()) {
            rvUsers.visibility = View.GONE
            llEmptyState.visibility = View.VISIBLE
            tvUserCount.text = "0 users"
        } else {
            rvUsers.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
            tvUserCount.text = "${users.size} user${if (users.size != 1) "s" else ""}"
        }
        
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Setup button listeners
        btnRefresh.setOnClickListener {
            dialog.dismiss()
            showAllUsers() // Refresh the list
        }
        
        btnAddUser.setOnClickListener {
            dialog.dismiss()
            // Open RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        
        // Make dialog larger
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun handleToggleUserStatus(user: User) {
        val action = if (user.isActive) "deactivate" else "activate"
        val title = if (user.isActive) "Deactivate User" else "Activate User"
        val message = "Are you sure you want to $action ${user.fullName} (@${user.username})?"
        
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(action.replaceFirstChar { it.uppercase() }) { _, _ ->
                lifecycleScope.launch {
                    try {
                        if (user.isActive) {
                            // Prevent deactivating the current user
                            val currentUsername = getSharedPreferences("user_session", MODE_PRIVATE)
                                .getString("username", "")
                            if (user.username == currentUsername) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Cannot deactivate your own account", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            
                            database.userDao().deactivateUser(user.id)
                        } else {
                            // Reactivate user by updating isActive = true
                            val updatedUser = user.copy(isActive = true)
                            database.userDao().updateUser(updatedUser)
                        }
                        
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "User ${action}d successfully", Toast.LENGTH_SHORT).show()
                            showAllUsers() // Refresh the list
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun handleEditUser(user: User) {
        val roleOptions = arrayOf("ADMIN", "MANAGER", "STAFF")
        var selectedRole = user.role
        var selectedRoleIndex = roleOptions.indexOf(user.role).takeIf { it >= 0 } ?: 0
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit User: ${user.fullName}")
            .setSingleChoiceItems(roleOptions, selectedRoleIndex) { _, which ->
                selectedRole = roleOptions[which]
                selectedRoleIndex = which
            }
            .setPositiveButton("Update Role") { _, _ ->
                if (selectedRole != user.role) {
                    updateUserRole(user, selectedRole)
                } else {
                    Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("View Details") { _, _ ->
                showUserDetails(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateUserRole(user: User, newRole: String) {
        // Prevent changing own role from ADMIN
        val currentUsername = getSharedPreferences("user_session", MODE_PRIVATE)
            .getString("username", "")
        
        if (user.username == currentUsername && user.role == RoleManager.ROLE_ADMIN && newRole != RoleManager.ROLE_ADMIN) {
            Toast.makeText(this, "Cannot remove admin privileges from your own account", Toast.LENGTH_LONG).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val updatedUser = user.copy(role = newRole)
                database.userDao().updateUser(updatedUser)
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "User role updated to $newRole", Toast.LENGTH_SHORT).show()
                    showAllUsers() // Refresh the list
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error updating role: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showUserDetails(user: User) {
        val details = buildString {
            append("👤 Full Name: ${user.fullName}\n\n")
            append("🏷️ Username: @${user.username}\n\n")
            append("📧 Email: ${user.email ?: "Not provided"}\n\n")
            append("👔 Role: ${user.role}\n\n")
            append("🟢 Status: ${if (user.isActive) "Active" else "Inactive"}\n\n")
            append("📅 Created: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(user.createdAt)}\n\n")
            if (user.lastLoginAt != null) {
                append("🕐 Last Login: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(user.lastLoginAt)}")
            } else {
                append("🕐 Last Login: Never")
            }
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("User Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun handleDeleteUser(user: User) {
        // Double-check permissions
        val currentUserRole = RoleManager.getCurrentUserRole(this)
        if (currentUserRole != RoleManager.ROLE_ADMIN) {
            Toast.makeText(this, "Only administrators can delete users", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Prevent self-deletion
        val currentUsername = getSharedPreferences("user_session", MODE_PRIVATE)
            .getString("username", "")
        if (user.username == currentUsername) {
            Toast.makeText(this, "Cannot delete your own account", Toast.LENGTH_SHORT).show()
            return
        }
        
        val deleteMessage = "⚠️ WARNING: This action cannot be undone!\n\n" +
                "Are you sure you want to permanently delete user:\n\n" +
                "👤 ${user.fullName} (@${user.username})\n" +
                "👔 Role: ${user.role}\n\n" +
                "All user data will be permanently removed from the system."
        
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ Delete User")
            .setMessage(deleteMessage)
            .setPositiveButton("Delete") { _, _ ->
                performUserDeletion(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performUserDeletion(user: User) {
        lifecycleScope.launch {
            try {
                database.userDao().deleteUserById(user.id)
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, 
                        "User ${user.fullName} has been permanently deleted", 
                        Toast.LENGTH_LONG).show()
                    showAllUsers() // Refresh the list
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, 
                        "Error deleting user: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
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
    
    private fun showReportsAnalyticsDialog() {
        // Check if user has permission to access backup functionality
        if (!RoleManager.canAccessBackup(this)) {
            Toast.makeText(this, "Access denied. Administrator privileges required.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Show comprehensive options dialog for sales analytics and backup
        val options = arrayOf(
            "📊 Today's Sales Analytics",
            "📈 Monthly Sales Analytics", 
            "📤 Export Today's Sales",
            "📤 Export Monthly Sales",
            "💾 Backup Bills & Clean DB",
            "ℹ️ Backup Information",
            "🔔 Backup Reminders"
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Reports & Analytics")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTodaySalesAnalytics()
                    1 -> showMonthlySalesAnalytics()
                    2 -> exportSalesData("today")
                    3 -> exportSalesData("month")
                    4 -> showBackupConfirmation()
                    5 -> showBackupInformation()
                    6 -> showReminderSettings()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTodaySalesAnalytics() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Loading today's analytics...", Toast.LENGTH_SHORT).show()
                val analytics = reportsManager.getTodaySalesAnalytics()
                
                val message = buildString {
                    append("📊 Today's Sales Report\n")
                    append("📅 ${analytics.date}\n\n")
                    append("💰 Total Revenue: ${String.format("₹%.2f", analytics.totalRevenue)}\n")
                    append("🧾 Total Orders: ${analytics.totalOrders}\n")
                    append("🛍️ Total Items: ${analytics.totalItems}\n\n")
                    
                    if (analytics.totalOrders > 0) {
                        append("📈 Average Order: ${String.format("₹%.2f", analytics.averageOrderValue)}\n\n")
                        append("💳 Payment Breakdown:\n")
                        append("💵 Cash: ${String.format("₹%.2f", analytics.cashAmount)}\n")
                        append("📱 UPI: ${String.format("₹%.2f", analytics.upiAmount)}\n")
                    } else {
                        append("ℹ️ No sales recorded today")
                    }
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Today's Analytics")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Export") { _, _ ->
                        exportSalesData("today")
                    }
                    .show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Failed to load analytics: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun showMonthlySalesAnalytics() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Loading monthly analytics...", Toast.LENGTH_SHORT).show()
                val analytics = reportsManager.getMonthSalesAnalytics()
                
                val message = buildString {
                    append("📊 Monthly Sales Report\n")
                    append("📅 ${analytics.date}\n\n")
                    append("💰 Total Revenue: ${String.format("₹%.2f", analytics.totalRevenue)}\n")
                    append("🧾 Total Orders: ${analytics.totalOrders}\n")
                    append("🛍️ Total Items: ${analytics.totalItems}\n\n")
                    
                    if (analytics.totalOrders > 0) {
                        append("📈 Average Order: ${String.format("₹%.2f", analytics.averageOrderValue)}\n\n")
                        append("💳 Payment Breakdown:\n")
                        append("💵 Cash: ${String.format("₹%.2f", analytics.cashAmount)}\n")
                        append("📱 UPI: ${String.format("₹%.2f", analytics.upiAmount)}\n")
                    } else {
                        append("ℹ️ No sales recorded this month")
                    }
                }
                
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Monthly Analytics")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Export") { _, _ ->
                        exportSalesData("month")
                    }
                    .show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Failed to load analytics: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun exportSalesData(period: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Exporting $period sales data...", Toast.LENGTH_SHORT).show()
                val result = reportsManager.exportSalesData(period)
                
                if (result.success) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Export Successful")
                        .setMessage("${result.recordCount} sales records exported!\n\nFile saved to:\n${result.filePath}")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Export Failed")
                        .setMessage(result.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Export Error")
                    .setMessage("Failed to export sales data: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun showBackupConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Backup Bills")
            .setMessage("This will export your bills to CSV and clean them from the database.\n\nThis action cannot be undone. Continue?")
            .setPositiveButton("Backup & Clean") { _, _ ->
                performBillsBackup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performBillsBackup() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Creating bills backup...", Toast.LENGTH_SHORT).show()
                val result = reportsManager.performIncrementalBackup()
                
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
    
    private fun showBackupInformation() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Checking backup status...", Toast.LENGTH_SHORT).show()
                val backupInfo = reportsManager.getBackupInfo()
                
                val message = buildString {
                    if (backupInfo.isFirstBackup) {
                        append("🔄 First Time Backup\n\n")
                        append("📊 Bills to backup: ${backupInfo.pendingBillCount}\n")
                        append("💰 Total amount: ${String.format("₹%.2f", backupInfo.pendingAmount)}\n\n")
                        append("This will backup all your bills and clean the database.")
                    } else {
                        append("🔄 Incremental Backup\n\n")
                        append("📅 Last backup: ${backupInfo.lastBackupDate}\n")
                        append("📊 New bills since then: ${backupInfo.pendingBillCount}\n")
                        append("💰 New amount: ${String.format("₹%.2f", backupInfo.pendingAmount)}\n\n")
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
                            showBackupConfirmation()
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
}