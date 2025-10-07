package com.billgenie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityLoginBinding
import com.billgenie.model.User
import kotlinx.coroutines.launch
import java.util.Date

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: BillGenieDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = BillGenieDatabase.getDatabase(this)
        
        // Check if already logged in
        if (isUserLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupClickListeners()
        checkRegistrationAvailability()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun checkRegistrationAvailability() {
        lifecycleScope.launch {
            try {
                // Check if any admin users exist
                val users = database.userDao().getAllUsersOnce()
                val adminExists = users.any { it.role == "ADMIN" && it.isActive }
                
                runOnUiThread {
                    if (adminExists) {
                        // Hide register button if admin exists
                        binding.tvRegister.visibility = android.view.View.GONE
                    } else {
                        // Show register button for initial setup
                        binding.tvRegister.visibility = android.view.View.VISIBLE
                        binding.tvRegister.text = "Create Admin Account"
                    }
                }
            } catch (e: Exception) {
                // On error, show register button (fail-safe)
                runOnUiThread {
                    binding.tvRegister.visibility = android.view.View.VISIBLE
                    binding.tvRegister.text = "Create Admin Account"
                }
            }
        }
    }
    
    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (validateInput(username, password)) {
            performLogin(username, password)
        }
    }
    
    private fun validateInput(username: String, password: String): Boolean {
        var isValid = true
        
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        return isValid
    }
    
    private fun performLogin(username: String, password: String) {
        lifecycleScope.launch {
            try {
                val user = database.userDao().authenticateUser(username, password)
                
                if (user != null) {
                    if (user.isActive) {
                        // Update last login time
                        database.userDao().updateLastLogin(user.id, Date())
                        
                        // Save login session
                        saveUserSession(user)
                        
                        // Navigate to main activity
                        navigateToMain()
                        
                        Toast.makeText(this@LoginActivity, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LoginActivity, "Your account is deactivated. Please contact admin.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    binding.tilPassword.error = "Invalid credentials"
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveUserSession(user: User) {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("user_id", user.id)
            putString("username", user.username)
            putString("full_name", user.fullName)
            putString("role", user.role)
            putBoolean("is_logged_in", true)
            apply()
        }
    }
    
    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    

    
    companion object {
        fun getCurrentUserId(context: Context): Int {
            val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            return sharedPref.getInt("user_id", -1)
        }
        
        fun getCurrentUserRole(context: Context): String {
            val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            return sharedPref.getString("role", "STAFF") ?: "STAFF"
        }
        
        fun logout(context: Context) {
            val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear()
                apply()
            }
        }
    }
}