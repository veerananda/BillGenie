package com.billgenie

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.MenuSelectionAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.database.MenuItemRepository
import com.billgenie.databinding.ActivityMenuSelectionBinding
import com.billgenie.model.MenuItem
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class MenuSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMenuSelectionBinding
    private lateinit var menuSelectionAdapter: MenuSelectionAdapter
    private lateinit var menuItemRepository: MenuItemRepository
    
    private val selectedMenuItems = mutableListOf<MenuItem>()
    private val selectedItems = mutableListOf<MenuItem>()
    private val allMenuItems = mutableListOf<MenuItem>()
    private var selectedCategories = mutableSetOf<String>()
    
    // Bill mode flag - when true, shows "View Order" instead of "Done"
    private var isBillMode = false
    
    // Category filtering
    private var selectedCategoryId: Long = -1
    private var selectedCategoryName: String = ""
    
    // Rupee formatter
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            android.util.Log.d("MenuSelectionActivity", "Starting onCreate")
            
            // Check if this is bill mode
            isBillMode = intent.getBooleanExtra("bill_mode", false)
            android.util.Log.d("MenuSelectionActivity", "Bill mode: $isBillMode")
            
            // Get category filtering info
            selectedCategoryId = intent.getLongExtra("category_id", -1)
            selectedCategoryName = intent.getStringExtra("category_name") ?: ""
            android.util.Log.d("MenuSelectionActivity", "Category filter: ID=$selectedCategoryId, Name=$selectedCategoryName")
            
            binding = ActivityMenuSelectionBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            android.util.Log.d("MenuSelectionActivity", "Layout inflated successfully")
            setupToolbar()
            android.util.Log.d("MenuSelectionActivity", "Toolbar setup complete")
            setupDatabase()
            android.util.Log.d("MenuSelectionActivity", "Database setup complete")
            setupRecyclerView()
            android.util.Log.d("MenuSelectionActivity", "RecyclerView setup complete")
            setupSearchFunctionality()
            android.util.Log.d("MenuSelectionActivity", "Search setup complete")
            setupCategoryFilters()
            android.util.Log.d("MenuSelectionActivity", "Category filters setup complete")
            setupClickListeners()
            android.util.Log.d("MenuSelectionActivity", "Click listeners setup complete")
            loadMenuItems()
            android.util.Log.d("MenuSelectionActivity", "Menu items loading initiated")
            updateSelectedCount()
            android.util.Log.d("MenuSelectionActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            // Log the error for debugging
            android.util.Log.e("MenuSelectionActivity", "Error in onCreate: ${e.message}", e)
            android.widget.Toast.makeText(this, "Error loading menu: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Set title based on category selection
        val title = if (selectedCategoryName.isNotEmpty()) {
            "Select from $selectedCategoryName"
        } else {
            "Select Menu Items"
        }
        supportActionBar?.title = title
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupDatabase() {
        val database = BillGenieDatabase.getDatabase(this)
        menuItemRepository = MenuItemRepository(database.menuItemDao())
    }
    
    private fun setupRecyclerView() {
        menuSelectionAdapter = MenuSelectionAdapter(
            onItemSelected = { menuItem ->
                addItemToSelection(menuItem)
            }
        )
        
        binding.recyclerViewMenuItems.apply {
            layoutManager = LinearLayoutManager(this@MenuSelectionActivity)
            adapter = menuSelectionAdapter
            // Ensure smooth scrolling
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }
    
    private fun setupSearchFunctionality() {
        binding.etSearchMenu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterMenuItems()
            }
        })
    }
    
    private fun setupCategoryFilters() {
        // Setup "All Items" chip click listener
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCategories.clear()
                // Uncheck all other category chips
                for (i in 1 until binding.chipGroupCategories.childCount) {
                    val chip = binding.chipGroupCategories.getChildAt(i) as? Chip
                    chip?.isChecked = false
                }
            }
            filterMenuItems()
        }
        
        // Setup category chip group listener
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategories.clear()
            
            var hasCheckedCategoryChips = false
            for (id in checkedIds) {
                if (id != binding.chipAll.id) {
                    val chip = findViewById<Chip>(id)
                    selectedCategories.add(chip.text.toString())
                    hasCheckedCategoryChips = true
                }
            }
            
            // If no category chips are selected, check "All Items"
            if (!hasCheckedCategoryChips && !binding.chipAll.isChecked) {
                binding.chipAll.isChecked = true
            }
            
            filterMenuItems()
        }
    }
    
    private fun populateCategoryChips(categories: List<String>) {
        // Clear existing category chips (except "All Items")
        for (i in binding.chipGroupCategories.childCount - 1 downTo 1) {
            binding.chipGroupCategories.removeViewAt(i)
        }
        
        // Add chips for each category
        categories.sorted().forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                setChipBackgroundColorResource(R.color.purple_50)
            }
            binding.chipGroupCategories.addView(chip)
        }
    }
    
    private fun setupClickListeners() {
        // Update button text based on mode
        if (isBillMode) {
            binding.btnDoneSelection.text = "View Order"
        } else {
            binding.btnDoneSelection.text = "Done"
        }
        
        binding.btnDoneSelection.setOnClickListener {
            returnSelectedItems()
        }
        
        binding.btnClearSelection.setOnClickListener {
            clearSelection()
        }
    }
    
    private fun loadMenuItems() {
        try {
            // Load all enabled menu items
            menuItemRepository.getEnabledMenuItems().observe(this, Observer { menuItems ->
                android.util.Log.d("MenuSelectionActivity", "Loaded ${menuItems.size} menu items")
                allMenuItems.clear()
                selectedMenuItems.clear()
                
                var activeItems = menuItems.filter { it.isActive && it.isEnabled }
                
                // Apply category filter if specified
                if (selectedCategoryName.isNotEmpty()) {
                    activeItems = activeItems.filter { it.category == selectedCategoryName }
                    android.util.Log.d("MenuSelectionActivity", "Filtered by category '$selectedCategoryName': ${activeItems.size} items")
                } else {
                    android.util.Log.d("MenuSelectionActivity", "No category filter applied: ${activeItems.size} items")
                }
                
                if (activeItems.isEmpty()) {
                    // No menu items found - guide user to add items first
                    android.util.Log.d("MenuSelectionActivity", "No menu items found in database")
                    allMenuItems.clear()
                    selectedMenuItems.clear()
                    menuSelectionAdapter.submitList(selectedMenuItems.toList())
                    
                    // Show helpful message
                    android.widget.Toast.makeText(this, "No menu items found.\nPlease go to Menu Management and add some items first.", android.widget.Toast.LENGTH_LONG).show()
                    
                    // Close this activity after a delay so user can go add menu items
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 3000)
                } else {
                    allMenuItems.addAll(activeItems)
                    selectedMenuItems.addAll(activeItems)
                    
                    // Populate category filters
                    val categories = activeItems.map { it.category }.distinct().filter { it.isNotBlank() }
                    populateCategoryChips(categories)
                }
                
                menuSelectionAdapter.submitList(selectedMenuItems.toList())
                
                if (selectedMenuItems.isEmpty()) {
                    android.widget.Toast.makeText(this, "No menu items found. Please add menu items first.", android.widget.Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("MenuSelectionActivity", "Error loading menu items: ${e.message}", e)
            android.widget.Toast.makeText(this, "Error loading menu items: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    private fun filterMenuItems() {
        val searchQuery = binding.etSearchMenu.text.toString().lowercase().trim()
        
        val filteredItems = allMenuItems.filter { item ->
            // Filter by search query
            val matchesSearch = searchQuery.isEmpty() || 
                    item.name.lowercase().contains(searchQuery) ||
                    item.category.lowercase().contains(searchQuery)
            
            // Filter by category
            val matchesCategory = selectedCategories.isEmpty() || 
                    selectedCategories.contains(item.category) ||
                    binding.chipAll.isChecked
            
            matchesSearch && matchesCategory
        }
        
        selectedMenuItems.clear()
        selectedMenuItems.addAll(filteredItems)
        menuSelectionAdapter.submitList(selectedMenuItems.toList())
    }
    
    private fun addItemToSelection(menuItem: MenuItem) {
        try {
            selectedItems.add(menuItem)
            updateSelectedCount()
            
            // Count how many times this item has been selected
            val itemCount = selectedItems.count { it.id == menuItem.id }
            
            // Show confirmation with full name (includes subcategory if applicable)
            val displayName = menuItem.name
            if (itemCount == 1) {
                android.widget.Toast.makeText(this, "$displayName added", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "$displayName (×$itemCount)", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MenuSelectionActivity", "Error adding item: ${e.message}", e)
            android.widget.Toast.makeText(this, "Error adding item: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateSelectedCount() {
        val count = selectedItems.size
        binding.tvSelectedCount.text = "$count item${if (count != 1) "s" else ""} selected"
        binding.btnDoneSelection.isEnabled = count > 0
        
        if (count > 0) {
            // Group items by full name (includes subcategory) and show quantities
            val itemCounts = selectedItems.groupingBy { it.name }.eachCount()
            val displayItems = itemCounts.entries.take(3).map { (name, count) ->
                if (count > 1) "$name (×$count)" else name
            }
            
            val display = if (itemCounts.size > 3) {
                "${displayItems.joinToString(", ")} and ${itemCounts.size - 3} more"
            } else {
                displayItems.joinToString(", ")
            }
            binding.tvSelectedItems.text = "Selected: $display"
        } else {
            binding.tvSelectedItems.text = "No items selected"
        }
    }
    
    private fun clearSelection() {
        selectedItems.clear()
        updateSelectedCount()
    }
    
    private fun returnSelectedItems() {
        val resultIntent = Intent().apply {
            putExtra("SELECTED_ITEMS", ArrayList(selectedItems.map { 
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "price" to it.price,
                    "category" to it.category
                )
            }))
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}