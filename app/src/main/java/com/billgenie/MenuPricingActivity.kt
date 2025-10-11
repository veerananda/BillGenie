package com.billgenie

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.MenuCategoryAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityMenuPricingBinding
import com.billgenie.model.MenuCategory
import com.billgenie.model.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MenuPricingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMenuPricingBinding
    private lateinit var database: BillGenieDatabase
    private lateinit var adapter: MenuCategoryAdapter
    private var categories: MutableList<MenuCategory> = mutableListOf()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPricingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDatabase()
        setupRecyclerView()
        setupFab()
        loadCategories()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Update Categories"
    }
    
    private fun setupDatabase() {
        database = BillGenieDatabase.getDatabase(this)
    }
    
    private fun setupRecyclerView() {
        adapter = MenuCategoryAdapter(
            categories = categories,
            onCategoryEditClick = { category -> editCategory(category) },
            onCategoryDeleteClick = { category -> deleteCategory(category) },
            onItemAddClick = { category -> addMenuItem(category) },
            onItemEditClick = { item -> editMenuItem(item) },
            onItemDeleteClick = { item -> deleteMenuItem(item) },
            onItemVegStatusChange = { item, isVeg -> updateItemVegStatus(item, isVeg) },
            onItemEnabledStatusChange = { item, isEnabled -> updateItemEnabledStatus(item, isEnabled) }
        )
        binding.recyclerViewCategories.adapter = adapter
        binding.recyclerViewCategories.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupFab() {
        binding.fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }
    
    private fun loadCategories() {
        database.menuCategoryDao().getAllCategories().asLiveData().observe(this) { categoryList ->
            categories.clear()
            categories.addAll(categoryList)
            adapter.updateCategories(categories)
            
            // Show/hide empty state
            if (categoryList.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.recyclerViewCategories.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.recyclerViewCategories.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etCategoryName = dialogView.findViewById<TextInputEditText>(R.id.etCategoryName)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    addCategory(name)
                } else {
                    Toast.makeText(this, "Please enter category name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addCategory(name: String) {
        lifecycleScope.launch {
            // Check for duplicate category name
            val existingCategory = database.menuCategoryDao().findDuplicateCategoryByName(name)
            if (existingCategory != null) {
                Toast.makeText(this@MenuPricingActivity, "Category '$name' already exists. Please choose a different name.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            val category = MenuCategory(
                name = name,
                isActive = true
            )
            database.menuCategoryDao().insertCategory(category)
            Toast.makeText(this@MenuPricingActivity, "Category added successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun editCategory(category: MenuCategory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etCategoryName = dialogView.findViewById<TextInputEditText>(R.id.etCategoryName)
        etCategoryName.setText(category.name)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = etCategoryName.text.toString().trim()
                if (name.isNotEmpty()) {
                    updateCategory(category.copy(name = name))
                } else {
                    Toast.makeText(this, "Please enter category name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateCategory(category: MenuCategory) {
        lifecycleScope.launch {
            // Check for duplicate category name (excluding current category)
            val existingCategory = database.menuCategoryDao().findDuplicateCategoryByName(category.name, category.id)
            if (existingCategory != null) {
                Toast.makeText(this@MenuPricingActivity, "Category '${category.name}' already exists. Please choose a different name.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            database.menuCategoryDao().updateCategory(category)
            Toast.makeText(this@MenuPricingActivity, "Category updated successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteCategory(category: MenuCategory) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This will also delete all items in this category.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.menuCategoryDao().deleteCategory(category)
                    // Also delete all items in this category
                    database.menuItemDao().deleteItemsByCategory(category.name)
                    Toast.makeText(this@MenuPricingActivity, "Category deleted successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addMenuItem(category: MenuCategory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_menu_item, null)
        val etItemName = dialogView.findViewById<TextInputEditText>(R.id.etItemName)
        val etItemPrice = dialogView.findViewById<TextInputEditText>(R.id.etItemPrice)
        val switchIsVegetarian = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchIsVegetarian)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Menu Item to ${category.name}")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etItemName.text.toString().trim()
                val priceText = etItemPrice.text.toString().trim()
                val isVegetarian = switchIsVegetarian.isChecked
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (priceText.isEmpty()) {
                    Toast.makeText(this, "Please enter item price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val price = priceText.toDoubleOrNull()
                if (price == null || price <= 0) {
                    Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addMenuItem(category, name, price, isVegetarian)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addMenuItem(category: MenuCategory, name: String, price: Double, isVegetarian: Boolean) {
        lifecycleScope.launch {
            // Check for duplicate menu item name within the same category
            val existingItem = database.menuItemDao().findDuplicateByNameInCategory(name, category.name)
            if (existingItem != null) {
                Toast.makeText(this@MenuPricingActivity, "Menu item '$name' already exists in '${category.name}' category. Please choose a different name.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            val menuItem = MenuItem(
                name = name,
                price = price,
                category = category.name,
                isVegetarian = isVegetarian,
                isActive = true,
                isEnabled = true // New items are enabled by default
            )
            database.menuItemDao().insert(menuItem)
            Toast.makeText(this@MenuPricingActivity, "Menu item added successfully", Toast.LENGTH_SHORT).show()
            adapter.notifyDataSetChanged() // Refresh to show new item
        }
    }
    
    private fun editMenuItem(item: MenuItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_menu_item, null)
        val etItemName = dialogView.findViewById<TextInputEditText>(R.id.etItemName)
        val etItemPrice = dialogView.findViewById<TextInputEditText>(R.id.etItemPrice)
        val switchIsVegetarian = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchIsVegetarian)
        
        // Pre-fill with current values
        etItemName.setText(item.name)
        etItemPrice.setText(item.price.toString())
        switchIsVegetarian.isChecked = item.isVegetarian
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Menu Item")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = etItemName.text.toString().trim()
                val priceText = etItemPrice.text.toString().trim()
                val isVegetarian = switchIsVegetarian.isChecked
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (priceText.isEmpty()) {
                    Toast.makeText(this, "Please enter item price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val price = priceText.toDoubleOrNull()
                if (price == null || price <= 0) {
                    Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                updateMenuItem(item.copy(name = name, price = price, isVegetarian = isVegetarian))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateMenuItem(item: MenuItem) {
        lifecycleScope.launch {
            // Check for duplicate menu item name within the same category (excluding current item)
            val existingItem = database.menuItemDao().findDuplicateByNameInCategory(item.name, item.category, item.id)
            if (existingItem != null) {
                Toast.makeText(this@MenuPricingActivity, "Menu item '${item.name}' already exists in '${item.category}' category. Please choose a different name.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            database.menuItemDao().update(item)
            Toast.makeText(this@MenuPricingActivity, "Menu item updated successfully", Toast.LENGTH_SHORT).show()
            adapter.notifyDataSetChanged() // Refresh to show updated item
        }
    }
    
    private fun deleteMenuItem(item: MenuItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Menu Item")
            .setMessage("Are you sure you want to delete '${item.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.menuItemDao().delete(item)
                    Toast.makeText(this@MenuPricingActivity, "Menu item deleted successfully", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged() // Refresh to remove deleted item
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateItemVegStatus(item: MenuItem, isVegetarian: Boolean) {
        lifecycleScope.launch {
            val updatedItem = item.copy(isVegetarian = isVegetarian)
            database.menuItemDao().update(updatedItem)
            // No need to show toast for this quick toggle
        }
    }
    
    private fun updateItemEnabledStatus(item: MenuItem, isEnabled: Boolean) {
        lifecycleScope.launch {
            database.menuItemDao().setMenuItemEnabled(item.id, isEnabled)
            val message = if (isEnabled) "Item '${item.name}' enabled" else "Item '${item.name}' disabled"
            Toast.makeText(this@MenuPricingActivity, message, Toast.LENGTH_SHORT).show()
            adapter.notifyDataSetChanged() // Refresh to show visual changes
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}