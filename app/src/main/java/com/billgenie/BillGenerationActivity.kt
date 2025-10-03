package com.billgenie

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.billgenie.adapter.BillItemsAdapter
// import com.billgenie.adapter.OrderCategoryAdapter
// import com.billgenie.adapter.FlavourSelectionAdapter
import com.billgenie.adapter.CustomerOrderAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.database.BillRepository
// import com.billgenie.database.CategoryDatabase
// import com.billgenie.repository.SimpleCategoryRepository
import com.billgenie.databinding.ActivityBillGenerationBinding
import com.billgenie.model.Bill
import com.billgenie.model.BillItem
import com.billgenie.model.BillItemDisplay
// import com.billgenie.model.Category
import com.billgenie.model.CustomerOrder
// import com.billgenie.model.Flavour
import com.billgenie.model.MenuItem
import com.billgenie.model.OrderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager

class BillGenerationActivity : AppCompatActivity() {
    
    // UPI Merchant Configuration - Replace with your actual details
    companion object {
        private const val MERCHANT_VPA = "billgenie@paytm" // Replace with your UPI ID
        private const val MERCHANT_NAME = "BillGenie Store"
        private const val MERCHANT_CODE = "1234" // Replace with your merchant code if required
    }
    
    private lateinit var binding: ActivityBillGenerationBinding
    private lateinit var billItemsAdapter: BillItemsAdapter
    // private lateinit var orderCategoryAdapter: OrderCategoryAdapter
    private lateinit var customerOrderAdapter: CustomerOrderAdapter
    private lateinit var billRepository: BillRepository
    // private lateinit var categoryRepository: SimpleCategoryRepository
    
    // Customer queue management
    private val customerOrders = mutableMapOf<Int, CustomerOrder>()
    private var nextCustomerNumber = 1
    
    // Remove local CustomerOrder class - using database entity instead
    
