package com.billgenie

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.lifecycle.lifecycleScope
import com.billgenie.database.BillGenieDatabase
import com.billgenie.database.MenuItemRepository
import com.billgenie.model.MenuItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddMenuItemActivity : AppCompatActivity() {
    
    private lateinit var repository: MenuItemRepository
    private lateinit var tilItemName: TextInputLayout
    private lateinit var tilItemCategory: TextInputLayout
    private lateinit var tilItemPrice: TextInputLayout
    private lateinit var etItemName: TextInputEditText
    private lateinit var actvItemCategory: AppCompatAutoCompleteTextView
    private lateinit var etItemPrice: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    
    // Predefined categories
    private val categories = listOf(
        "Beverages",
        "Appetizers", 
        "Pizza",
        "Burger",
        "Noodles",
        "Waffles",
        "Main Course",
        "Desserts",
        "Snacks",
        "General"
    )
    
    private var editingItemId: Long = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_add_menu_item)
            setupDatabase()
            setupViews()
            loadDataIfEditing()
        } catch (e: Exception) {
            // Log the error for debugging
            android.util.Log.e("AddMenuItemActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupDatabase() {
        val database = BillGenieDatabase.getDatabase(this)
        repository = MenuItemRepository(database.menuItemDao())
    }
    
    private fun setupViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            tilItemName = findViewById(R.id.tilItemName)
            tilItemCategory = findViewById(R.id.tilItemCategory)
            tilItemPrice = findViewById(R.id.tilItemPrice)
            etItemName = findViewById(R.id.etItemName)
            actvItemCategory = findViewById(R.id.actvItemCategory)
            etItemPrice = findViewById(R.id.etItemPrice)
            btnSave = findViewById(R.id.btnSave)
            btnCancel = findViewById(R.id.btnCancel)
            
            // Setup category dropdown
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            actvItemCategory.setAdapter(categoryAdapter)
            actvItemCategory.setText("General", false) // Set default category
            
            toolbar.setNavigationOnClickListener { finish() }
            
            btnSave.setOnClickListener { saveMenuItem() }
            btnCancel.setOnClickListener { finish() }
        } catch (e: Exception) {
            android.util.Log.e("AddMenuItemActivity", "Error in setupViews: ${e.message}", e)
            Toast.makeText(this, "Error setting up UI: ${e.message}", Toast.LENGTH_LONG).show()
            throw e // Re-throw to be caught by onCreate
        }
    }
    
    private fun loadDataIfEditing() {
        editingItemId = intent.getLongExtra("MENU_ITEM_ID", -1)
        
        if (editingItemId != -1L) {
            // Editing mode
            toolbar.title = "Edit Menu Item"
            lifecycleScope.launch {
                val item = repository.getMenuItemById(editingItemId)
                item?.let {
                    etItemName.setText(it.name)
                    actvItemCategory.setText(it.category, false)
                    etItemPrice.setText(it.price.toString())
                }
            }
        }
    }
    
    private fun saveMenuItem() {
        val name = etItemName.text.toString().trim()
        val category = actvItemCategory.text.toString().trim()
        val priceText = etItemPrice.text.toString().trim()
        
        // Validation
        if (name.isEmpty()) {
            tilItemName.error = getString(R.string.item_name_required)
            return
        } else {
            tilItemName.error = null
        }
        
        if (category.isEmpty()) {
            tilItemCategory.error = "Please select a category"
            return
        } else {
            tilItemCategory.error = null
        }
        
        if (priceText.isEmpty()) {
            tilItemPrice.error = getString(R.string.item_price_required)
            return
        }
        
        val price = try {
            priceText.toDouble()
        } catch (e: NumberFormatException) {
            tilItemPrice.error = getString(R.string.invalid_price)
            return
        }
        
        if (price <= 0) {
            tilItemPrice.error = getString(R.string.invalid_price)
            return
        } else {
            tilItemPrice.error = null
        }
        
        lifecycleScope.launch {
            // Check for duplicates before saving
            val duplicate = repository.findDuplicateByName(name, editingItemId)
            if (duplicate != null) {
                tilItemName.error = getString(R.string.duplicate_item_name)
                return@launch
            }
            
            if (editingItemId == -1L) {
                // Adding new item
                val menuItem = MenuItem(
                    name = name,
                    category = category,
                    price = price
                )
                repository.insertMenuItem(menuItem)
            } else {
                // Updating existing item
                val existingItem = repository.getMenuItemById(editingItemId)
                existingItem?.let { item ->
                    val updatedItem = item.copy(
                        name = name,
                        category = category,
                        price = price
                    )
                    repository.updateMenuItem(updatedItem)
                }
            }
            
            Toast.makeText(this@AddMenuItemActivity, getString(R.string.item_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}