package com.billgenie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.User
import com.billgenie.utils.RoleManager

class UserManagementModernAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit,
    private val onToggleStatusClick: (User) -> Unit
) : RecyclerView.Adapter<UserManagementModernAdapter.UserViewHolder>() {

    private var users = listOf<User>()
    private var currentUser: User? = null

    fun submitList(newUsers: List<User>, currentUser: User?) {
        this.users = newUsers
        this.currentUser = currentUser
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_modern, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserRole: TextView = itemView.findViewById(R.id.tvUserRole)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvUserStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
        private val ivUserAvatar: TextView = itemView.findViewById(R.id.ivUserAvatar)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val ivToggleStatus: ImageView = itemView.findViewById(R.id.ivToggleStatus)

        fun bind(user: User) {
            tvUserName.text = user.fullName
            tvUserRole.text = getRoleDisplayName(user.role)
            tvUserEmail.text = if (!user.email.isNullOrEmpty()) user.email else "No email"
            
            // Set status
            if (user.isActive) {
                tvUserStatus.text = "Active"
                tvUserStatus.setBackgroundResource(R.drawable.status_active_background)
                tvUserStatus.setTextColor(itemView.context.getColor(R.color.status_active_text))
            } else {
                tvUserStatus.text = "Inactive"
                tvUserStatus.setBackgroundResource(R.drawable.status_inactive_background)
                tvUserStatus.setTextColor(itemView.context.getColor(R.color.status_inactive_text))
            }
            
            // Set avatar based on role
            ivUserAvatar.text = when (user.role) {
                RoleManager.ROLE_ADMIN -> "👨‍💼"
                RoleManager.ROLE_MANAGER -> "👩‍💼"
                RoleManager.ROLE_STAFF -> "👨‍🍳"
                else -> "👤"
            }
            
            // Set button visibility based on permissions
            val currentUser = this@UserManagementModernAdapter.currentUser
            if (currentUser != null) {
                val canEdit = RoleManager.canEditUser(currentUser.role, user.role)
                val canDelete = RoleManager.canDeleteUser(currentUser.role, user.role, currentUser.username == user.username)
                val canToggleStatus = currentUser.username != user.username // Can't toggle own status
                
                ivEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
                ivDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
                ivToggleStatus.visibility = if (canToggleStatus) View.VISIBLE else View.GONE
                
                // Set toggle status icon
                if (user.isActive) {
                    ivToggleStatus.setImageResource(R.drawable.ic_visibility_off)
                    ivToggleStatus.contentDescription = "Disable user"
                } else {
                    ivToggleStatus.setImageResource(R.drawable.ic_visibility)
                    ivToggleStatus.contentDescription = "Enable user"
                }
            }
            
            // Set click listeners
            ivEdit.setOnClickListener { onEditClick(user) }
            ivDelete.setOnClickListener { onDeleteClick(user) }
            ivToggleStatus.setOnClickListener { onToggleStatusClick(user) }
        }
        
        private fun getRoleDisplayName(role: String): String = when (role) {
            RoleManager.ROLE_ADMIN -> "Administrator"
            RoleManager.ROLE_MANAGER -> "Manager"
            RoleManager.ROLE_STAFF -> "Staff Member"
            else -> role
        }
    }
}