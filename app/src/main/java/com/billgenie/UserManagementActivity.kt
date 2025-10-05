package com.billgenie

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapters.UserManagementModernAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityUserManagementBinding
import com.billgenie.model.User
import com.billgenie.utils.RoleManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var database: BillGenieDatabase
    private lateinit var adapter: UserManagementModernAdapter
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = BillGenieDatabase.getDatabase(this)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        getCurrentUser()
        loadUsers()
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = "User Management"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = UserManagementModernAdapter(
            onEditClick = { user -> handleEditUser(user) },
            onDeleteClick = { user -> handleDeleteUser(user) },
            onToggleStatusClick = { user -> handleToggleUserStatus(user) }
        )
        
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
            adapter = this@UserManagementActivity.adapter
        }
    }
    
    private fun setupClickListeners() {
        binding.fabAddUser.setOnClickListener {
            openAddUserForm()
        }
    }
    
    private fun getCurrentUser() {
        lifecycleScope.launch {
            try {
                val currentUserId = LoginActivity.getCurrentUserId(this@UserManagementActivity)
                currentUser = database.userDao().getUserById(currentUserId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val users = database.userDao().getAllUsersOnce()
                runOnUiThread {
                    adapter.submitList(users, currentUser)
                    updateEmptyState(users.isEmpty())
                    updateStatistics(users)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UserManagementActivity, 
                        "Error loading users: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateStatistics(users: List<User>) {
        val totalUsers = users.size
        val activeUsers = users.count { it.isActive }
        
        binding.tvTotalUsers.text = totalUsers.toString()
        binding.tvActiveUsers.text = activeUsers.toString()
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewUsers.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewUsers.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }
    
    private fun openAddUserForm() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    
    private fun handleEditUser(user: User) {
        // TODO: Implement edit user functionality
        Toast.makeText(this, "Edit user: ${user.fullName}", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleDeleteUser(user: User) {
        val currentUser = this.currentUser ?: return
        
        // Check permissions
        if (!RoleManager.canDeleteUser(currentUser.role, user.role, currentUser.username == user.username)) {
            Toast.makeText(this, "You don't have permission to delete this user", Toast.LENGTH_SHORT).show()
            return
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.fullName}?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performUserDeletion(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun handleToggleUserStatus(user: User) {
        val currentUser = this.currentUser ?: return
        
        // Prevent user from disabling themselves
        if (currentUser.username == user.username) {
            Toast.makeText(this, "You cannot disable your own account", Toast.LENGTH_SHORT).show()
            return
        }
        
        val action = if (user.isActive) "disable" else "enable"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("${action.replaceFirstChar { it.uppercase() }} User")
            .setMessage("Are you sure you want to $action ${user.fullName}?")
            .setPositiveButton(action.replaceFirstChar { it.uppercase() }) { _, _ ->
                performStatusToggle(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performUserDeletion(user: User) {
        lifecycleScope.launch {
            try {
                database.userDao().deleteUser(user)
                runOnUiThread {
                    Toast.makeText(this@UserManagementActivity, 
                        "User ${user.fullName} deleted successfully", 
                        Toast.LENGTH_SHORT).show()
                    loadUsers() // Refresh the list
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UserManagementActivity, 
                        "Error deleting user: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun performStatusToggle(user: User) {
        lifecycleScope.launch {
            try {
                val updatedUser = user.copy(isActive = !user.isActive)
                database.userDao().updateUser(updatedUser)
                runOnUiThread {
                    val status = if (updatedUser.isActive) "enabled" else "disabled"
                    Toast.makeText(this@UserManagementActivity, 
                        "User ${user.fullName} $status successfully", 
                        Toast.LENGTH_SHORT).show()
                    loadUsers() // Refresh the list
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UserManagementActivity, 
                        "Error updating user status: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadUsers() // Refresh when returning from other activities
    }
}