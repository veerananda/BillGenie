package com.billgenie.utils

import android.content.Context
import com.billgenie.LoginActivity

object RoleManager {
    
    // Role constants
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_MANAGER = "MANAGER"
    const val ROLE_STAFF = "STAFF"
    
    /**
     * Get current user's role
     */
    fun getCurrentUserRole(context: Context): String {
        return LoginActivity.getCurrentUserRole(context)
    }
    
    /**
     * Check if current user is admin
     */
    fun isAdmin(context: Context): Boolean {
        return getCurrentUserRole(context) == ROLE_ADMIN
    }
    
    /**
     * Check if current user is manager or above
     */
    fun isManagerOrAbove(context: Context): Boolean {
        val role = getCurrentUserRole(context)
        return role == ROLE_ADMIN || role == ROLE_MANAGER
    }
    
    /**
     * Check if current user is staff only
     */
    fun isStaffOnly(context: Context): Boolean {
        return getCurrentUserRole(context) == ROLE_STAFF
    }
    
    /**
     * Check if user can access menu management
     */
    fun canAccessMenuManagement(context: Context): Boolean {
        return isManagerOrAbove(context)
    }
    
    /**
     * Check if user can access backup functionality
     */
    fun canAccessBackup(context: Context): Boolean {
        return isAdmin(context)
    }
    
    /**
     * Check if user can access app information
     */
    fun canAccessAppInformation(context: Context): Boolean {
        return isManagerOrAbove(context)
    }
    
    /**
     * Check if user can register new users
     */
    fun canRegisterUsers(context: Context): Boolean {
        return isAdmin(context)
    }
    
    /**
     * Check if current user can edit target user
     */
    fun canEditUser(currentUserRole: String, targetUserRole: String): Boolean {
        return when (currentUserRole) {
            ROLE_ADMIN -> true // Admin can edit anyone
            ROLE_MANAGER -> targetUserRole == ROLE_STAFF // Manager can edit staff only
            else -> false // Staff cannot edit users
        }
    }
    
    /**
     * Check if current user can delete target user
     */
    fun canDeleteUser(currentUserRole: String, targetUserRole: String, isSameUser: Boolean): Boolean {
        // Users cannot delete themselves
        if (isSameUser) return false
        
        return when (currentUserRole) {
            ROLE_ADMIN -> true // Admin can delete anyone except themselves
            ROLE_MANAGER -> targetUserRole == ROLE_STAFF // Manager can delete staff only
            else -> false // Staff cannot delete users
        }
    }
    
    /**
     * Get role display name
     */
    fun getRoleDisplayName(role: String): String {
        return when (role) {
            ROLE_ADMIN -> "Administrator"
            ROLE_MANAGER -> "Manager"
            ROLE_STAFF -> "Staff"
            else -> "Unknown"
        }
    }
    
    /**
     * Get permissions description for role
     */
    fun getPermissionsDescription(role: String): String {
        return when (role) {
            ROLE_ADMIN -> "Full access to all features"
            ROLE_MANAGER -> "Access to orders, billing, and menu management"
            ROLE_STAFF -> "Access to orders and billing only"
            else -> "No permissions"
        }
    }
}