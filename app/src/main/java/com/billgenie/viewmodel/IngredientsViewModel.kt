package com.billgenie.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billgenie.model.*
import com.billgenie.repository.IngredientsRepository
import kotlinx.coroutines.launch

class IngredientsViewModel(private val repository: IngredientsRepository) : ViewModel() {
    
    private val _categories = MutableLiveData<List<MenuCategory>>()
    val categories = _categories
    
    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems = _menuItems
    
    private val _recipeIngredients = MutableLiveData<List<RecipeDisplayItem>>()
    val recipeIngredients = _recipeIngredients
    
    private val _allIngredients = MutableLiveData<List<Ingredient>>()
    val allIngredients = _allIngredients
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading = _isLoading
    
    fun loadCategories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("IngredientsViewModel", "Starting to load categories...")
                val categoriesList = repository.getAllActiveCategories()
                android.util.Log.d("IngredientsViewModel", "Loaded ${categoriesList.size} categories from repository")
                categoriesList.forEach { category ->
                    android.util.Log.d("IngredientsViewModel", "Category: ${category.name}, Active: ${category.isActive}")
                }
                _categories.value = categoriesList
            } catch (e: Exception) {
                android.util.Log.e("IngredientsViewModel", "Error loading categories", e)
                _categories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMenuItemsByCategory(category: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val items = repository.getMenuItemsByCategory(category)
                _menuItems.value = items
            } catch (e: Exception) {
                // Handle error
                _menuItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadRecipeIngredients(menuItemId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val ingredients = repository.getRecipeDisplayItemsByMenuItem(menuItemId)
                _recipeIngredients.value = ingredients
            } catch (e: Exception) {
                // Handle error
                _recipeIngredients.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadAllIngredients() {
        viewModelScope.launch {
            try {
                val ingredients = repository.getAllActiveIngredients()
                _allIngredients.value = ingredients
            } catch (e: Exception) {
                // Handle error
                _allIngredients.value = emptyList()
            }
        }
    }
    
    suspend fun findOrCreateIngredient(name: String): Ingredient {
        return repository.findOrCreateIngredient(name)
    }
    
    suspend fun addRecipe(recipe: Recipe): Long {
        return repository.insertRecipe(recipe)
    }
    
    suspend fun updateRecipe(recipe: Recipe) {
        repository.updateRecipe(recipe)
    }
    
    suspend fun deleteRecipe(recipe: Recipe) {
        repository.deleteRecipe(recipe)
    }
    
    suspend fun addIngredient(ingredient: Ingredient): Long {
        val id = repository.insertIngredient(ingredient)
        // Refresh ingredients list
        loadAllIngredients()
        return id
    }
    
    suspend fun updateIngredient(ingredient: Ingredient) {
        repository.updateIngredient(ingredient)
        // Refresh ingredients list
        loadAllIngredients()
    }
    
    suspend fun deleteIngredient(ingredient: Ingredient) {
        repository.deleteIngredient(ingredient)
        // Refresh ingredients list
        loadAllIngredients()
    }
    
    fun getIngredientUsageCount(ingredientId: Long) {
        viewModelScope.launch {
            try {
                val count = repository.getIngredientUsageCount(ingredientId)
                // Could emit this through another LiveData if needed
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}