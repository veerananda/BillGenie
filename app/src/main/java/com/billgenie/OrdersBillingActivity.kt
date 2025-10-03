package com.billgenie

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.CustomerOrderAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityOrdersBillingBinding
import com.billgenie.model.BillItemDisplay
import com.billgenie.model.CustomerOrder
import com.billgenie.model.OrderStatus
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class OrdersBillingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOrdersBillingBinding
    private lateinit var database: BillGenieDatabase
    private lateinit var customerOrderAdapter: CustomerOrderAdapter
    
    // Customer orders management
    private val customerOrders = mutableListOf<CustomerOrder>()
    private var nextCustomerNumber = 1
    
    // Currency formatter
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDatabase()
        setupRecyclerView()
        setupClickListeners()
        loadExistingOrders()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Orders & Billing"
        
        // Set navigation icon tint to white
        binding.toolbar.navigationIcon?.setTint(
            androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
        )
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupDatabase() {
        database = BillGenieDatabase.getDatabase(this)
    }
    
    private fun setupRecyclerView() {
        customerOrderAdapter = CustomerOrderAdapter(
            onEditOrder = { customerOrder ->
                editCustomerOrder(customerOrder)
            },
            onCheckoutOrder = { customerOrder ->
                checkoutCustomerOrder(customerOrder)
            }
        )
        
        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(this@OrdersBillingActivity)
            adapter = customerOrderAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.fabTakeNewOrder.setOnClickListener {
            takeNewOrder()
        }
        
        binding.btnTakeNewOrder.setOnClickListener {
            takeNewOrder()
        }
    }
    
    private fun loadExistingOrders() {
        android.util.Log.d("OrdersBillingActivity", "Loading existing orders...")
        lifecycleScope.launch {
            try {
                val pendingOrders = database.customerOrderDao().getOrdersByStatus(OrderStatus.PENDING)
                android.util.Log.d("OrdersBillingActivity", "Found ${pendingOrders.size} pending orders")
                
                customerOrders.clear()
                customerOrders.addAll(pendingOrders)
                
                // Log each order for debugging
                pendingOrders.forEachIndexed { index, order ->
                    android.util.Log.d("OrdersBillingActivity", "Order $index: Table '${order.tableName}', Customer Name: '${order.customerName}', Items: ${order.orderItems.size}, Total: ${order.total}")
                }
                
                // Update next customer number
                val maxCustomerNumber = database.customerOrderDao().getMaxCustomerNumber() ?: 0
                nextCustomerNumber = maxCustomerNumber + 1
                android.util.Log.d("OrdersBillingActivity", "Next order number: $nextCustomerNumber")
                
                updateUI()
                
            } catch (e: Exception) {
                android.util.Log.e("OrdersBillingActivity", "Error loading orders: ${e.message}", e)
                Toast.makeText(this@OrdersBillingActivity, "Error loading orders: ${e.message}", Toast.LENGTH_LONG).show()
                updateUI()
            }
        }
    }
    
    private fun updateUI() {
        android.util.Log.d("OrdersBillingActivity", "Updating UI with ${customerOrders.size} orders")
        
        if (customerOrders.isEmpty()) {
            // Show empty state
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewOrders.visibility = android.view.View.GONE
            android.util.Log.d("OrdersBillingActivity", "Showing empty state")
        } else {
            // Show orders
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewOrders.visibility = android.view.View.VISIBLE
            
            // Update adapter
            customerOrderAdapter.submitList(customerOrders.toList())
            android.util.Log.d("OrdersBillingActivity", "Submitted ${customerOrders.size} orders to adapter")
        }
    }
    
    private fun takeNewOrder() {
        val intent = Intent(this, TakeOrderActivity::class.java)
        intent.putExtra(TakeOrderActivity.EXTRA_CUSTOMER_NUMBER, nextCustomerNumber)
        intent.putExtra(TakeOrderActivity.EXTRA_TABLE_NAME, "")
        intent.putExtra(TakeOrderActivity.EXTRA_CUSTOMER_NAME, "")
        
        takeOrderLauncher.launch(intent)
    }
    
    private fun handleOrderResult(data: Intent) {
        val tableName = data.getStringExtra("table_name") ?: ""
        val customerName = data.getStringExtra("customer_name") ?: ""
        val orderItems = data.getSerializableExtra("order_items") as? ArrayList<BillItemDisplay>
        val orderTotal = data.getDoubleExtra("order_total", 0.0)
        val isSaved = data.getBooleanExtra("is_saved", false)

        android.util.Log.d("OrdersBillingActivity", "Order result - Table: '$tableName', Customer Name: '$customerName', Items: ${orderItems?.size}, Total: $orderTotal, Saved: $isSaved")
        
        if (orderItems != null && orderItems.isNotEmpty() && isSaved) {
            // Order was saved, reload from database to get latest state
            android.util.Log.d("OrdersBillingActivity", "Order was saved, refreshing order list...")
            loadExistingOrders()
            
            val displayMessage = if (tableName.isNotEmpty()) {
                "Order saved for Table $tableName!"
            } else {
                val tableInfo = if (tableName.isNotEmpty()) "table $tableName" else "walk-in customer"
                "Order saved for $tableInfo!"
            }
            Toast.makeText(this, displayMessage, Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.d("OrdersBillingActivity", "No order was saved or order was empty")
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
        // Create a simple checkout dialog
        val checkoutMessage = buildString {
            appendLine("Checkout Order")
            if (customerOrder.tableName.isNotEmpty()) {
                appendLine("Table: ${customerOrder.tableName}")
            }
            if (customerOrder.customerName.isNotEmpty()) {
                appendLine("Customer: ${customerOrder.customerName}")
            }
            appendLine("Total: ${rupeeFormatter.format(customerOrder.total)}")
            appendLine()
            appendLine("Proceed with checkout?")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Checkout Order")
            .setMessage(checkoutMessage)
            .setPositiveButton("Checkout") { _, _ ->
                processCheckout(customerOrder)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun processCheckout(customerOrder: CustomerOrder) {
        lifecycleScope.launch {
            try {
                // Update order status to completed
                val updatedOrder = customerOrder.copy(status = OrderStatus.COMPLETED)
                database.customerOrderDao().updateOrder(updatedOrder)
                
                // Remove from local list
                customerOrders.remove(customerOrder)
                
                // Update UI
                updateUI()
                
                Toast.makeText(this@OrdersBillingActivity, "Order checked out successfully!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                android.util.Log.e("OrdersBillingActivity", "Error during checkout: ${e.message}", e)
                Toast.makeText(this@OrdersBillingActivity, "Error during checkout: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh orders when returning to this activity
        loadExistingOrders()
    }
}