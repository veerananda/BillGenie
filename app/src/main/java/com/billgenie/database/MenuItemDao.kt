package com.billgenie.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.billgenie.model.MenuItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items WHERE isActive = 1 ORDER BY dateAdded DESC")
    fun getAllMenuItems(): LiveData<List<MenuItem>>
    
    @Query("SELECT * FROM menu_items WHERE isActive = 1 AND isEnabled = 1 ORDER BY dateAdded DESC")
    fun getEnabledMenuItems(): LiveData<List<MenuItem>>
    
    @Query("SELECT * FROM menu_items WHERE category = :category AND isActive = 1 ORDER BY dateAdded DESC")
    fun getItemsByCategory(category: String): Flow<List<MenuItem>>
    
    @Query("SELECT * FROM menu_items WHERE category = :category AND isActive = 1 AND isEnabled = 1 ORDER BY dateAdded DESC")
    fun getEnabledItemsByCategory(category: String): Flow<List<MenuItem>>
    
    @Query("SELECT * FROM menu_items WHERE isActive = 1 AND isEnabled = 1 ORDER BY category ASC, name ASC")
    suspend fun getAllEnabledMenuItemsSync(): List<MenuItem>
    
    @Query("SELECT * FROM menu_items WHERE category = :category AND isActive = 1 ORDER BY dateAdded DESC")
    suspend fun getItemsByCategorySync(category: String): List<MenuItem>
    
    @Query("SELECT * FROM menu_items WHERE category = :category AND isActive = 1 AND isEnabled = 1 ORDER BY dateAdded DESC")
    suspend fun getEnabledItemsByCategorySync(category: String): List<MenuItem>

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getMenuItemById(id: Long): MenuItem?
    
    @Query("SELECT * FROM menu_items WHERE LOWER(name) = LOWER(:name) AND isActive = 1 LIMIT 1")
    suspend fun getMenuItemByName(name: String): MenuItem?

    @Query("SELECT * FROM menu_items WHERE LOWER(name) = LOWER(:name) AND isActive = 1 AND id != :excludeId")
    suspend fun findDuplicateByName(name: String, excludeId: Long = -1): MenuItem?
    
    @Query("SELECT * FROM menu_items WHERE LOWER(name) = LOWER(:name) AND category = :category AND isActive = 1 AND id != :excludeId")
    suspend fun findDuplicateByNameInCategory(name: String, category: String, excludeId: Long = -1): MenuItem?

    @Insert
    suspend fun insert(menuItem: MenuItem): Long
    
    @Insert
    suspend fun insertMenuItem(menuItem: MenuItem): Long

    @Update
    suspend fun update(menuItem: MenuItem)
    
    @Update
    suspend fun updateMenuItem(menuItem: MenuItem)

    @Delete
    suspend fun delete(menuItem: MenuItem)
    
    @Delete
    suspend fun deleteMenuItem(menuItem: MenuItem)
    
    @Query("DELETE FROM menu_items WHERE category = :category")
    suspend fun deleteItemsByCategory(category: String)

    @Query("UPDATE menu_items SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteMenuItem(id: Long)
    
    @Query("UPDATE menu_items SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setMenuItemEnabled(id: Long, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM menu_items WHERE isActive = 1")
    suspend fun getActiveItemCount(): Int
    
    @Query("DELETE FROM menu_items WHERE isActive = 0 AND dateAdded < :cutoffTimestamp")
    suspend fun cleanupOldInactiveItems(cutoffTimestamp: Long)
    
    // Method for monthly backup statistics
    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getTotalCount(): Int
}