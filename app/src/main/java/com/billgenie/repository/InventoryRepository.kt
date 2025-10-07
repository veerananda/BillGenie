package com.billgenie.repository

import android.content.Context
import com.billgenie.dao.IngredientDao
import com.billgenie.dao.InventoryDao
import com.billgenie.model.InventoryDisplayItem
import com.billgenie.model.InventoryItem
import com.billgenie.utils.StockAlertManager

class InventoryRepository(
    private val ingredientDao: IngredientDao,
    private val inventoryDao: InventoryDao,
    private val context: Context? = null
) {

    private val stockAlertManager by lazy { 
        context?.let { StockAlertManager(it) }
    }

    suspend fun getAllInventoryItems(): List<InventoryDisplayItem> {
        val items = inventoryDao.getAllInventoryDisplayItems()
        // Check for critical stock levels and send notifications
        checkStockLevelsAndNotify(items)
        return items
    }

    suspend fun getLowStockItems(): List<InventoryDisplayItem> {
        return inventoryDao.getLowStockItems()
    }

    suspend fun updateInventoryQuantity(ingredientId: Long, newQuantity: Double) {
        inventoryDao.updateStock(ingredientId, newQuantity)
        // Check if this update created a critical stock situation
        checkStockAfterUpdate()
    }

    suspend fun updateMinimumStock(ingredientId: Long, minimumStock: Double) {
        inventoryDao.updateMinimumStock(ingredientId, minimumStock)
        // Check if this update affected critical stock status
        checkStockAfterUpdate()
    }

    suspend fun updateFullQuantity(ingredientId: Long, fullQuantity: Double) {
        inventoryDao.updateFullQuantity(ingredientId, fullQuantity)
        // Check if this update affected critical stock status
        checkStockAfterUpdate()
    }

    suspend fun createInventoryForIngredient(ingredientId: Long) {
        inventoryDao.createInventoryForIngredient(ingredientId)
    }

    suspend fun getInventoryByIngredient(ingredientId: Long): InventoryItem? {
        return inventoryDao.getInventoryByIngredient(ingredientId)
    }

    // Sync inventory with all ingredients used in recipes
    suspend fun syncInventoryWithIngredients() {
        // Get all unique ingredients used in recipes
        val allIngredients = ingredientDao.getAllActiveIngredients()
        
        // Create inventory entries for any missing ingredients
        for (ingredient in allIngredients) {
            val existingInventory = inventoryDao.getInventoryByIngredient(ingredient.id)
            if (existingInventory == null) {
                // Create new inventory item with 0 stock
                val inventoryItem = InventoryItem(
                    ingredientId = ingredient.id,
                    currentStock = 0.0,
                    minimumStock = 0.0
                )
                inventoryDao.insertInventoryItem(inventoryItem)
            }
        }
        
        // After sync, check for any critical stock levels
        checkStockAfterUpdate()
    }

    /**
     * Check stock levels and send notifications for critical items
     */
    private suspend fun checkStockLevelsAndNotify(items: List<InventoryDisplayItem>) {
        stockAlertManager?.checkAndSendStockAlerts(items)
    }

    /**
     * Check stock levels after an update operation
     */
    private suspend fun checkStockAfterUpdate() {
        try {
            val allItems = inventoryDao.getAllInventoryDisplayItems()
            checkStockLevelsAndNotify(allItems)
        } catch (e: Exception) {
            android.util.Log.e("InventoryRepository", "Error checking stock levels for notifications", e)
        }
    }

    /**
     * Manually trigger stock level check (useful for testing)
     */
    suspend fun checkStockLevels() {
        checkStockAfterUpdate()
    }
}