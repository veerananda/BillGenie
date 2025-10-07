package com.billgenie.dao

import androidx.room.*
import com.billgenie.model.MenuCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuCategoryDao {
    
    @Query("SELECT * FROM menu_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<MenuCategory>>
    
    @Query("SELECT * FROM menu_categories WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveCategoriesSync(): List<MenuCategory>
    
    @Query("SELECT * FROM menu_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<MenuCategory>>
    
    @Query("SELECT * FROM menu_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): MenuCategory?
    
    @Query("SELECT * FROM menu_categories WHERE LOWER(name) = LOWER(:name) AND isActive = 1 AND id != :excludeId")
    suspend fun findDuplicateCategoryByName(name: String, excludeId: Long = -1): MenuCategory?
    
    @Insert
    suspend fun insertCategory(category: MenuCategory): Long
    
    @Update
    suspend fun updateCategory(category: MenuCategory)
    
    @Delete
    suspend fun deleteCategory(category: MenuCategory)
    
    @Query("UPDATE menu_categories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: Long)
}