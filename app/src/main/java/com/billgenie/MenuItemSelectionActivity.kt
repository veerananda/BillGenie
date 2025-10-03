package com.billgenie

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.MenuItemAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityMenuItemSelectionBinding
import com.billgenie.model.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class MenuItemSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMenuItemSelectionBinding
    private lateinit var database: BillGenieDatabase
    private lateinit var menuItemAdapter: MenuItemAdapter
    private var categoryName: String = ""
    private var categoryId: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuItemSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = BillGenieDatabase.getDatabase(this)
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""
        categoryId = intent.getLongExtra("CATEGORY_ID", 0)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadMenuItems()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "$categoryName - Menu Items"
    }
    
    private fun setupRecyclerView() {
        menuItemAdapter = MenuItemAdapter(
            onEditClick = { menuItem -> showEditItemDialog(menuItem) },
            onDeleteClick = { menuItem -> showDeleteItemConfirmation(menuItem) }
        )
        
        binding.recyclerViewMenuItems.apply {
            layoutManager = LinearLayoutManager(this@MenuItemSelectionActivity)
            adapter = menuItemAdapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddItem.setOnClickListener {
            showAddItemDialog()
        }
    }
    
    private fun loadMenuItems() {
        lifecycleScope.launch {
            database.menuItemDao().getItemsByCategory(categoryName).collect { menuItems ->
                menuItemAdapter.submitList(menuItems)
                
                // Show/hide empty state
                if (menuItems.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewMenuItems.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerViewMenuItems.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun showAddItemDialog() {
        // TODO: Implement add item dialog
        Toast.makeText(this, "Add item functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showEditItemDialog(menuItem: MenuItem) {
        // TODO: Implement edit item dialog  
        Toast.makeText(this, "Edit functionality coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeleteItemConfirmation(menuItem: MenuItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Menu Item")
            .setMessage("Are you sure you want to delete '${menuItem.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMenuItem(menuItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addMenuItem(name: String, price: Double, isVegetarian: Boolean) {
        lifecycleScope.launch {
            try {
                val menuItem = MenuItem(
                    name = name,
                    price = price,
                    category = categoryName,
                    isVegetarian = isVegetarian
                )
                database.menuItemDao().insert(menuItem)
                Toast.makeText(this@MenuItemSelectionActivity, "Item added successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MenuItemSelectionActivity, "Error adding item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateMenuItem(menuItem: MenuItem) {
        lifecycleScope.launch {
            try {
                database.menuItemDao().update(menuItem)
                Toast.makeText(this@MenuItemSelectionActivity, "Item updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MenuItemSelectionActivity, "Error updating item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteMenuItem(menuItem: MenuItem) {
        lifecycleScope.launch {
            try {
                database.menuItemDao().delete(menuItem)
                Toast.makeText(this@MenuItemSelectionActivity, "Item deleted successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MenuItemSelectionActivity, "Error deleting item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}