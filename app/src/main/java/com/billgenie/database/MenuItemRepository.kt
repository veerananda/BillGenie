package com.billgenie.database

import androidx.lifecycle.LiveData
import com.billgenie.model.MenuItem

class MenuItemRepository(private val dao: MenuItemDao) {
    
    fun getAllMenuItems(): LiveData<List<MenuItem>> = dao.getAllMenuItems()
    
    fun getEnabledMenuItems(): LiveData<List<MenuItem>> = dao.getEnabledMenuItems()
    
    suspend fun getMenuItemById(id: Long): MenuItem? = dao.getMenuItemById(id)
    
    suspend fun findDuplicateByName(name: String, excludeId: Long = -1): MenuItem? = 
        dao.findDuplicateByName(name, excludeId)
    
    suspend fun insertMenuItem(menuItem: MenuItem): Long = dao.insertMenuItem(menuItem)
    
    suspend fun updateMenuItem(menuItem: MenuItem) = dao.updateMenuItem(menuItem)
    
    suspend fun deleteMenuItem(menuItem: MenuItem) = dao.deleteMenuItem(menuItem)
    
    suspend fun softDeleteMenuItem(id: Long) = dao.softDeleteMenuItem(id)
    
    suspend fun getActiveItemCount(): Int = dao.getActiveItemCount()
}