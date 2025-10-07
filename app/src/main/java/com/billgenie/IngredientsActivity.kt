package com.billgenie

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.CategorySelectionAdapter
import com.billgenie.adapter.MenuItemSelectionAdapter
import com.billgenie.adapter.RecipeIngredientAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.model.*
import com.billgenie.repository.IngredientsRepository
import com.billgenie.utils.UnitConverter
import com.billgenie.viewmodel.IngredientsViewModel
import com.billgenie.viewmodel.IngredientsViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IngredientsActivity : AppCompatActivity() {

    private lateinit var database: BillGenieDatabase
    private lateinit var repository: IngredientsRepository
    private val viewModel: IngredientsViewModel by viewModels {
        IngredientsViewModelFactory(repository)
    }

    // UI Components
    private lateinit var categoryAdapter: CategorySelectionAdapter
    private lateinit var menuItemAdapter: MenuItemSelectionAdapter
    private lateinit var ingredientAdapter: RecipeIngredientAdapter
    
    // Current selections
    private var selectedCategory: String? = null
    private var selectedMenuItem: MenuItem? = null
    private var currentIngredients: List<RecipeDisplayItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients)

        initializeDatabase()
        setupToolbar()
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        
        // Load initial data
        viewModel.loadCategories()
        
        // Load ingredients with error handling
        try {
            viewModel.loadAllIngredients()
        } catch (e: Exception) {
            android.util.Log.e("IngredientsActivity", "Error loading ingredients on startup", e)
        }
        
        // Add direct database check
        checkDatabaseState()
    }

    private fun initializeDatabase() {
        database = BillGenieDatabase.getDatabase(this)
        repository = IngredientsRepository(
            database.ingredientDao(),
            database.recipeDao(),
            database.menuItemDao(),
            database.menuCategoryDao()
        )
    }

    private fun setupToolbar() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupRecyclerViews() {
        // Categories RecyclerView
        categoryAdapter = CategorySelectionAdapter { category ->
            selectedCategory = category
            viewModel.loadMenuItemsByCategory(category)
            clearSelectedItem()
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewCategories).apply {
            layoutManager = LinearLayoutManager(this@IngredientsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Menu Items RecyclerView
        menuItemAdapter = MenuItemSelectionAdapter { menuItem ->
            selectedMenuItem = menuItem
            displaySelectedItem(menuItem)
            viewModel.loadRecipeIngredients(menuItem.id)
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMenuItems).apply {
            layoutManager = LinearLayoutManager(this@IngredientsActivity)
            adapter = menuItemAdapter
        }

        // Ingredients RecyclerView
        ingredientAdapter = RecipeIngredientAdapter(
            onEditIngredient = { recipeItem ->
                showAddEditIngredientDialog(recipeItem)
            },
            onDeleteIngredient = { recipeItem ->
                showDeleteConfirmation(recipeItem)
            }
        )
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewIngredients).apply {
            layoutManager = LinearLayoutManager(this@IngredientsActivity)
            adapter = ingredientAdapter
        }
    }

    private fun setupObservers() {
        viewModel.categories.observe(this, Observer { categories ->
            android.util.Log.d("IngredientsActivity", "Categories loaded: ${categories.size}")
            categories.forEach { category ->
                android.util.Log.d("IngredientsActivity", "Category: ${category.name}")
            }
            val categoryNames = categories.map { it.name }
            categoryAdapter.updateCategories(categoryNames)
            
            // Show debug message if no categories
            if (categories.isEmpty()) {
                Toast.makeText(this, "No menu categories found. Please add categories in Menu & Pricing first.", Toast.LENGTH_LONG).show()
            }
        })

        viewModel.menuItems.observe(this, Observer { menuItems ->
            android.util.Log.d("IngredientsActivity", "Menu items loaded for category '$selectedCategory': ${menuItems.size}")
            menuItemAdapter.updateMenuItems(menuItems)
        })

        viewModel.recipeIngredients.observe(this, Observer { ingredients ->
            android.util.Log.d("IngredientsActivity", "Recipe ingredients loaded: ${ingredients.size}")
            currentIngredients = ingredients
            ingredientAdapter.updateIngredients(ingredients)
            updateIngredientsVisibility(ingredients.isNotEmpty())
        })

        viewModel.allIngredients.observe(this, Observer { ingredients ->
            android.util.Log.d("IngredientsActivity", "All ingredients loaded: ${ingredients.size}")
            // Store for dropdown usage in dialog
        })
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.buttonAddIngredient).setOnClickListener {
            selectedMenuItem?.let { menuItem ->
                try {
                    android.util.Log.d("IngredientsActivity", "Add ingredient clicked for item: ${menuItem.name}")
                    showAddEditIngredientDialog(null)
                } catch (e: Exception) {
                    android.util.Log.e("IngredientsActivity", "Error opening add ingredient dialog", e)
                    Toast.makeText(this, "Error opening ingredient dialog: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                android.util.Log.w("IngredientsActivity", "Add ingredient clicked but no menu item selected")
                Toast.makeText(this, "Please select a menu item first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displaySelectedItem(menuItem: MenuItem) {
        findViewById<View>(R.id.layoutSelectedItem).visibility = View.VISIBLE
        findViewById<android.widget.TextView>(R.id.textSelectedItem).text = 
            "${menuItem.name} ${menuItem.category}"
        findViewById<MaterialButton>(R.id.buttonAddIngredient).isEnabled = true
        findViewById<View>(R.id.layoutEmptyState).visibility = View.GONE
    }

    private fun clearSelectedItem() {
        selectedMenuItem = null
        findViewById<View>(R.id.layoutSelectedItem).visibility = View.GONE
        findViewById<MaterialButton>(R.id.buttonAddIngredient).isEnabled = false
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewIngredients).visibility = View.GONE
        findViewById<View>(R.id.layoutEmptyState).visibility = View.VISIBLE
        currentIngredients = emptyList()
    }

    private fun updateIngredientsVisibility(hasIngredients: Boolean) {
        if (selectedMenuItem != null) {
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewIngredients).visibility = 
                if (hasIngredients) View.VISIBLE else View.GONE
            findViewById<View>(R.id.layoutEmptyState).visibility = 
                if (hasIngredients) View.GONE else View.VISIBLE
        }
    }

    private fun showAddEditIngredientDialog(existingRecipe: RecipeDisplayItem?) {
        try {
            android.util.Log.d("IngredientsActivity", "showAddEditIngredientDialog called")
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_ingredient, null)
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create()

            // Setup dialog components
            val editTextIngredientName = dialogView.findViewById<TextInputEditText>(R.id.editTextIngredientName)
            val editTextQuantity = dialogView.findViewById<TextInputEditText>(R.id.editTextQuantity)
            val autoCompleteUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteUnit)
            val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotes)
            val checkBoxOptional = dialogView.findViewById<MaterialCheckBox>(R.id.checkBoxOptional)
            val buttonSave = dialogView.findViewById<MaterialButton>(R.id.buttonSave)
            val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
            val textDialogTitle = dialogView.findViewById<android.widget.TextView>(R.id.textDialogTitle)

            android.util.Log.d("IngredientsActivity", "Dialog views found successfully")

            // Setup unit dropdown
            try {
                val units = listOf("grams", "kg", "ml", "liters", "pieces", "cups", "tbsp", "tsp", "oz", "lbs")
                val unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
                autoCompleteUnit.setAdapter(unitAdapter)
                
                // Set default unit
                autoCompleteUnit.setText("grams", false)
                
                android.util.Log.d("IngredientsActivity", "Unit dropdown setup completed with ${units.size} units")
            } catch (e: Exception) {
                android.util.Log.e("IngredientsActivity", "Error setting up unit dropdown", e)
            }

        // Fill existing data if editing
        try {
            existingRecipe?.let { recipe ->
                android.util.Log.d("IngredientsActivity", "Editing existing recipe: ${recipe.ingredientName}")
                textDialogTitle.text = "Edit Ingredient"
                buttonSave.text = "Update Ingredient"
                editTextIngredientName.setText(recipe.ingredientName)
                editTextQuantity.setText(recipe.quantityRequired.toString())
                autoCompleteUnit.setText(recipe.unit, false)
                editTextNotes.setText(recipe.notes ?: "")
                checkBoxOptional.isChecked = recipe.isOptional
            }
        } catch (e: Exception) {
            android.util.Log.e("IngredientsActivity", "Error filling existing data", e)
        }

        buttonCancel.setOnClickListener {
            try {
                dialog.dismiss()
            } catch (e: Exception) {
                android.util.Log.e("IngredientsActivity", "Error dismissing dialog", e)
            }
        }

        buttonSave.setOnClickListener {
            try {
                saveIngredient(
                    dialog,
                    editTextIngredientName.text.toString().trim(),
                    editTextQuantity.text.toString().trim(),
                    autoCompleteUnit.text.toString().trim(),
                    editTextNotes.text.toString().trim(),
                    checkBoxOptional.isChecked,
                    existingRecipe?.recipe
                )
            } catch (e: Exception) {
                android.util.Log.e("IngredientsActivity", "Error saving ingredient", e)
                Toast.makeText(this, "Error saving ingredient: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        try {
            dialog.show()
            android.util.Log.d("IngredientsActivity", "Dialog shown successfully")
        } catch (e: Exception) {
            android.util.Log.e("IngredientsActivity", "Error showing dialog", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
        } catch (e: Exception) {
            android.util.Log.e("IngredientsActivity", "Error in showAddEditIngredientDialog", e)
            Toast.makeText(this, "Error opening ingredient dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveIngredient(
        dialog: Dialog,
        ingredientName: String,
        quantityText: String,
        unit: String,
        notes: String,
        isOptional: Boolean,
        existingRecipe: Recipe?
    ) {
        if (ingredientName.isEmpty()) {
            Toast.makeText(this, "Please enter an ingredient name", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantityText.isEmpty()) {
            Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show()
            return
        }

        if (unit.isEmpty()) {
            Toast.makeText(this, "Please select a unit", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityText.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        selectedMenuItem?.let { menuItem ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    android.util.Log.d("IngredientsActivity", "Starting to save ingredient: $ingredientName")
                    
                    // Convert quantity to base unit for storage
                    val (baseQuantity, baseUnit) = UnitConverter.toBaseUnit(quantity, unit)
                    
                    // Find or create ingredient with the specified unit (will be converted to base unit)
                    val ingredient = repository.findOrCreateIngredient(ingredientName, unit)
                    android.util.Log.d("IngredientsActivity", "Found/created ingredient: ${ingredient.name}, ID: ${ingredient.id}, Unit: ${ingredient.unit}")
                    android.util.Log.d("IngredientsActivity", "Original: $quantity $unit -> Base: $baseQuantity ${ingredient.unit}")
                    
                    if (existingRecipe != null) {
                        android.util.Log.d("IngredientsActivity", "Updating existing recipe")
                        // Update existing recipe with base unit values
                        val updatedRecipe = existingRecipe.copy(
                            quantityRequired = baseQuantity,
                            unit = ingredient.unit, // Use the ingredient's base unit
                            notes = notes.ifEmpty { null },
                            isOptional = isOptional
                        )
                        repository.updateRecipe(updatedRecipe)
                    } else {
                        android.util.Log.d("IngredientsActivity", "Creating new recipe")
                        // Create new recipe with base unit values
                        val recipe = Recipe(
                            menuItemId = menuItem.id,
                            ingredientId = ingredient.id,
                            quantityRequired = baseQuantity,
                            unit = ingredient.unit, // Use the ingredient's base unit
                            notes = notes.ifEmpty { null },
                            isOptional = isOptional
                        )
                        repository.insertRecipe(recipe)
                    }

                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(
                            this@IngredientsActivity,
                            if (existingRecipe != null) "Ingredient updated successfully" 
                            else "Ingredient added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Reload ingredients for current item
                        viewModel.loadRecipeIngredients(menuItem.id)
                    }
                } catch (e: Exception) {
                    Log.e("IngredientsActivity", "Error saving ingredient", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@IngredientsActivity,
                            "Error saving ingredient: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation(recipeItem: RecipeDisplayItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Ingredient")
            .setMessage("Are you sure you want to remove ${recipeItem.ingredientName} from ${recipeItem.menuItemName}?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        viewModel.deleteRecipe(recipeItem.recipe)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@IngredientsActivity,
                                "Ingredient removed successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Reload ingredients for current item
                            selectedMenuItem?.let { menuItem ->
                                viewModel.loadRecipeIngredients(menuItem.id)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("IngredientsActivity", "Error deleting ingredient", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@IngredientsActivity,
                                "Error removing ingredient: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun checkDatabaseState() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check menu categories
                val categories = database.menuCategoryDao().getAllActiveCategoriesSync()
                
                // Check menu items  
                val menuItems = database.menuItemDao().getActiveItemCount()
                
                withContext(Dispatchers.Main) {
                    val message = buildString {
                        append("Database State Check:\n")
                        append("Active Categories: ${categories.size}\n")
                        append("Active Menu Items: $menuItems\n\n")
                        
                        if (categories.isEmpty()) {
                            append("❌ No categories found!\n")
                            append("Please go to 'Menu & Pricing' and add some categories and menu items first.\n\n")
                            append("The database was reset due to version upgrade. You'll need to recreate your menu structure.")
                        } else {
                            append("✅ Categories found:\n")
                            categories.forEach { cat ->
                                append("• ${cat.name}\n")
                            }
                        }
                    }
                    
                    Toast.makeText(this@IngredientsActivity, message, Toast.LENGTH_LONG).show()
                    android.util.Log.d("IngredientsActivity", message)
                }
            } catch (e: Exception) {
                android.util.Log.e("IngredientsActivity", "Error checking database state", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@IngredientsActivity, "Database check error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}