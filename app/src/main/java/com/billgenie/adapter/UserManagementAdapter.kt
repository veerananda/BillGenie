package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.User
import com.billgenie.utils.RoleManager
import java.text.SimpleDateFormat
import java.util.*

class UserManagementAdapter(
    private var users: List<User>,
    private val currentUser: User,
    private val onToggleStatusClick: (User) -> Unit,
    private val onEditUserClick: (User) -> Unit,
    private val onDeleteUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserIcon: TextView = itemView.findViewById(R.id.tvUserIcon)
        val tvFullName: TextView = itemView.findViewById(R.id.tvFullName)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvRole: TextView = itemView.findViewById(R.id.tvRole)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvLastLogin: TextView = itemView.findViewById(R.id.tvLastLogin)
        val btnToggleStatus: Button = itemView.findViewById(R.id.btnToggleStatus)
        val btnEditUser: Button = itemView.findViewById(R.id.btnEditUser)
        val btnDeleteUser: Button = itemView.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_management, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        // Set user icon based on role
        holder.tvUserIcon.text = when (user.role) {
            RoleManager.ROLE_ADMIN -> "👑"
            RoleManager.ROLE_MANAGER -> "👨‍💼"
            RoleManager.ROLE_STAFF -> "👤"
            else -> "❓"
        }
        
        // Set user details
        holder.tvFullName.text = user.fullName
        holder.tvUsername.text = "@${user.username}"
        holder.tvEmail.text = user.email ?: "No email"
        
        // Set role with color
        holder.tvRole.text = user.role
        when (user.role) {
            RoleManager.ROLE_ADMIN -> {
                holder.tvRole.setBackgroundColor(holder.itemView.context.getColor(R.color.error_color))
            }
            RoleManager.ROLE_MANAGER -> {
                holder.tvRole.setBackgroundColor(holder.itemView.context.getColor(R.color.primary_color))
            }
            RoleManager.ROLE_STAFF -> {
                holder.tvRole.setBackgroundColor(holder.itemView.context.getColor(R.color.success_color))
            }
        }
        
        // Set status
        if (user.isActive) {
            holder.tvStatus.text = "🟢 Active"
            holder.btnToggleStatus.text = "Deactivate"
        } else {
            holder.tvStatus.text = "🔴 Inactive"
            holder.btnToggleStatus.text = "Activate"
        }
        
        // Set last login
        if (user.lastLoginAt != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            holder.tvLastLogin.text = "Last login: ${dateFormat.format(user.lastLoginAt)}"
        } else {
            holder.tvLastLogin.text = "Last login: Never"
        }
        
        // Determine if current user can delete this user
        val canDelete = canDeleteUser(currentUser, user)
        
        // Debug logging
        android.util.Log.d("UserManagement", "User: ${user.fullName} (@${user.username}), Role: ${user.role}")
        android.util.Log.d("UserManagement", "Current user: ${currentUser.fullName} (@${currentUser.username}), Role: ${currentUser.role}")
        android.util.Log.d("UserManagement", "Can delete: $canDelete, Is same user: ${user.id == currentUser.id}")
        
        // Show/hide delete button based on permissions
        if (canDelete) {
            holder.btnDeleteUser.visibility = View.VISIBLE
            android.util.Log.d("UserManagement", "SHOWING delete button for ${user.username}")
        } else {
            holder.btnDeleteUser.visibility = View.GONE
            android.util.Log.d("UserManagement", "HIDING delete button for ${user.username}")
        }
        
        // Prevent users from deleting themselves
        if (user.id == currentUser.id) {
            holder.btnDeleteUser.visibility = View.GONE
            android.util.Log.d("UserManagement", "Hiding delete button - user cannot delete themselves")
        }
        
        // Set click listeners
        holder.btnToggleStatus.setOnClickListener {
            onToggleStatusClick(user)
        }
        
        holder.btnEditUser.setOnClickListener {
            onEditUserClick(user)
        }
        
        holder.btnDeleteUser.setOnClickListener {
            onDeleteUserClick(user)
        }
    }
    
    private fun canDeleteUser(currentUser: User, targetUser: User): Boolean {
        android.util.Log.d("UserManagement", "Checking delete permission - Current: ${currentUser.role}, Target: ${targetUser.role}")
        
        // Only admins can delete users
        if (currentUser.role != RoleManager.ROLE_ADMIN) {
            android.util.Log.d("UserManagement", "Cannot delete - current user is not admin")
            return false
        }
        
        // Admins can delete any user type including other admins
        return when (targetUser.role) {
            RoleManager.ROLE_STAFF -> {
                android.util.Log.d("UserManagement", "Can delete - target is staff")
                true
            }
            RoleManager.ROLE_MANAGER -> {
                android.util.Log.d("UserManagement", "Can delete - target is manager")
                true
            }
            RoleManager.ROLE_ADMIN -> {
                android.util.Log.d("UserManagement", "Can delete - target is admin (other than self)")
                true // Allow admin deletion of other admins
            }
            else -> {
                android.util.Log.d("UserManagement", "Cannot delete - unknown role")
                false
            }
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}