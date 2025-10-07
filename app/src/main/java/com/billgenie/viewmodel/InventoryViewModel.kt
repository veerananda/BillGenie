package com.billgenie.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billgenie.model.InventoryDisplayItem
import com.billgenie.repository.InventoryRepository
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val _inventoryItems = MutableLiveData<List<InventoryDisplayItem>>()
    val inventoryItems = _inventoryItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading = _isLoading

    private val _error = MutableLiveData<String>()
    val error = _error

    fun loadInventoryItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                
                // First sync inventory with current ingredients
                repository.syncInventoryWithIngredients()
                
                // Then load all inventory items
                val items = repository.getAllInventoryItems()
                _inventoryItems.value = items
                
            } catch (e: Exception) {
                _error.value = "Error loading inventory: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Error loading inventory", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshInventoryList() {
        loadInventoryItems()
    }

    fun updateInventoryQuantity(ingredientId: Long, newQuantity: Double) {
        viewModelScope.launch {
            try {
                repository.updateInventoryQuantity(ingredientId, newQuantity)
                // Don't update UI immediately to prevent focus issues during editing
                // Only update database, UI will be refreshed later
            } catch (e: Exception) {
                _error.value = "Error updating quantity: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Error updating quantity", e)
            }
        }
    }

    fun updateMinimumStock(ingredientId: Long, minimumStock: Double) {
        viewModelScope.launch {
            try {
                repository.updateMinimumStock(ingredientId, minimumStock)
                // Don't update UI immediately to prevent focus issues during editing
            } catch (e: Exception) {
                _error.value = "Error updating minimum stock: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Error updating minimum stock", e)
            }
        }
    }

    fun updateFullQuantity(ingredientId: Long, fullQuantity: Double) {
        viewModelScope.launch {
            try {
                repository.updateFullQuantity(ingredientId, fullQuantity)
                // Don't update UI immediately to prevent focus issues during editing
            } catch (e: Exception) {
                _error.value = "Error updating full stock: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Error updating full stock", e)
            }
        }
    }

    private fun updateItemInCurrentList(
        ingredientId: Long, 
        newStock: Double? = null, 
        newMinStock: Double? = null, 
        newFullQuantity: Double? = null
    ) {
        val currentItems = _inventoryItems.value ?: return
        val updatedItems = currentItems.map { item ->
            if (item.ingredientId == ingredientId) {
                item.copy(
                    currentStock = newStock ?: item.currentStock,
                    minimumStock = newMinStock ?: item.minimumStock,
                    fullQuantity = newFullQuantity ?: item.fullQuantity,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                item
            }
        }
        _inventoryItems.value = updatedItems
    }

    fun getLowStockItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lowStockItems = repository.getLowStockItems()
                _inventoryItems.value = lowStockItems
            } catch (e: Exception) {
                _error.value = "Error loading low stock items: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Error loading low stock items", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkStockLevelsForNotifications() {
        viewModelScope.launch {
            try {
                repository.checkStockLevels()
            } catch (e: Exception) {
                android.util.Log.e("InventoryViewModel", "Error checking stock levels for notifications", e)
            }
        }
    }
}