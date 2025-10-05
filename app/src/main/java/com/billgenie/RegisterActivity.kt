package com.billgenie

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityRegisterBinding
import com.billgenie.model.User
import com.billgenie.utils.RoleManager
import kotlinx.coroutines.launch
import java.util.Date

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var database: BillGenieDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user has permission to register new users
        if (LoginActivity.getCurrentUserId(this) != -1 && !RoleManager.canRegisterUsers(this)) {
            Toast.makeText(this, "Access denied. Administrator privileges required to register users.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = BillGenieDatabase.getDatabase(this)
        
        // Check if this is public access (from login screen) and admin already exists
        val isPublicAccess = LoginActivity.getCurrentUserId(this) == -1
        if (isPublicAccess) {
            lifecycleScope.launch {
                val adminExists = checkIfAdminExists()
                runOnUiThread {
                    if (adminExists) {
                        // Block public registration completely
                        showRegistrationBlockedMessage()
                        return@runOnUiThread
                    } else {
                        // First-time setup: Creating admin account
                        Toast.makeText(this@RegisterActivity, 
                            "Welcome! Please create the initial administrator account to set up your system.", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            // Internal access from admin: Still creating admin accounts
            Toast.makeText(this, "Creating new administrator account", Toast.LENGTH_SHORT).show()
        }
        
        setupClickListeners()
    }
    
    private fun showRegistrationBlockedMessage() {
        // Hide all form elements and show message
        binding.tilFullName.isEnabled = false
        binding.tilUsername.isEnabled = false
        binding.tilEmail.isEnabled = false
        binding.tilPassword.isEnabled = false
        binding.tilConfirmPassword.isEnabled = false
        binding.btnRegister.isEnabled = false
        
        // Set opacity to show they're disabled
        binding.tilFullName.alpha = 0.5f
        binding.tilUsername.alpha = 0.5f
        binding.tilEmail.alpha = 0.5f
        binding.tilPassword.alpha = 0.5f
        binding.tilConfirmPassword.alpha = 0.5f
        binding.btnRegister.alpha = 0.5f
        
        Toast.makeText(this, 
            "Registration is disabled. The system has been initialized. Contact your administrator to create new accounts.", 
            Toast.LENGTH_LONG).show()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            attemptRegistration()
        }
        
        binding.tvLogin.setOnClickListener {
            // Check if we're coming from within the app or from login screen
            if (LoginActivity.getCurrentUserId(this) != -1) {
                // User is logged in, so go back to main activity
                finish()
            } else {
                // User is not logged in, so go back to login
                finish()
            }
        }
        
        // Update UI based on context
        updateUIForContext()
        
        // Enable/disable register button based on admin role check
        checkAdminPrivileges()
    }
    
    private fun updateUIForContext() {
        // If user is already logged in, this is admin creating a new user
        if (LoginActivity.getCurrentUserId(this) != -1) {
            binding.tvLogin.text = "← Back to Main"
            // Could add a title or header indicating this is admin user creation
        } else {
            binding.tvLogin.text = "Already have an account? Login"
        }
    }
    
    private fun checkAdminPrivileges() {
        // Registration is only for admin accounts now
        val currentUserRole = LoginActivity.getCurrentUserRole(this)
        if (currentUserRole != "ADMIN" && LoginActivity.getCurrentUserId(this) != -1) {
            // If not admin and logged in, show error and finish
            Toast.makeText(this, "Only administrators can create new accounts", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun attemptRegistration() {
        val fullName = binding.etFullName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        if (validateInput(fullName, username, email, password, confirmPassword)) {
            performRegistration(fullName, username, email, password, "ADMIN")
        }
    }
    
    private fun validateInput(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true
        
        // Full Name validation
        if (TextUtils.isEmpty(fullName)) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        } else if (fullName.length < 2) {
            binding.tilFullName.error = "Full name must be at least 2 characters"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }
        
        // Username validation
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            binding.tilUsername.error = "Username must be at least 3 characters"
            isValid = false
        } else if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            binding.tilUsername.error = "Username can only contain letters, numbers, and underscores"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }
        
        // Email validation (optional)
        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email address"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }
        
        // Password validation
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        // Confirm Password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }
        
        return isValid
    }
    
    private fun performRegistration(
        fullName: String,
        username: String,
        email: String,
        password: String,
        role: String
    ) {
        lifecycleScope.launch {
            try {
                val isPublicAccess = LoginActivity.getCurrentUserId(this@RegisterActivity) == -1
                val adminExists = checkIfAdminExists()
                
                // Enhanced security checks
                if (isPublicAccess && adminExists) {
                    // Block all public registration once admin exists
                    Toast.makeText(this@RegisterActivity, 
                        "Public registration is disabled. Contact your administrator to create new accounts.", 
                        Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (isPublicAccess && !adminExists && role != RoleManager.ROLE_ADMIN) {
                    // First-time setup must be admin
                    Toast.makeText(this@RegisterActivity, 
                        "Initial setup requires administrator account creation.", 
                        Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (role == RoleManager.ROLE_ADMIN && adminExists && isPublicAccess) {
                    // Prevent public admin registration if admin already exists
                    Toast.makeText(this@RegisterActivity, 
                        "Admin registration not allowed. Contact existing administrator.", 
                        Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // Check if username already exists
                val existingUser = database.userDao().getUserByUsername(username)
                if (existingUser != null) {
                    binding.tilUsername.error = "Username already exists"
                    return@launch
                }
                
                // Check if email already exists (only if email is provided)
                if (!TextUtils.isEmpty(email)) {
                    val existingEmail = database.userDao().getUserByEmail(email)
                    if (existingEmail != null) {
                        binding.tilEmail.error = "Email already registered"
                        return@launch
                    }
                }
                
                // Create new user
                val newUser = User(
                    username = username,
                    password = password, // In production, this should be hashed
                    fullName = fullName,
                    email = if (TextUtils.isEmpty(email)) null else email,
                    role = role,
                    isActive = true,
                    createdAt = Date(),
                    lastLoginAt = null
                )
                
                val userId = database.userDao().insertUser(newUser)
                
                if (userId > 0) {
                    Toast.makeText(this@RegisterActivity, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                    finish() // Go back to login
                } else {
                    Toast.makeText(this@RegisterActivity, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun checkIfAdminExists(): Boolean {
        return try {
            val users = database.userDao().getAllUsersOnce()
            users.any { it.role == RoleManager.ROLE_ADMIN && it.isActive }
        } catch (e: Exception) {
            false
        }
    }
}