package com.billgenie.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String, // e.g., "kg", "liter", "pieces", "grams"
    val costPerUnit: Double = 0.0,
    val category: String = "General", // e.g., "Vegetables", "Spices", "Meat", "Dairy"
    val description: String? = null,
    val isActive: Boolean = true,
    val minimumStock: Double = 0.0, // for inventory tracking
    val currentStock: Double = 0.0, // current available quantity
    val lastUpdated: Long = System.currentTimeMillis()
)