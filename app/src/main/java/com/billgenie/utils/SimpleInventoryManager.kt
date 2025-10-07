package com.billgenie.utils

import android.util.Log
import com.billgenie.database.BillGenieDatabase
import com.billgenie.model.BillItemDisplay
import com.billgenie.model.CustomerOrder
import com.billgenie.repository.InventoryRepository
import com.billgenie.repository.IngredientsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple inventory manager that updates inventory immediately when an order is saved.
 * No background tasks, no schema changes, just direct inventory updates.
 */
class SimpleInventoryManager(private val database: BillGenieDatabase) {
    
    private val inventoryRepository = InventoryRepository(
        ingredientDao = database.ingredientDao(),
        inventoryDao = database.inventoryDao()
    )
    
    private val ingredientsRepository = IngredientsRepository(
        ingredientDao = database.ingredientDao(),
        recipeDao = database.recipeDao(),
        menuItemDao = database.menuItemDao(),
        menuCategoryDao = database.menuCategoryDao()
    )
    
    /**
     * Process inventory deduction for a customer order immediately
     */
    suspend fun processOrderInventoryDeduction(customerOrder: CustomerOrder): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SimpleInventoryManager", "Processing inventory deduction for customer ${customerOrder.customerNumber}")
                
                var success = true
                val deductionSummary = mutableListOf<String>()
                
                // Process each order item
                for (orderItem in customerOrder.orderItems) {
                    val itemDeductionResult = processOrderItemInventoryDeduction(orderItem)
                    if (itemDeductionResult.isNotEmpty()) {
                        deductionSummary.addAll(itemDeductionResult)
                    }
                }
                
                if (deductionSummary.isNotEmpty()) {
                    Log.d("SimpleInventoryManager", "Inventory deductions completed:")
                    deductionSummary.forEach { Log.d("SimpleInventoryManager", "  - $it") }
                }
                
                success
            } catch (e: Exception) {
                Log.e("SimpleInventoryManager", "Error processing inventory deduction for order", e)
                false
            }
        }
    }
    
    /**
     * Process inventory deduction for a single order item
     */
    private suspend fun processOrderItemInventoryDeduction(orderItem: BillItemDisplay): List<String> {
        val deductionResults = mutableListOf<String>()
        
        try {
            // Get the recipe for this menu item
            val recipeItems = ingredientsRepository.getRecipeDisplayItemsByMenuItem(orderItem.menuItemId)
            
            if (recipeItems.isEmpty()) {
                Log.w("SimpleInventoryManager", "No recipe found for menu item: ${orderItem.itemName}")
                return deductionResults
            }
            
            Log.d("SimpleInventoryManager", "Processing ${orderItem.quantity}x ${orderItem.itemName}")
            
            // Process each ingredient in the recipe
            for (recipeItem in recipeItems) {
                val totalQuantityNeeded = recipeItem.quantityRequired * orderItem.quantity
                
                // Get current inventory for this ingredient
                val inventoryItem = inventoryRepository.getInventoryByIngredient(recipeItem.ingredientId)
                
                if (inventoryItem == null) {
                    Log.w("SimpleInventoryManager", "No inventory found for ingredient: ${recipeItem.ingredientName}")
                    // Create inventory entry with 0 stock if it doesn't exist
                    inventoryRepository.createInventoryForIngredient(recipeItem.ingredientId)
                    continue
                }
                
                // Check if we have enough stock
                if (inventoryItem.currentStock >= totalQuantityNeeded) {
                    // Deduct the required quantity
                    val newStock = inventoryItem.currentStock - totalQuantityNeeded
                    inventoryRepository.updateInventoryQuantity(recipeItem.ingredientId, newStock)
                    
                    val result = "Deducted ${totalQuantityNeeded} ${recipeItem.unit} of ${recipeItem.ingredientName} (${inventoryItem.currentStock} → ${newStock})"
                    deductionResults.add(result)
                    Log.d("SimpleInventoryManager", result)
                } else {
                    // Log insufficient stock but don't block the order
                    val warning = "Insufficient stock for ${recipeItem.ingredientName}: needed ${totalQuantityNeeded} ${recipeItem.unit}, available ${inventoryItem.currentStock} ${recipeItem.unit}"
                    Log.w("SimpleInventoryManager", warning)
                    
                    // Still deduct what we can (this could go negative, which might be acceptable for tracking purposes)
                    val newStock = inventoryItem.currentStock - totalQuantityNeeded
                    inventoryRepository.updateInventoryQuantity(recipeItem.ingredientId, newStock)
                    
                    val result = "⚠️ Deducted ${totalQuantityNeeded} ${recipeItem.unit} of ${recipeItem.ingredientName} (${inventoryItem.currentStock} → ${newStock}) - INSUFFICIENT STOCK"
                    deductionResults.add(result)
                }
            }
            
        } catch (e: Exception) {
            Log.e("SimpleInventoryManager", "Error processing inventory for order item: ${orderItem.itemName}", e)
        }
        
        return deductionResults
    }
}