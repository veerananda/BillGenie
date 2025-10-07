package com.billgenie.dao

import androidx.room.*
import com.billgenie.model.Ingredient

@Dao
interface IngredientDao {
    
    @Query("SELECT * FROM ingredients WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveIngredients(): List<Ingredient>
    
    @Query("SELECT * FROM ingredients WHERE category = :category AND isActive = 1 ORDER BY name ASC")
    suspend fun getIngredientsByCategory(category: String): List<Ingredient>
    
    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getIngredientByName(name: String): Ingredient?
    
    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): Ingredient?
    
    @Insert
    suspend fun insertIngredient(ingredient: Ingredient): Long
    
    @Update
    suspend fun updateIngredient(ingredient: Ingredient)
    
    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)
    
    @Query("UPDATE ingredients SET isActive = 0 WHERE id = :id")
    suspend fun deactivateIngredient(id: Long)
    
    @Query("SELECT DISTINCT category FROM ingredients WHERE isActive = 1 ORDER BY category ASC")
    suspend fun getAllIngredientCategories(): List<String>
    
    @Query("UPDATE ingredients SET currentStock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Double)
    
    @Query("SELECT * FROM ingredients WHERE currentStock <= minimumStock AND isActive = 1")
    suspend fun getLowStockIngredients(): List<Ingredient>
}