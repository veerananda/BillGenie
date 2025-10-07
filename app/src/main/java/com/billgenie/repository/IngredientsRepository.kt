package com.billgenie.repository

import com.billgenie.dao.IngredientDao
import com.billgenie.dao.MenuCategoryDao
import com.billgenie.dao.RecipeDao
import com.billgenie.database.MenuItemDao
import com.billgenie.model.*
import com.billgenie.utils.UnitConverter

class IngredientsRepository(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val menuItemDao: MenuItemDao,
    private val menuCategoryDao: MenuCategoryDao
) {
    
    // Ingredient operations
    suspend fun getAllActiveIngredients(): List<Ingredient> {
        return ingredientDao.getAllActiveIngredients()
    }
    
    suspend fun getIngredientsByCategory(category: String): List<Ingredient> {
        return ingredientDao.getIngredientsByCategory(category)
    }
    
    suspend fun getIngredientByName(name: String): Ingredient? {
        return ingredientDao.getIngredientByName(name)
    }
    
    suspend fun insertIngredient(ingredient: Ingredient): Long {
        return ingredientDao.insertIngredient(ingredient)
    }
    
    suspend fun updateIngredient(ingredient: Ingredient) {
        ingredientDao.updateIngredient(ingredient)
    }
    
    suspend fun deleteIngredient(ingredient: Ingredient) {
        ingredientDao.deleteIngredient(ingredient)
    }
    
    // Recipe operations
    suspend fun getRecipeDisplayItemsByMenuItem(menuItemId: Long): List<RecipeDisplayItem> {
        return recipeDao.getRecipeDisplayItemsByMenuItem(menuItemId)
    }
    
    suspend fun getRecipeByMenuItemAndIngredient(menuItemId: Long, ingredientId: Long): Recipe? {
        return recipeDao.getRecipeByMenuItemAndIngredient(menuItemId, ingredientId)
    }
    
    suspend fun insertRecipe(recipe: Recipe): Long {
        return recipeDao.insertRecipe(recipe)
    }
    
    suspend fun updateRecipe(recipe: Recipe) {
        recipeDao.updateRecipe(recipe)
    }
    
    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe)
    }
    
    // Menu item operations
    suspend fun getMenuItemsByCategory(category: String): List<MenuItem> {
        return menuItemDao.getItemsByCategorySync(category)
    }
    
    suspend fun getMenuItemById(id: Long): MenuItem? {
        return menuItemDao.getMenuItemById(id)
    }
    
    // Menu category operations
    suspend fun getAllActiveCategories(): List<MenuCategory> {
        android.util.Log.d("IngredientsRepository", "getAllActiveCategories called")
        val categories = menuCategoryDao.getAllActiveCategoriesSync()
        android.util.Log.d("IngredientsRepository", "Retrieved ${categories.size} active categories from DAO")
        return categories
    }
    
    // Combined operations
    suspend fun findOrCreateIngredient(name: String, unit: String = "pieces"): Ingredient {
        // Convert to base unit for storage
        val (_, baseUnit) = UnitConverter.toBaseUnit(1.0, unit)
        
        val existing = ingredientDao.getIngredientByName(name)
        if (existing != null) {
            // Update existing ingredient's unit if different (always use base unit)
            if (existing.unit != baseUnit) {
                val updatedIngredient = existing.copy(
                    unit = baseUnit,
                    lastUpdated = System.currentTimeMillis()
                )
                ingredientDao.updateIngredient(updatedIngredient)
                return updatedIngredient
            }
            return existing
        }
        
        // Create new ingredient with base unit
        val newIngredient = Ingredient(
            name = name,
            unit = baseUnit,
            category = "General"
        )
        val id = ingredientDao.insertIngredient(newIngredient)
        return newIngredient.copy(id = id)
    }
    
    suspend fun getIngredientUsageCount(ingredientId: Long): Int {
        return recipeDao.getActiveMenuItemCountForIngredient(ingredientId)
    }
    
    suspend fun getLowStockIngredients(): List<Ingredient> {
        return ingredientDao.getLowStockIngredients()
    }
    
    suspend fun updateIngredientStock(ingredientId: Long, newStock: Double) {
        ingredientDao.updateStock(ingredientId, newStock)
    }
}