    // Activity result launcher for take order
    private val takeOrderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                handleOrderResult(data)
            }
        }
    }
    
    private val billItems = mutableListOf<BillItemDisplay>()
    private var currentBillTotal = 0.0
    
    // Rupee formatter
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    // Activity result launcher for menu selection
    private val menuSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val selectedItems = data.getSerializableExtra("SELECTED_ITEMS") as? ArrayList<Map<String, Any>>
                selectedItems?.let { items ->
                    addSelectedItemsToBill(items)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillGenerationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDatabase()
        setupRecyclerView()
        setupClickListeners()
        updateBillTotal()
        
        // Initialize view state
        loadOrdersFromDatabase()
    }
    
    private fun loadOrdersFromDatabase() {
        lifecycleScope.launch {
            try {
                val database = BillGenieDatabase.getDatabase(this@BillGenerationActivity)
                val customerOrderDao = database.customerOrderDao()
                
                // Load pending orders from database
                val pendingOrders = customerOrderDao.getAllPendingOrdersSync(OrderStatus.PENDING)
                
                android.util.Log.d("BillGenerationActivity", "Loaded ${pendingOrders.size} orders from database")
                Toast.makeText(this@BillGenerationActivity, "DEBUG: Loaded ${pendingOrders.size} orders from DB", Toast.LENGTH_LONG).show()
                
                // Clear current in-memory orders and populate from database
                customerOrders.clear()
                for (order in pendingOrders) {
                    customerOrders[order.customerNumber] = order
                }
                
                // Update next customer number based on database
                val maxCustomerNumber = customerOrderDao.getMaxCustomerNumber() ?: 0
                nextCustomerNumber = maxCustomerNumber + 1
                
                android.util.Log.d("BillGenerationActivity", "In-memory orders updated. Count: ${customerOrders.size}, Next customer: $nextCustomerNumber")
                
                // Initialize view state based on loaded data
                initializeViewState()
                
            } catch (e: Exception) {
                android.util.Log.e("BillGenerationActivity", "Error loading orders from database", e)
                Toast.makeText(this@BillGenerationActivity, "Error loading orders: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Fall back to empty state
                initializeViewState()
            }
        }
    }
    
    private suspend fun loadOrdersFromDatabaseSync() {
        try {
            val database = BillGenieDatabase.getDatabase(this@BillGenerationActivity)
            val customerOrderDao = database.customerOrderDao()
            
            // Load pending orders from database
            val pendingOrders = customerOrderDao.getAllPendingOrdersSync(OrderStatus.PENDING)
            
            android.util.Log.d("BillGenerationActivity", "Sync loaded ${pendingOrders.size} orders from database")
            
            // Update UI on main thread
            withContext(Dispatchers.Main) {
                // Clear current in-memory orders and populate from database
                customerOrders.clear()
                for (order in pendingOrders) {
                    customerOrders[order.customerNumber] = order
                }
                
                android.util.Log.d("BillGenerationActivity", "Sync updated in-memory orders. Count: ${customerOrders.size}")
                Toast.makeText(this@BillGenerationActivity, "DEBUG: Sync loaded ${customerOrders.size} orders", Toast.LENGTH_LONG).show()
                
                // Update UI
                updateUIState()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BillGenerationActivity", "Error sync loading orders from database", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BillGenerationActivity, "Error loading orders: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun initializeViewState() {
        if (customerOrders.isNotEmpty()) {
            showCustomerQueue()
        } else {
            // Show initial state and load categories for future use
            binding.layoutInitialState.visibility = android.view.View.VISIBLE
            binding.layoutOrderTaking.visibility = android.view.View.GONE
            loadCategories()
        }
        
        // Ensure saved orders card is initially hidden
        binding.cardSavedOrders.visibility = android.view.View.GONE
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
            finish()
        }
    }
    
    private fun setupDatabase() {
        val database = BillGenieDatabase.getDatabase(this)
        billRepository = BillRepository(database.billDao(), database.billItemDao())
        
        // val categoryDatabase = CategoryDatabase.getDatabase(this)
        // categoryRepository = SimpleCategoryRepository(
        //     categoryDatabase.categoryDao(),
        //     categoryDatabase.flavourDao()
        // )
    }
    
    private fun setupRecyclerView() {
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
            layoutManager = LinearLayoutManager(this@BillGenerationActivity)
            adapter = billItemsAdapter
            // Ensure smooth scrolling
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
        
        // Setup category adapter - Menu ingredients functionality removed
        /*
        orderCategoryAdapter = OrderCategoryAdapter { category ->
            onCategorySelected(category)
        }
        
        binding.recyclerViewCategories.apply {
            layoutManager = GridLayoutManager(this@BillGenerationActivity, 2)
            adapter = orderCategoryAdapter
            isNestedScrollingEnabled = false
        }
        */
        
        // Setup customer order adapter
        customerOrderAdapter = CustomerOrderAdapter(
            onEditOrder = { customerOrder ->
                editCustomerOrder(customerOrder)
            },
            onCheckoutOrder = { customerOrder ->
                checkoutCustomerOrder(customerOrder)
            }
        )
        
        binding.recyclerViewCustomerOrders.apply {
            layoutManager = LinearLayoutManager(this@BillGenerationActivity)
            adapter = customerOrderAdapter
            isNestedScrollingEnabled = false
        }
    }
    
    private fun setupClickListeners() {
        binding.btnStartOrder.setOnClickListener {
            startOrderTaking()
        }
        
        binding.btnCheckout.setOnClickListener {
            generateBill()
        }
        
        binding.btnClearOrder.setOnClickListener {
            clearBill()
        }
    }
    
    private fun startOrderTaking() {
        // Always show the customer queue UI if there are saved orders
        if (customerOrders.isNotEmpty()) {
            showCustomerQueue()
        }
        
        val intent = Intent(this, TakeOrderActivity::class.java)
        intent.putExtra(TakeOrderActivity.EXTRA_CUSTOMER_NUMBER, nextCustomerNumber)
        intent.putExtra(TakeOrderActivity.EXTRA_TABLE_NAME, binding.etTableNumber.text.toString().trim())
        intent.putExtra(TakeOrderActivity.EXTRA_CUSTOMER_NAME, binding.etCustomerName.text.toString().trim())
        
        takeOrderLauncher.launch(intent)
    }
    
    private fun handleOrderResult(data: Intent) {
        val customerNumber = data.getIntExtra("customer_number", 0)
        val tableName = data.getStringExtra("table_name") ?: ""
        val customerName = data.getStringExtra("customer_name") ?: ""
        val orderItems = data.getSerializableExtra("order_items") as? ArrayList<BillItemDisplay>
        val orderTotal = data.getDoubleExtra("order_total", 0.0)
        val isSaved = data.getBooleanExtra("is_saved", false)
        
        android.util.Log.d("BillGenerationActivity", "handleOrderResult called - customerNumber: $customerNumber")
        android.util.Log.d("BillGenerationActivity", "tableName: $tableName, customerName: $customerName")
        android.util.Log.d("BillGenerationActivity", "orderItems size: ${orderItems?.size ?: 0}")
        android.util.Log.d("BillGenerationActivity", "orderTotal: $orderTotal, isSaved: $isSaved")
        android.util.Log.d("BillGenerationActivity", "customerOrders.size before: ${customerOrders.size}")
        
        Toast.makeText(this, "DEBUG: Received order data - table: $tableName, items: ${orderItems?.size ?: 0}, saved: $isSaved", Toast.LENGTH_LONG).show()
        
        if (orderItems != null && orderItems.isNotEmpty() && isSaved) {
            // Order was saved to database in TakeOrderActivity, so reload orders from database
            android.util.Log.d("BillGenerationActivity", "Order was saved to database, reloading orders...")
            Toast.makeText(this, "DEBUG: Reloading orders from database...", Toast.LENGTH_LONG).show()
            
            // Reload all orders from database to update UI
            lifecycleScope.launch {
                loadOrdersFromDatabaseSync()
            }
            
        } else {
            // Customer came back without saving order
            android.util.Log.d("BillGenerationActivity", "No valid saved order data received")
            Toast.makeText(this, "DEBUG: No order taken for table $tableName", Toast.LENGTH_LONG).show()
        }
        
        // Force update the UI state immediately
        android.util.Log.d("BillGenerationActivity", "Calling updateUIState...")
        Toast.makeText(this, "DEBUG: About to call updateUIState with ${customerOrders.size} orders", Toast.LENGTH_LONG).show()
        updateUIState()
        
        // Clear form for next customer
        binding.etTableNumber.text?.clear()
        binding.etCustomerName.text?.clear()
    }
    
    private fun updateUIState() {
        android.util.Log.d("BillGenerationActivity", "updateUIState called - customerOrders.size: ${customerOrders.size}")
        Toast.makeText(this, "DEBUG: updateUIState - customerOrders: ${customerOrders.size}", Toast.LENGTH_LONG).show()
        
        if (customerOrders.isNotEmpty()) {
            android.util.Log.d("BillGenerationActivity", "Updating UI to show customer queue")
            Toast.makeText(this, "DEBUG: Switching to order taking layout", Toast.LENGTH_LONG).show()
            // Force switch to order taking layout
            binding.layoutInitialState.visibility = android.view.View.GONE
            binding.layoutOrderTaking.visibility = android.view.View.VISIBLE
            // Show customer queue
            showCustomerQueue()
        } else {
            android.util.Log.d("BillGenerationActivity", "No orders, showing initial state")
            Toast.makeText(this, "DEBUG: No orders, showing initial state", Toast.LENGTH_LONG).show()
            // No orders, go back to initial state
            binding.layoutOrderTaking.visibility = android.view.View.GONE
            binding.layoutInitialState.visibility = android.view.View.VISIBLE
            binding.cardSavedOrders.visibility = android.view.View.GONE
        }
    }
    
    private fun showCustomerQueue() {
        android.util.Log.d("BillGenerationActivity", "showCustomerQueue called with ${customerOrders.size} customers")
        Toast.makeText(this, "DEBUG: showCustomerQueue with ${customerOrders.size} customers", Toast.LENGTH_LONG).show()
        
        if (customerOrders.isNotEmpty()) {
            // Switch to order taking layout
            binding.layoutInitialState.visibility = android.view.View.GONE
            binding.layoutOrderTaking.visibility = android.view.View.VISIBLE
            
            // Update UI to show customer queue
            updateCustomerQueueDisplay()
            
            // Make sure the "Start Order" button functionality still works for new customers
            binding.btnStartOrder.text = "Take New Order"
            
            android.util.Log.d("BillGenerationActivity", "Layout switched to order taking mode")
            Toast.makeText(this, "DEBUG: Layout switched to order taking mode", Toast.LENGTH_LONG).show()
        } else {
            android.util.Log.d("BillGenerationActivity", "No customers to show")
            Toast.makeText(this, "DEBUG: No customers to show", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateCustomerQueueDisplay() {
        android.util.Log.d("BillGenerationActivity", "updateCustomerQueueDisplay called - customerOrders size: ${customerOrders.size}")
        Toast.makeText(this, "DEBUG: updateCustomerQueueDisplay - ${customerOrders.size} orders", Toast.LENGTH_LONG).show()
        
        if (customerOrders.isNotEmpty()) {
            // Force show saved customer orders card
            binding.cardSavedOrders.visibility = android.view.View.VISIBLE
            Toast.makeText(this, "DEBUG: cardSavedOrders set to VISIBLE", Toast.LENGTH_LONG).show()
            
            // Update adapter with current customer orders
            val customerList = customerOrders.values.toList()
            customerOrderAdapter.submitList(customerList)
            
            android.util.Log.d("BillGenerationActivity", "cardSavedOrders visibility set to VISIBLE")
            android.util.Log.d("BillGenerationActivity", "Submitted ${customerList.size} orders to adapter")
            Toast.makeText(this, "DEBUG: Submitted ${customerList.size} orders to adapter", Toast.LENGTH_LONG).show()
            
            // Also show categories for new orders (if available)
            loadCategories()
            
            val queueMessage = "${customerOrders.size} order${if (customerOrders.size != 1) "s" else ""} waiting for checkout"
            Toast.makeText(this, queueMessage, Toast.LENGTH_SHORT).show()
        } else {
            // Hide saved orders if no customers
            binding.cardSavedOrders.visibility = android.view.View.GONE
            android.util.Log.d("BillGenerationActivity", "No customers - hiding cardSavedOrders")
            Toast.makeText(this, "DEBUG: No customers - hiding cardSavedOrders", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun editCustomerOrder(customerOrder: CustomerOrder) {
        val intent = Intent(this, TakeOrderActivity::class.java)
        intent.putExtra(TakeOrderActivity.EXTRA_CUSTOMER_NUMBER, customerOrder.customerNumber)
        intent.putExtra(TakeOrderActivity.EXTRA_TABLE_NAME, customerOrder.tableName)
        intent.putExtra(TakeOrderActivity.EXTRA_CUSTOMER_NAME, customerOrder.customerName)
        
        takeOrderLauncher.launch(intent)
    }
    
    private fun checkoutCustomerOrder(customerOrder: CustomerOrder) {
        // Set current bill items to this customer's order
        billItems.clear()
        billItems.addAll(customerOrder.orderItems)
        billItemsAdapter.submitList(billItems.toList())
        
        // Proceed to generate bill
        generateBillForCustomer(customerOrder)
    }
    
    private fun generateBillForCustomer(customerOrder: CustomerOrder) {
        val customerName = customerOrder.customerName.ifEmpty { 
            if (customerOrder.tableName.isNotEmpty()) {
                "Table ${customerOrder.tableName}"
            } else {
                "Walk-in Customer"
            }
        }
        val customerPhone = "" // Phone number not captured in new flow
        
        if (billItems.isEmpty()) {
            Toast.makeText(this, "No items in the order", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show payment method selection dialog
        showPaymentMethodDialog(customerName, customerPhone, customerOrder)
    }
    
    private fun showPaymentMethodDialog(customerName: String, customerPhone: String, customerOrder: CustomerOrder) {
        val paymentOptions = arrayOf(
            "💵 Cash Payment",
            "📱 UPI Payment"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Select Payment Method")
            .setItems(paymentOptions) { _, which ->
                when (which) {
                    0 -> showCashTransactionDialog(customerName, customerPhone, customerOrder)
                    1 -> showUpiPaymentDialog(customerName, customerPhone, customerOrder)
                    else -> finalizeBillForCustomer(customerName, customerPhone, "Cash", 0.0, 0.0, customerOrder)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCashTransactionDialog(customerName: String, customerPhone: String, customerOrder: CustomerOrder) {
        // Use existing cash transaction dialog but modify the callback
        // ... (keeping existing cash dialog implementation)
        finalizeBillForCustomer(customerName, customerPhone, "Cash", 0.0, 0.0, customerOrder)
    }
    
    private fun showUpiPaymentDialog(customerName: String, customerPhone: String, customerOrder: CustomerOrder) {
        // Use existing UPI dialog but modify the callback
        // ... (keeping existing UPI dialog implementation) 
        finalizeBillForCustomer(customerName, customerPhone, "UPI", 0.0, 0.0, customerOrder)
    }
    
    private fun finalizeBillForCustomer(customerName: String, customerPhone: String, paymentMethod: String, cashGiven: Double = 0.0, changeReturned: Double = 0.0, customerOrder: CustomerOrder) {
        // Use existing finalizeBill logic
        finalizeBill(customerName, customerPhone, paymentMethod, cashGiven, changeReturned)
        
        // Remove customer from queue after successful checkout
        customerOrders.remove(customerOrder.customerNumber)
        
        // Update display based on remaining customers
        if (customerOrders.isEmpty()) {
            // No more customers, go back to initial state
            binding.layoutOrderTaking.visibility = android.view.View.GONE
            binding.layoutInitialState.visibility = android.view.View.VISIBLE
            binding.cardSavedOrders.visibility = android.view.View.GONE
        } else {
            // Update customer queue display with remaining customers
            updateCustomerQueueDisplay()
        }
        
        Toast.makeText(this, "Order completed successfully!", Toast.LENGTH_LONG).show()
    }
    
    private fun loadCategories() {
        // Menu ingredients functionality removed
        /*
        lifecycleScope.launch {
            try {
                categoryRepository.getAllCategories().observe(this@BillGenerationActivity) { categories ->
                    if (categories.isNullOrEmpty()) {
                        // Show empty state
                        binding.recyclerViewCategories.visibility = android.view.View.GONE
                        binding.tvEmptyCategories.visibility = android.view.View.VISIBLE
                    } else {
                        // Show categories
                        binding.recyclerViewCategories.visibility = android.view.View.VISIBLE
                        binding.tvEmptyCategories.visibility = android.view.View.GONE
                        orderCategoryAdapter.submitList(categories)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BillGenerationActivity", "Error loading categories: ${e.message}", e)
                binding.recyclerViewCategories.visibility = android.view.View.GONE
                binding.tvEmptyCategories.visibility = android.view.View.VISIBLE
            }
        }
        */
    }
    
    /*
    private fun onCategorySelected(category: Category) {
        // Menu ingredients functionality removed
        /*
        // Show flavour selection dialog for this category
        showFlavourSelectionDialog(category)
        */
    }
    */
    
    /*
    private fun showFlavourSelectionDialog(category: Category) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_flavour_selection, null)
        val tvCategoryName = dialogView.findViewById<TextView>(R.id.tvCategoryName)
        val recyclerViewFlavours = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewFlavours)
        val tvEmptyFlavours = dialogView.findViewById<TextView>(R.id.tvEmptyFlavours)
        val btnCloseDialog = dialogView.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnCloseCategory = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseCategory)
        
        tvCategoryName.text = category.name
        
        val flavourAdapter = FlavourSelectionAdapter(
            onAddItem = { flavour, quantity ->
                addFlavourToOrder(flavour, quantity)
            },
            onQuantityChange = { flavour, quantity ->
                updateFlavourInOrder(flavour, quantity)
            }
        )
        
        recyclerViewFlavours.apply {
            layoutManager = LinearLayoutManager(this@BillGenerationActivity)
            adapter = flavourAdapter
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Load flavours for this category
        lifecycleScope.launch {
            try {
                val flavours = categoryRepository.getFlavoursByCategorySync(category.id)
                if (flavours.isNullOrEmpty()) {
                    recyclerViewFlavours.visibility = android.view.View.GONE
                    tvEmptyFlavours.visibility = android.view.View.VISIBLE
                } else {
                    recyclerViewFlavours.visibility = android.view.View.VISIBLE
                    tvEmptyFlavours.visibility = android.view.View.GONE
                    flavourAdapter.submitList(flavours)
                }
            } catch (e: Exception) {
                android.util.Log.e("BillGenerationActivity", "Error loading flavours: ${e.message}", e)
                recyclerViewFlavours.visibility = android.view.View.GONE
                tvEmptyFlavours.visibility = android.view.View.VISIBLE
            }
        }
        
        // Set click listeners
        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnCloseCategory.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }
    */
    
    /*
    private fun addFlavourToOrder(flavour: Flavour, quantity: Int) {
        // Check if flavour already exists in order
        val existingItem = billItems.find { it.menuItemId == flavour.id }
        
        if (existingItem != null) {
            // Update existing item quantity
            updateBillItemQuantity(existingItem, existingItem.quantity + quantity)
        } else {
            // Add new item to order
            val billItem = BillItemDisplay(
                menuItemId = flavour.id,
                itemName = flavour.name,
                quantity = quantity,
                itemPrice = flavour.price,
                totalPrice = flavour.price * quantity
            )
            billItems.add(billItem)
            billItemsAdapter.submitList(billItems.toList())
        }
        
        updateBillTotal()
        Toast.makeText(this, "${flavour.name} added to order", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateFlavourInOrder(flavour: Flavour, quantity: Int) {
        val existingItem = billItems.find { it.menuItemId == flavour.id }
        
        if (existingItem != null) {
            if (quantity <= 0) {
                // Remove item from order
                removeBillItem(existingItem)
            } else {
                // Update quantity
                updateBillItemQuantity(existingItem, quantity)
            }
        } else if (quantity > 0) {
            // Add new item
            addFlavourToOrder(flavour, quantity)
        }
    }
    */
    
    private fun openMenuSelection() {
        try {
            val intent = Intent(this, MenuSelectionActivity::class.java)
            menuSelectionLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("BillGenerationActivity", "Error opening menu selection: ${e.message}", e)
            Toast.makeText(this, "Error opening menu selection: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun addSelectedItemsToBill(selectedItems: ArrayList<Map<String, Any>>) {
        // Group items by ID and count quantities
        val itemQuantities = mutableMapOf<Long, Int>()
        val itemDetails = mutableMapOf<Long, Map<String, Any>>()
        
        selectedItems.forEach { itemMap ->
            val menuId = (itemMap["id"] as? Number)?.toLong() ?: 0L
            itemQuantities[menuId] = (itemQuantities[menuId] ?: 0) + 1
            itemDetails[menuId] = itemMap
        }
        
        // Process each unique item
        itemQuantities.forEach { (menuId, totalQuantity) ->
            val itemMap = itemDetails[menuId]!!
            val name = itemMap["name"] as? String ?: ""
            val price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0
            
            // Check if item already exists in bill
            val existingItem = billItems.find { it.menuItemId == menuId }
            
            if (existingItem != null) {
                // Increase quantity by the total selected
                updateBillItemQuantity(existingItem, existingItem.quantity + totalQuantity)
            } else {
                // Add new item with correct quantity
                val billItem = BillItemDisplay(
                    menuItemId = menuId,
                    itemName = name,
                    quantity = totalQuantity,
                    itemPrice = price,
                    totalPrice = price * totalQuantity
                )
                billItems.add(billItem)
                billItemsAdapter.submitList(billItems.toList())
            }
        }
        
        updateBillTotal()
        val totalItemsAdded = itemQuantities.values.sum()
        Toast.makeText(this, "$totalItemsAdded item(s) added to bill", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "${billItem.itemName} removed from bill", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateBillTotal() {
        currentBillTotal = billItems.sumOf { it.totalPrice }
        binding.tvOrderTotal.text = rupeeFormatter.format(currentBillTotal)
        
        // Update bill items count
        val totalItems = billItems.sumOf { it.quantity }
        binding.tvOrderItemsCount.text = "$totalItems item${if (totalItems != 1) "s" else ""}"
        
        // Enable/disable generate bill button
        binding.btnCheckout.isEnabled = billItems.isNotEmpty()
        
        // Show/hide empty state and current order card
        if (billItems.isEmpty()) {
            binding.cardCurrentOrder.visibility = android.view.View.GONE
        } else {
            binding.cardCurrentOrder.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun generateBill() {
        val customerName = binding.etCustomerName.text.toString().trim()
        val customerPhone = "" // Phone number not captured in new flow
        
        // Validate input
        if (customerName.isEmpty()) {
            binding.etCustomerName.error = "Customer name is required"
            return
        }
        
        // Phone number is optional in the new flow
        
        if (billItems.isEmpty()) {
            Toast.makeText(this, "Please add items to the bill", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show payment method selection dialog
        showPaymentMethodDialog(customerName, customerPhone)
    }
    
    private fun showPaymentMethodDialog(customerName: String, customerPhone: String) {
        val paymentOptions = arrayOf(
            "💵 Cash Payment",
            "📱 UPI Payment"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Select Payment Method")
            .setItems(paymentOptions) { _, which ->
                when (which) {
                    0 -> showCashTransactionDialog(customerName, customerPhone) // Cash - show transaction screen
                    1 -> showUpiPaymentDialog(customerName, customerPhone) // UPI - show payment request screen
                    else -> finalizeBill(customerName, customerPhone, "Cash", 0.0, 0.0)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCashTransactionDialog(customerName: String, customerPhone: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cash_transaction, null)
        val tvBillTotal = dialogView.findViewById<TextView>(R.id.tvBillTotal)
        val tilCashGiven = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCashGiven)
        val etCashGiven = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCashGiven)
        val cardChange = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardChange)
        val tvChangeAmount = dialogView.findViewById<TextView>(R.id.tvChangeAmount)
        val tvInsufficientWarning = dialogView.findViewById<TextView>(R.id.tvInsufficientWarning)
        
        // Set bill total
        tvBillTotal.text = rupeeFormatter.format(currentBillTotal)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("💵 Cash Payment")
            .setView(dialogView)
            .setPositiveButton("Generate Bill", null) // Set to null initially
            .setNegativeButton("Cancel", null)
            .create()
        
        // Add text watcher for real-time change calculation
        etCashGiven.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val cashText = s.toString().trim()
                if (cashText.isNotEmpty()) {
                    try {
                        val cashGiven = cashText.toDouble()
                        val change = cashGiven - currentBillTotal
                        
                        if (change >= 0) {
                            // Sufficient cash
                            cardChange.visibility = android.view.View.VISIBLE
                            tvChangeAmount.text = rupeeFormatter.format(change)
                            tvInsufficientWarning.visibility = android.view.View.GONE
                            tilCashGiven.error = null
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                        } else {
                            // Insufficient cash
                            cardChange.visibility = android.view.View.GONE
                            tvInsufficientWarning.visibility = android.view.View.VISIBLE
                            tilCashGiven.error = "Insufficient amount"
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                        }
                    } catch (e: NumberFormatException) {
                        cardChange.visibility = android.view.View.GONE
                        tvInsufficientWarning.visibility = android.view.View.GONE
                        tilCashGiven.error = "Invalid amount"
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                    }
                } else {
                    cardChange.visibility = android.view.View.GONE
                    tvInsufficientWarning.visibility = android.view.View.GONE
                    tilCashGiven.error = null
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                }
            }
        })
        
        dialog.show()
        
        // Disable the positive button initially
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        
        // Set the positive button click listener after dialog is shown
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val cashText = etCashGiven.text.toString().trim()
            if (cashText.isNotEmpty()) {
                try {
                    val cashGiven = cashText.toDouble()
                    val change = cashGiven - currentBillTotal
                    if (change >= 0) {
                        dialog.dismiss()
                        finalizeBill(customerName, customerPhone, "Cash", cashGiven, change)
                    }
                } catch (e: NumberFormatException) {
                    tilCashGiven.error = "Invalid amount"
                }
            }
        }
    }
    
    private fun showUpiPaymentDialog(customerName: String, customerPhone: String) {
        // Validate customer phone number for UPI
        if (customerPhone.isBlank()) {
            Toast.makeText(this, "Please enter customer phone number for UPI payment", Toast.LENGTH_LONG).show()
            return
        }
        
        if (!isValidPhoneNumber(customerPhone)) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_LONG).show()
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_upi_payment, null)
        val tvUpiAmount = dialogView.findViewById<TextView>(R.id.tvUpiAmount)
        val tvUpiCustomerName = dialogView.findViewById<TextView>(R.id.tvUpiCustomerName)
        val tvUpiCustomerPhone = dialogView.findViewById<TextView>(R.id.tvUpiCustomerPhone)
        val btnGooglePay = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGooglePay)
        val btnPhonePe = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPhonePe)
        val btnPaytm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPaytm)
        val btnBhim = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBhim)
        val btnGenericUpi = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenericUpi)
        
        // Set payment details
        tvUpiAmount.text = rupeeFormatter.format(currentBillTotal)
        tvUpiCustomerName.text = customerName
        tvUpiCustomerPhone.text = "📱 $customerPhone"
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("📱 UPI Payment Request")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
        
        // UPI payment request handlers
        btnGooglePay.setOnClickListener {
            sendUpiPaymentRequest("Google Pay", customerPhone)
            dialog.dismiss()
        }
        
        btnPhonePe.setOnClickListener {
            sendUpiPaymentRequest("PhonePe", customerPhone)
            dialog.dismiss()
        }
        
        btnPaytm.setOnClickListener {
            sendUpiPaymentRequest("Paytm", customerPhone)
            dialog.dismiss()
        }
        
        btnBhim.setOnClickListener {
            sendUpiPaymentRequest("BHIM UPI", customerPhone)
            dialog.dismiss()
        }
        
        btnGenericUpi.setOnClickListener {
            sendUpiPaymentRequest("Generic UPI", customerPhone)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Check if phone number is valid (10 digits, optionally starting with +91)
        val phonePattern = Regex("^(\\+91)?[6-9]\\d{9}$")
        val cleanPhone = phone.replace("\\s+".toRegex(), "").replace("-", "")
        return phonePattern.matches(cleanPhone)
    }
    
    private fun formatPhoneNumber(phone: String): String {
        // Format phone number for display
        val cleanPhone = phone.replace("\\s+".toRegex(), "").replace("-", "").replace("+91", "")
        return if (cleanPhone.length == 10) {
            "+91 $cleanPhone"
        } else {
            phone
        }
    }

    private fun sendUpiPaymentRequest(upiApp: String, customerPhone: String) {
        // UPI payment details using merchant configuration
        val merchantVpa = MERCHANT_VPA
        val merchantName = MERCHANT_NAME
        val transactionNote = "Bill Payment - ${binding.etCustomerName.text.toString().trim()}"
        val amount = currentBillTotal.toString()
        val formattedPhone = formatPhoneNumber(customerPhone)
        
        try {
            // Create UPI payment URL
            val upiUrl = "upi://pay?pa=$merchantVpa&pn=$merchantName&am=$amount&cu=INR&tn=$transactionNote"
            
            // Create intent based on selected UPI app
            val intent = when (upiApp) {
                "Google Pay" -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(upiUrl)
                        setPackage("com.google.android.apps.nbu.paisa.user")
                    }
                }
                "PhonePe" -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(upiUrl)
                        setPackage("com.phonepe.app")
                    }
                }
                "Paytm" -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(upiUrl)
                        setPackage("net.one97.paytm")
                    }
                }
                "BHIM UPI" -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(upiUrl)
                        setPackage("in.org.npci.upiapp")
                    }
                }
                else -> {
                    // Generic UPI intent - let user choose
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(upiUrl)
                    }
                }
            }
            
            // Check if the app is installed
            val packageManager = packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            
            if (activities.isNotEmpty()) {
                // App is installed, launch payment request
                startActivity(intent)
                
                // Show confirmation dialog
                showPaymentConfirmationDialog(upiApp, formattedPhone)
            } else {
                // App not installed, show alternative options
                showAppNotInstalledDialog(upiApp, upiUrl)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("UpiPayment", "Error creating UPI payment request: ${e.message}")
            Toast.makeText(this, "Error creating payment request: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showPaymentConfirmationDialog(upiApp: String, customerPhone: String) {
        val message = """
            📱 UPI Payment Request Opened in $upiApp
            
            Customer: $customerPhone
            Amount: ${rupeeFormatter.format(currentBillTotal)}
            
            Please wait for customer to complete the payment, then confirm below.
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("⏳ Waiting for Payment")
            .setMessage(message)
            .setPositiveButton("✅ Payment Completed") { _, _ ->
                finalizeBill(
                    binding.etCustomerName.text.toString().trim(),
                    "", // Phone number not captured in new flow
                    "UPI ($upiApp)"
                )
            }
            .setNegativeButton("❌ Payment Failed/Cancelled") { _, _ ->
                Toast.makeText(this, "Payment cancelled. You can try again.", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showAppNotInstalledDialog(upiApp: String, upiUrl: String) {
        val message = """
            $upiApp is not installed on this device.
            
            You can:
            1. Install $upiApp from Play Store
            2. Use any other UPI app
            3. Share payment link with customer
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("⚠️ App Not Found")
            .setMessage(message)
            .setPositiveButton("Share Payment Link") { _, _ ->
                shareUpiPaymentLink(upiUrl)
            }
            .setNegativeButton("Try Another App") { _, _ ->
                // User can select another UPI app
            }
            .setNeutralButton("Install $upiApp") { _, _ ->
                openPlayStoreForApp(upiApp)
            }
            .show()
    }
    
    private fun shareUpiPaymentLink(upiUrl: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Please complete your payment using this UPI link: $upiUrl")
            putExtra(Intent.EXTRA_SUBJECT, "Payment Request - BillGenie")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Payment Link"))
    }
    
    private fun openPlayStoreForApp(upiApp: String) {
        val packageName = when (upiApp) {
            "Google Pay" -> "com.google.android.apps.nbu.paisa.user"
            "PhonePe" -> "com.phonepe.app"
            "Paytm" -> "net.one97.paytm"
            "BHIM UPI" -> "in.org.npci.upiapp"
            else -> ""
        }
        
        if (packageName.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
            }
        }
    }

    private fun finalizeBill(customerName: String, customerPhone: String, paymentMethod: String, cashGiven: Double = 0.0, changeReturned: Double = 0.0) {
        android.util.Log.d("BillGenerationActivity", "finalizeBill called with payment: $paymentMethod")
        android.util.Log.d("BillGenerationActivity", "Bill items count: ${billItems.size}")
        android.util.Log.d("BillGenerationActivity", "Current total: $currentBillTotal")
        
        // Check if there are items in the bill
        if (billItems.isEmpty()) {
            Toast.makeText(this, "No items selected. Please add items to the bill first.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Create bill with payment method
        val bill = Bill(
            customerName = customerName,
            customerPhone = customerPhone,
            totalAmount = currentBillTotal,
            paymentMethod = paymentMethod
        )
        
        android.util.Log.d("BillGenerationActivity", "Starting database save operation")
        
        // Save bill and bill items
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val billId = billRepository.insertBill(bill)
                android.util.Log.d("BillGenerationActivity", "Bill saved with ID: $billId")
                
                // Convert BillItemDisplay to BillItem for database storage
                val billItemsForDb = billItems.map { displayItem ->
                    BillItem(
                        billId = billId,
                        menuItemId = displayItem.menuItemId,
                        itemName = displayItem.itemName,
                        quantity = displayItem.quantity,
                        itemPrice = displayItem.itemPrice
                    )
                }
                billRepository.insertBillItems(billItemsForDb)
                android.util.Log.d("BillGenerationActivity", "Bill items saved successfully")
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BillGenerationActivity, "Bill generated successfully with ${paymentMethod} payment!", Toast.LENGTH_LONG).show()
                    showBillSummary(bill, billItems, cashGiven, changeReturned)
                    clearBill()
                }
            } catch (e: Exception) {
                android.util.Log.e("BillGenerationActivity", "Error saving bill: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BillGenerationActivity, "Error generating bill: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showBillSummary(bill: Bill, billItems: List<BillItemDisplay>, cashGiven: Double = 0.0, changeReturned: Double = 0.0) {
        val paymentIcon = if (bill.paymentMethod == "UPI") "📱" else "💵"
        val summary = buildString {
            appendLine("BILL GENERATED")
            appendLine("═══════════════")
            appendLine("Customer: ${bill.customerName}")
            appendLine("Phone: ${bill.customerPhone}")
            appendLine("Date: ${bill.dateCreated}")
            appendLine("Payment: $paymentIcon ${bill.paymentMethod}")
            
            // Add cash details for cash payments
            if (bill.paymentMethod == "Cash" && cashGiven > 0) {
                appendLine()
                appendLine("CASH DETAILS:")
                appendLine("Cash Given: ${rupeeFormatter.format(cashGiven)}")
                appendLine("Change: ${rupeeFormatter.format(changeReturned)}")
            }
            
            appendLine()
            appendLine("ITEMS:")
            appendLine("───────────────")
            billItems.forEach { item ->
                val itemTotal = item.totalPrice
                appendLine("${item.itemName}")
                appendLine("  ${item.quantity} × ${rupeeFormatter.format(item.itemPrice)} = ${rupeeFormatter.format(itemTotal)}")
            }
            appendLine("───────────────")
            appendLine("TOTAL: ${rupeeFormatter.format(bill.totalAmount)}")
            appendLine("═══════════════")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bill Generated Successfully")
            .setMessage(summary)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("New Bill") { dialog, _ ->
                dialog.dismiss()
                // Already cleared, ready for new bill
            }
            .show()
    }
    
    private fun clearBill() {
        billItems.clear()
        billItemsAdapter.submitList(billItems.toList())
        binding.etCustomerName.text?.clear()
        binding.etTableNumber.text?.clear()
        updateBillTotal()
        Toast.makeText(this, "Bill cleared", Toast.LENGTH_SHORT).show()
    }
}