package com.billgenie.model

// Display model for inventory management
data class InventoryDisplayItem(
    val id: Long = 0,
    val ingredientId: Long,
    val ingredientName: String,
    val ingredientUnit: String,
    val currentStock: Double,
    val minimumStock: Double,
    val fullQuantity: Double,
    val lastUpdated: Long,
    val notes: String?,
    val isLowStock: Boolean = currentStock <= minimumStock
) {
    // Calculate stock percentage
    val stockPercentage: Double
        get() = if (fullQuantity > 0) (currentStock / fullQuantity) * 100 else 0.0
    
    // Check if stock is critically low (15% or below)
    val isCriticallyLow: Boolean
        get() = stockPercentage <= 15.0
    
    // Helper property to get InventoryItem object
    val inventoryItem: InventoryItem
        get() = InventoryItem(
            id = id,
            ingredientId = ingredientId,
            currentStock = currentStock,
            minimumStock = minimumStock,
            fullQuantity = fullQuantity,
            lastUpdated = lastUpdated,
            notes = notes
        )
}