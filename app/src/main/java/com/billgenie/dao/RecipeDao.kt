package com.billgenie.dao

import androidx.room.*
import com.billgenie.model.Recipe
import com.billgenie.model.RecipeDisplayItem

@Dao
interface RecipeDao {
    
    @Query("SELECT * FROM recipes WHERE menuItemId = :menuItemId ORDER BY preparationStep ASC")
    suspend fun getRecipesByMenuItem(menuItemId: Long): List<Recipe>
    
    @Query("SELECT * FROM recipes WHERE ingredientId = :ingredientId")
    suspend fun getRecipesByIngredient(ingredientId: Long): List<Recipe>
    
    @Query("""
        SELECT r.id, r.menuItemId, r.ingredientId, r.quantityRequired, r.unit, 
               r.notes, r.isOptional, r.preparationStep, r.createdAt,
               i.name as ingredientName, i.unit as ingredientUnit, m.name as menuItemName
        FROM recipes r
        INNER JOIN ingredients i ON r.ingredientId = i.id
        INNER JOIN menu_items m ON r.menuItemId = m.id
        WHERE r.menuItemId = :menuItemId
        ORDER BY r.preparationStep ASC
    """)
    suspend fun getRecipeDisplayItemsByMenuItem(menuItemId: Long): List<RecipeDisplayItem>
    
    @Query("""
        SELECT r.id, r.menuItemId, r.ingredientId, r.quantityRequired, r.unit,
               r.notes, r.isOptional, r.preparationStep, r.createdAt,
               i.name as ingredientName, i.unit as ingredientUnit, m.name as menuItemName
        FROM recipes r
        INNER JOIN ingredients i ON r.ingredientId = i.id
        INNER JOIN menu_items m ON r.menuItemId = m.id
        WHERE r.ingredientId = :ingredientId
        ORDER BY m.name ASC
    """)
    suspend fun getRecipeDisplayItemsByIngredient(ingredientId: Long): List<RecipeDisplayItem>
    
    @Query("SELECT * FROM recipes WHERE menuItemId = :menuItemId AND ingredientId = :ingredientId LIMIT 1")
    suspend fun getRecipeByMenuItemAndIngredient(menuItemId: Long, ingredientId: Long): Recipe?
    
    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long
    
    @Update
    suspend fun updateRecipe(recipe: Recipe)
    
    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
    
    @Query("DELETE FROM recipes WHERE menuItemId = :menuItemId")
    suspend fun deleteRecipesByMenuItem(menuItemId: Long)
    
    @Query("DELETE FROM recipes WHERE ingredientId = :ingredientId")
    suspend fun deleteRecipesByIngredient(ingredientId: Long)
    
    @Query("""
        SELECT COUNT(*) FROM recipes r
        INNER JOIN menu_items m ON r.menuItemId = m.id
        WHERE r.ingredientId = :ingredientId AND m.isActive = 1
    """)
    suspend fun getActiveMenuItemCountForIngredient(ingredientId: Long): Int
    
    @Query("""
        SELECT SUM(r.quantityRequired) FROM recipes r
        INNER JOIN menu_items m ON r.menuItemId = m.id
        WHERE r.ingredientId = :ingredientId AND m.isActive = 1
    """)
    suspend fun getTotalQuantityRequiredForIngredient(ingredientId: Long): Double?
}