package com.billgenie

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.BillItemsAdapter
import com.billgenie.adapter.OrderCategoryAdapter
import com.billgenie.adapter.OrderMenuItemAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityTakeOrderBinding
import com.billgenie.model.BillItemDisplay
import com.billgenie.model.CustomerOrder
import com.billgenie.model.OrderStatus
import com.billgenie.model.MenuCategory
import com.billgenie.model.MenuItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.NumberFormat
import java.util.*

class TakeOrderActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTakeOrderBinding
    private lateinit var orderCategoryAdapter: OrderCategoryAdapter
    private lateinit var billItemsAdapter: BillItemsAdapter
    private lateinit var database: BillGenieDatabase
    
    private val billItems = mutableListOf<BillItemDisplay>()
    private var currentBillTotal = 0.0
    private var customerNumber = 1
    private var tableName = ""
    private var customerName = ""
    private var isOrderSaved = false
    private var lastSavedTotal = 0.0  // Track the total when last saved
    private var lastSavedItemCount = 0  // Track item count when last saved
    
    // Rupee formatter
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    companion object {
        const val EXTRA_CUSTOMER_NUMBER = "customer_number"
        const val EXTRA_TABLE_NAME = "table_name"
        const val EXTRA_CUSTOMER_NAME = "customer_name"
        const val RESULT_CUSTOMER_ORDER = "customer_order"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakeOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get customer details from intent
        customerNumber = intent.getIntExtra(EXTRA_CUSTOMER_NUMBER, 1)
        tableName = intent.getStringExtra(EXTRA_TABLE_NAME) ?: ""
        customerName = intent.getStringExtra(EXTRA_CUSTOMER_NAME) ?: ""
        
        setupToolbar()
        setupDatabase()
        setupRecyclerViews()
        setupClickListeners()
        loadCategories()
        setCustomerInfoInForm()
        loadExistingOrderIfEditing() // Load existing order items if editing
        updateBillTotal()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Take Order"
        
        // Set navigation icon tint to white
        binding.toolbar.navigationIcon?.setTint(
            androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
        )
        
        binding.toolbar.setNavigationOnClickListener {
            handleBackNavigation()
        }
        
        // Setup modern back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }
    
    private fun setupDatabase() {
        database = BillGenieDatabase.getDatabase(this)
    }
    
    private fun loadExistingOrderIfEditing() {
        // Check if we're editing an existing order (non-empty table name or customer name)
        if (tableName.isNotEmpty() || customerName.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val database = BillGenieDatabase.getDatabase(this@TakeOrderActivity)
                    val existingOrder = database.customerOrderDao().getOrderByCustomerNumber(customerNumber)
                    
                    if (existingOrder != null) {
                        android.util.Log.d("TakeOrderActivity", "Loading existing order for customer $customerNumber with ${existingOrder.orderItems.size} items")
                        
                        // Load existing items into the bill
                        billItems.clear()
                        billItems.addAll(existingOrder.orderItems)
                        
                        // Mark as saved since this is an existing order from database
                        isOrderSaved = true
                        lastSavedTotal = existingOrder.total
                        lastSavedItemCount = existingOrder.orderItems.sumOf { it.quantity }
                        
                        // Update UI on main thread
                        runOnUiThread {
                            billItemsAdapter.submitList(billItems.toList())
                            updateBillTotal()
                            Toast.makeText(this@TakeOrderActivity, "Loaded existing order with ${billItems.size} items", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.util.Log.d("TakeOrderActivity", "No existing order found for customer $customerNumber")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TakeOrderActivity", "Error loading existing order: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(this@TakeOrderActivity, "Error loading existing order: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun onCategorySelected(category: MenuCategory) {
        showMenuItemsDialog(category)
    }
    
    private fun showMenuItemsDialog(category: MenuCategory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_menu_items, null)
        val tvCategoryName = dialogView.findViewById<TextView>(R.id.tvCategoryName)
        val recyclerViewMenuItems = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMenuItems)
        val tvEmptyMenuItems = dialogView.findViewById<TextView>(R.id.tvEmptyMenuItems)
        val btnCloseDialog = dialogView.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnCloseCategory = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseCategory)
        
        tvCategoryName.text = category.name
        
        val menuItemAdapter = OrderMenuItemAdapter { menuItem, quantity ->
            addMenuItemToOrder(menuItem, quantity)
        }
        
        recyclerViewMenuItems.apply {
            layoutManager = LinearLayoutManager(this@TakeOrderActivity)
            adapter = menuItemAdapter
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Load menu items for this category
        lifecycleScope.launch {
            try {
                val menuItems = withContext(Dispatchers.IO) {
                    database.menuItemDao().getItemsByCategorySync(category.name)
                }
                
                withContext(Dispatchers.Main) {
                    if (menuItems.isNullOrEmpty()) {
                        recyclerViewMenuItems.visibility = android.view.View.GONE
                        tvEmptyMenuItems.visibility = android.view.View.VISIBLE
                    } else {
                        recyclerViewMenuItems.visibility = android.view.View.VISIBLE
                        tvEmptyMenuItems.visibility = android.view.View.GONE
                        menuItemAdapter.submitList(menuItems)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TakeOrderActivity", "Error loading menu items: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    recyclerViewMenuItems.visibility = android.view.View.GONE
                    tvEmptyMenuItems.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        // Set click listeners
        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCloseCategory.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }
    
    private fun addMenuItemToOrder(menuItem: MenuItem, quantity: Int) {
        val existingItem = billItems.find { it.menuItemId == menuItem.id }
        
        if (quantity <= 0) {
            // Remove item if quantity is 0 or less
            if (existingItem != null) {
                billItems.remove(existingItem)
                billItemsAdapter.submitList(billItems.toList())
                Toast.makeText(this, "${menuItem.name} removed from order", Toast.LENGTH_SHORT).show()
            }
        } else if (existingItem != null) {
            // Update existing item quantity
            val updatedItem = existingItem.copy(
                quantity = quantity,
                totalPrice = menuItem.price * quantity
            )
            val index = billItems.indexOf(existingItem)
            billItems[index] = updatedItem
            billItemsAdapter.submitList(billItems.toList())
        } else {
            // Add new item
            val billItem = BillItemDisplay(
                menuItemId = menuItem.id,
                itemName = menuItem.name,
                quantity = quantity,
                itemPrice = menuItem.price,
                totalPrice = menuItem.price * quantity
            )
            billItems.add(billItem)
            billItemsAdapter.submitList(billItems.toList())
            Toast.makeText(this, "${menuItem.name} added to order", Toast.LENGTH_SHORT).show()
        }
        
        updateBillTotal()
    }
    
    private fun setupRecyclerViews() {
        // Setup category adapter
        orderCategoryAdapter = OrderCategoryAdapter { category ->
            onCategorySelected(category)
        }
        
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(this@TakeOrderActivity)
            adapter = orderCategoryAdapter
            isNestedScrollingEnabled = false
        }
        
        // Setup bill items adapter
        billItemsAdapter = BillItemsAdapter(
            onQuantityChange = { billItem, newQuantity ->
                updateBillItemQuantity(billItem, newQuantity)
            },
            onRemoveClick = { billItem ->
                removeBillItem(billItem)
            }
        )
        
        binding.recyclerViewOrderItems.apply {
            layoutManager = LinearLayoutManager(this@TakeOrderActivity)
            adapter = billItemsAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSaveOrder.setOnClickListener {
            android.util.Log.d("TakeOrderActivity", "btnSaveOrder clicked - billItems.size: ${billItems.size}")
            if (billItems.isNotEmpty()) {
                saveCurrentOrder()
            } else {
                Toast.makeText(this, "Please add items to save the order", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnCheckout.setOnClickListener {
            if (isOrderSaved) {
                proceedToCheckout()
            } else {
                Toast.makeText(this, "Please save the order first", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnClearOrder.setOnClickListener {
            clearOrder()
        }
    }
    
    private fun setCustomerInfoInForm() {
        // Pre-fill the form with existing data
        binding.etTableNumber.setText(tableName)
        binding.etCustomerName.setText(customerName)
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                database.menuCategoryDao().getAllCategories().asLiveData().observe(this@TakeOrderActivity) { categories ->
                    if (categories.isNullOrEmpty()) {
                        binding.recyclerViewCategories.visibility = android.view.View.GONE
                        binding.tvEmptyCategories.visibility = android.view.View.VISIBLE
                    } else {
                        binding.recyclerViewCategories.visibility = android.view.View.VISIBLE
                        binding.tvEmptyCategories.visibility = android.view.View.GONE
                        orderCategoryAdapter.submitList(categories)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TakeOrderActivity", "Error loading categories: ${e.message}", e)
                binding.recyclerViewCategories.visibility = android.view.View.GONE
                binding.tvEmptyCategories.visibility = android.view.View.VISIBLE
            }
        }
    }
    

    
    private fun updateBillItemQuantity(billItem: BillItemDisplay, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeBillItem(billItem)
            return
        }
        
        val index = billItems.indexOf(billItem)
        if (index != -1) {
            val updatedItem = billItem.copy(
                quantity = newQuantity,
                totalPrice = billItem.itemPrice * newQuantity
            )
            billItems[index] = updatedItem
            billItemsAdapter.submitList(billItems.toList())
            updateBillTotal()
        }
    }
    
    private fun removeBillItem(billItem: BillItemDisplay) {
        val index = billItems.indexOf(billItem)
        if (index != -1) {
            billItems.removeAt(index)
            billItemsAdapter.submitList(billItems.toList())
            updateBillTotal()
            Toast.makeText(this, "${billItem.itemName} removed from order", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateBillTotal() {
        currentBillTotal = billItems.sumOf { it.totalPrice }
        binding.tvOrderTotal.text = rupeeFormatter.format(currentBillTotal)
        
        val totalItems = billItems.sumOf { it.quantity }
        binding.tvOrderItemsCount.text = "$totalItems item${if (totalItems != 1) "s" else ""}"
        
        // Check if order has been modified after saving
        val hasUnsavedChanges = isOrderSaved && 
            (currentBillTotal != lastSavedTotal || totalItems != lastSavedItemCount)
        
        // Update button states based on order status and changes
        if (isOrderSaved && !hasUnsavedChanges) {
            // Order is saved and no changes - show "Order Saved" disabled button
            binding.btnSaveOrder.visibility = android.view.View.VISIBLE
            binding.btnSaveOrder.text = "Order Saved"
            binding.btnSaveOrder.isEnabled = false
            binding.btnSaveOrder.icon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_menu_agenda)
            binding.btnCheckout.visibility = android.view.View.GONE
        } else {
            // Order not saved yet OR has unsaved changes - show active Save Order button
            binding.btnSaveOrder.visibility = android.view.View.VISIBLE
            binding.btnSaveOrder.text = if (hasUnsavedChanges) "Save Changes" else "Save Order"
            binding.btnSaveOrder.isEnabled = billItems.isNotEmpty()
            binding.btnSaveOrder.icon = androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_menu_save)
            binding.btnCheckout.visibility = android.view.View.GONE
            
            // Reset saved state if there are unsaved changes
            if (hasUnsavedChanges) {
                isOrderSaved = false
            }
        }
        
        if (billItems.isEmpty()) {
            binding.cardCurrentOrder.visibility = android.view.View.GONE
        } else {
            binding.cardCurrentOrder.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun clearOrder() {
        billItems.clear()
        billItemsAdapter.submitList(billItems.toList())
        isOrderSaved = false
        updateBillTotal()
        Toast.makeText(this, "Order cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveCurrentOrder() {
        // Get updated customer info from form
        val updatedTableName = binding.etTableNumber.text.toString().trim()
        val updatedCustomerName = binding.etCustomerName.text.toString().trim()
        
        android.util.Log.d("TakeOrderActivity", "saveCurrentOrder called")
        android.util.Log.d("TakeOrderActivity", "Table: $updatedTableName, Customer: $updatedCustomerName")
        android.util.Log.d("TakeOrderActivity", "Items count: ${billItems.size}")
        
        if (updatedTableName.isEmpty()) {
            binding.tilTableNumber.error = "Table number is required"
            Toast.makeText(this, "DEBUG: Table number is required", Toast.LENGTH_LONG).show()
            return
        }
        
        // Check if table is already occupied (only for new orders or if table name changed)
        if (updatedTableName != tableName) {
            val database = BillGenieDatabase.getDatabase(this@TakeOrderActivity)
            val customerOrderDao = database.customerOrderDao()
            
            lifecycleScope.launch {
                try {
                    val existingTableOrder = customerOrderDao.getActiveOrderByTableName(updatedTableName)
                    if (existingTableOrder != null && existingTableOrder.customerNumber != customerNumber) {
                        runOnUiThread {
                            binding.tilTableNumber.error = "Table $updatedTableName is already occupied"
                            Toast.makeText(this@TakeOrderActivity, "Table $updatedTableName is already occupied. Please choose a different table.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    
                    // Table is available, proceed with saving
                    runOnUiThread {
                        binding.tilTableNumber.error = null
                        proceedWithSave(updatedTableName, updatedCustomerName)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        android.util.Log.e("TakeOrderActivity", "Error checking table availability", e)
                        Toast.makeText(this@TakeOrderActivity, "Error checking table availability: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        } else {
            // Table name hasn't changed, proceed directly
            binding.tilTableNumber.error = null
            proceedWithSave(updatedTableName, updatedCustomerName)
        }
    }
    
    private fun proceedWithSave(updatedTableName: String, updatedCustomerName: String) {
        if (billItems.isEmpty()) {
            Toast.makeText(this, "Please add items to save the order", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update customer info
        tableName = updatedTableName
        customerName = updatedCustomerName
        
        // Save order to database
        lifecycleScope.launch {
            try {
                val database = BillGenieDatabase.getDatabase(this@TakeOrderActivity)
                val customerOrderDao = database.customerOrderDao()
                
                // Check if order already exists
                val existingOrder = customerOrderDao.getOrderByCustomerNumber(customerNumber)
                
                val customerOrder = CustomerOrder(
                    customerNumber = customerNumber,
                    tableName = tableName,
                    customerName = customerName,
                    orderItems = billItems.toList(),
                    total = currentBillTotal,
                    status = OrderStatus.PENDING
                )
                
                if (existingOrder != null) {
                    android.util.Log.d("TakeOrderActivity", "Updating existing order for customer $customerNumber")
                    // Use insertOrder with REPLACE strategy to update existing order
                    customerOrderDao.insertOrder(customerOrder)
                } else {
                    android.util.Log.d("TakeOrderActivity", "Creating new order for customer $customerNumber")
                    customerOrderDao.insertOrder(customerOrder)
                }
                
                android.util.Log.d("TakeOrderActivity", "Order saved to database for customer $customerNumber")
                
                // Mark order as saved in memory for this session
                isOrderSaved = true
                lastSavedTotal = currentBillTotal  // Record the saved total
                lastSavedItemCount = billItems.sumOf { it.quantity }  // Record the saved item count
                updateBillTotal()
                
                val action = if (existingOrder != null) "updated" else "saved"
                Toast.makeText(this@TakeOrderActivity, "Order $action for Customer $customerNumber! Items: ${billItems.size}", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                android.util.Log.e("TakeOrderActivity", "Error saving order to database", e)
                Toast.makeText(this@TakeOrderActivity, "Error saving order: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun proceedToCheckout() {
        if (!isOrderSaved) {
            Toast.makeText(this, "Please save the order first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create result intent with order data
        val resultIntent = Intent()
        resultIntent.putExtra("customer_number", customerNumber)
        resultIntent.putExtra("table_name", tableName)
        resultIntent.putExtra("customer_name", customerName)
        resultIntent.putExtra("order_items", ArrayList(billItems))
        resultIntent.putExtra("order_total", currentBillTotal)
        resultIntent.putExtra("is_saved", isOrderSaved)
        
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
    private fun handleBackNavigation() {
        android.util.Log.d("TakeOrderActivity", "handleBackNavigation - billItems.size: ${billItems.size}, isOrderSaved: $isOrderSaved")
        
        try {
            if (billItems.isNotEmpty() && !isOrderSaved) {
                AlertDialog.Builder(this)
                    .setTitle("Unsaved Order")
                    .setMessage("You have items in this order but haven't saved it yet. What would you like to do?")
                    .setPositiveButton("Save & Go Back") { _, _ ->
                        if (binding.etTableNumber.text.toString().trim().isNotEmpty()) {
                            saveCurrentOrder()
                            returnSavedOrder()
                        } else {
                            binding.tilTableNumber.error = "Table number is required to save"
                        }
                    }
                    .setNegativeButton("Discard & Go Back") { _, _ ->
                        finishActivity()
                    }
                    .setNeutralButton("Continue Editing", null)
                    .show()
            } else if (isOrderSaved) {
                // Order is saved, return the data when going back
                android.util.Log.d("TakeOrderActivity", "Order is saved, calling returnSavedOrder")
                returnSavedOrder()
            } else {
                // No items, safe to go back
                android.util.Log.d("TakeOrderActivity", "No items, going back normally")
                finishActivity()
            }
        } catch (e: Exception) {
            android.util.Log.e("TakeOrderActivity", "Error in handleBackNavigation: ${e.message}", e)
            // Fallback: just finish the activity
            finishActivity()
        }
    }
    
    private fun finishActivity() {
        try {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TakeOrderActivity", "Error finishing activity: ${e.message}", e)
        }
    }
    
    private fun returnSavedOrder() {
        android.util.Log.d("TakeOrderActivity", "returnSavedOrder called - isOrderSaved: $isOrderSaved, billItems.size: ${billItems.size}")
        
        try {
            if (isOrderSaved && billItems.isNotEmpty()) {
                // Create result intent with saved order data
                val resultIntent = Intent()
                resultIntent.putExtra("customer_number", customerNumber)
                resultIntent.putExtra("table_name", tableName)
                resultIntent.putExtra("customer_name", customerName)
                resultIntent.putExtra("order_items", ArrayList(billItems))
                resultIntent.putExtra("order_total", currentBillTotal)
                resultIntent.putExtra("is_saved", isOrderSaved)
                
                android.util.Log.d("TakeOrderActivity", "Returning saved order for customer $customerNumber with ${billItems.size} items")
                
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                // No saved order data to return
                android.util.Log.d("TakeOrderActivity", "No saved order data to return - isOrderSaved: $isOrderSaved, billItems.isEmpty: ${billItems.isEmpty()}")
                finishActivity()
            }
        } catch (e: Exception) {
            android.util.Log.e("TakeOrderActivity", "Error in returnSavedOrder: ${e.message}", e)
            finishActivity()
        }
    }
    
    private fun saveOrderForLater() {
        // TODO: Implement order saving to shared preferences or database
        Toast.makeText(this, "Order saved for Customer $customerNumber", Toast.LENGTH_SHORT).show()
    }
}