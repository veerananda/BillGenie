package com.billgenie.dao

import androidx.room.*
import com.billgenie.model.InventoryItem
import com.billgenie.model.InventoryDisplayItem

@Dao
interface InventoryDao {
    
    @Query("SELECT * FROM inventory ORDER BY currentStock ASC")
    suspend fun getAllInventoryItems(): List<InventoryItem>
    
    @Query("""
        SELECT inv.id, inv.ingredientId, i.name as ingredientName, i.unit as ingredientUnit,
               inv.currentStock, inv.minimumStock, inv.fullQuantity, inv.lastUpdated, inv.notes,
               CASE WHEN inv.currentStock <= inv.minimumStock THEN 1 ELSE 0 END as isLowStock
        FROM inventory inv
        INNER JOIN ingredients i ON inv.ingredientId = i.id
        ORDER BY i.name ASC
    """)
    suspend fun getAllInventoryDisplayItems(): List<InventoryDisplayItem>
    
    @Query("""
        SELECT inv.id, inv.ingredientId, i.name as ingredientName, i.unit as ingredientUnit,
               inv.currentStock, inv.minimumStock, inv.fullQuantity, inv.lastUpdated, inv.notes,
               CASE WHEN inv.currentStock <= inv.minimumStock THEN 1 ELSE 0 END as isLowStock
        FROM inventory inv
        INNER JOIN ingredients i ON inv.ingredientId = i.id
        WHERE inv.currentStock <= inv.minimumStock
        ORDER BY i.name ASC
    """)
    suspend fun getLowStockItems(): List<InventoryDisplayItem>
    
    @Query("SELECT * FROM inventory WHERE ingredientId = :ingredientId LIMIT 1")
    suspend fun getInventoryByIngredient(ingredientId: Long): InventoryItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(inventoryItem: InventoryItem): Long
    
    @Update
    suspend fun updateInventoryItem(inventoryItem: InventoryItem)
    
    @Delete
    suspend fun deleteInventoryItem(inventoryItem: InventoryItem)
    
    @Query("DELETE FROM inventory WHERE ingredientId = :ingredientId")
    suspend fun deleteInventoryByIngredient(ingredientId: Long)
    
    @Query("UPDATE inventory SET currentStock = :newStock, lastUpdated = :timestamp WHERE ingredientId = :ingredientId")
    suspend fun updateStock(ingredientId: Long, newStock: Double, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE inventory SET minimumStock = :minimumStock WHERE ingredientId = :ingredientId")
    suspend fun updateMinimumStock(ingredientId: Long, minimumStock: Double)
    
    @Query("UPDATE inventory SET fullQuantity = :fullQuantity WHERE ingredientId = :ingredientId")
    suspend fun updateFullQuantity(ingredientId: Long, fullQuantity: Double)
    
    // Create inventory item for ingredient if it doesn't exist
    @Query("""
        INSERT OR IGNORE INTO inventory (ingredientId, currentStock, minimumStock, fullQuantity, lastUpdated)
        VALUES (:ingredientId, 0.0, 0.0, 100.0, :timestamp)
    """)
    suspend fun createInventoryForIngredient(ingredientId: Long, timestamp: Long = System.currentTimeMillis())
}