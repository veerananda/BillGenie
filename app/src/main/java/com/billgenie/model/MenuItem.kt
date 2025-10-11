package com.billgenie.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String = "General",
    val description: String? = null,
    val isActive: Boolean = true,
    val isVegetarian: Boolean = true,
    val isEnabled: Boolean = true, // New field for enable/disable functionality
    val dateAdded: Long = System.currentTimeMillis()
)