package com.billgenie.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String, // In production, this should be hashed
    val fullName: String,
    val email: String? = null,
    val role: String = "ADMIN", // ADMIN, MANAGER, STAFF
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val lastLoginAt: Date? = null
)