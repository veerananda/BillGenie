package com.billgenie.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ingredientId"], unique = true)
    ]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ingredientId: Long,
    val currentStock: Double = 0.0, // Current available quantity
    val minimumStock: Double = 0.0, // Minimum stock level for alerts
    val fullQuantity: Double = 100.0, // Full stock capacity (for percentage calculations)
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String? = null // Optional notes about the inventory item
)