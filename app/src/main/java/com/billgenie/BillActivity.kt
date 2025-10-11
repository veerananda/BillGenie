package com.billgenie

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.BillItemsAdapter
import com.billgenie.adapter.MenuSelectionAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityBillBinding
import com.billgenie.model.Bill
import com.billgenie.model.BillItem
import com.billgenie.model.BillItemDisplay
import com.billgenie.model.MenuItem
import com.billgenie.database.BillRepository
import com.billgenie.database.MenuItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class BillActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBillBinding
    private lateinit var menuSelectionAdapter: MenuSelectionAdapter
    private lateinit var billItemsAdapter: BillItemsAdapter
    private lateinit var menuItemRepository: MenuItemRepository
    private lateinit var billRepository: BillRepository
    
    private val selectedMenuItems = mutableListOf<MenuItem>()
    private val billItems = mutableListOf<BillItemDisplay>()
    private var currentBillTotal = 0.0
    
    // Rupee formatter
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDatabase()
        setupRecyclerViews()
        setupSearchFunctionality()
        setupClickListeners()
        loadMenuItems()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Generate Bill"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupDatabase() {
        val database = BillGenieDatabase.getDatabase(this)
        menuItemRepository = MenuItemRepository(database.menuItemDao())
        billRepository = BillRepository(database.billDao(), database.billItemDao())
    }
    
    private fun setupRecyclerViews() {
        // Menu selection RecyclerView
        menuSelectionAdapter = MenuSelectionAdapter(
            onItemSelected = { menuItem ->
                addMenuItemToBill(menuItem)
            }
        )
        
        binding.recyclerViewMenuItems.apply {
            layoutManager = LinearLayoutManager(this@BillActivity)
            adapter = menuSelectionAdapter
        }
        
        // Bill items RecyclerView
        billItemsAdapter = BillItemsAdapter(
            onQuantityChange = { billItem, newQuantity ->
                updateBillItemQuantity(billItem, newQuantity)
            },
            onRemoveClick = { billItem ->
                removeBillItem(billItem)
            }
        )
        
        binding.recyclerViewBillItems.apply {
            layoutManager = LinearLayoutManager(this@BillActivity)
            adapter = billItemsAdapter
        }
    }
    
    private fun setupSearchFunctionality() {
        binding.etItemSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchQuery = s.toString().trim()
                filterMenuItems(searchQuery)
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.btnGenerateBill.setOnClickListener {
            generateBill()
        }
        
        binding.btnClearBill.setOnClickListener {
            clearBill()
        }
    }
    
    private fun loadMenuItems() {
        menuItemRepository.getEnabledMenuItems().observe(this, Observer { menuItems ->
            selectedMenuItems.clear()
            selectedMenuItems.addAll(menuItems.filter { it.isActive && it.isEnabled })
            menuSelectionAdapter.submitList(selectedMenuItems.toList())
        })
    }
    
    private fun filterMenuItems(query: String) {
        val filteredItems = if (query.isEmpty()) {
            selectedMenuItems
        } else {
            selectedMenuItems.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
        menuSelectionAdapter.submitList(filteredItems.toList())
    }
    
    private fun addMenuItemToBill(menuItem: MenuItem) {
        // Check if item already exists in bill
        val existingItem = billItems.find { it.menuItemId == menuItem.id }
        
        if (existingItem != null) {
            // Increase quantity
            updateBillItemQuantity(existingItem, existingItem.quantity + 1)
        } else {
            // Add new item
            val billItem = BillItemDisplay(
                menuItemId = menuItem.id,
                itemName = menuItem.name,
                quantity = 1,
                itemPrice = menuItem.price
            )
            billItems.add(billItem)
            billItemsAdapter.submitList(billItems.toList())
        }
        
        updateBillTotal()
        Toast.makeText(this, "${menuItem.name} added to bill", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateBillItemQuantity(billItem: BillItemDisplay, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeBillItem(billItem)
            return
        }
        
        val index = billItems.indexOf(billItem)
        if (index != -1) {
            val updatedItem = billItem.copy(quantity = newQuantity)
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
        binding.tvBillTotal.text = "Total: ${rupeeFormatter.format(currentBillTotal)}"
        
        // Update bill items count
        val totalItems = billItems.sumOf { it.quantity }
        binding.tvBillItemsCount.text = "$totalItems item${if (totalItems != 1) "s" else ""}"
        
        // Enable/disable generate bill button
        binding.btnGenerateBill.isEnabled = billItems.isNotEmpty()
    }
    
    private fun generateBill() {
        val customerName = binding.etCustomerName.text.toString().trim()
        val customerPhone = binding.etCustomerPhone.text.toString().trim()
        
        // Validate input
        if (customerName.isEmpty()) {
            binding.etCustomerName.error = "Customer name is required"
            return
        }
        
        if (customerPhone.isEmpty()) {
            binding.etCustomerPhone.error = "Customer phone is required"
            return
        }
        
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
                val paymentMethod = when (which) {
                    0 -> "Cash"
                    1 -> "UPI"
                    else -> "Cash"
                }
                finalizeBill(customerName, customerPhone, paymentMethod)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun finalizeBill(customerName: String, customerPhone: String, paymentMethod: String) {
        // Create bill with payment method
        val bill = Bill(
            customerName = customerName,
            customerPhone = customerPhone,
            totalAmount = currentBillTotal,
            paymentMethod = paymentMethod
        )
        
        // Save bill and bill items
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val billId = billRepository.insertBill(bill)
                
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
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BillActivity, "Bill generated successfully with ${paymentMethod} payment!", Toast.LENGTH_LONG).show()
                    showBillSummary(bill, billItems)
                    clearBill()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BillActivity, "Error generating bill: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showBillSummary(bill: Bill, billItems: List<BillItemDisplay>) {
        val paymentIcon = if (bill.paymentMethod == "UPI") "📱" else "💵"
        val summary = buildString {
            appendLine("BILL GENERATED")
            appendLine("═══════════════")
            appendLine("Customer: ${bill.customerName}")
            appendLine("Phone: ${bill.customerPhone}")
            appendLine("Date: ${bill.dateCreated}")
            appendLine("Payment: $paymentIcon ${bill.paymentMethod}")
            appendLine()
            appendLine("ITEMS:")
            appendLine("───────────────")
            billItems.forEach { item ->
                val itemTotal = item.quantity * item.itemPrice
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
        binding.etCustomerPhone.text?.clear()
        binding.etItemSearch.text?.clear()
        updateBillTotal()
        Toast.makeText(this, "Bill cleared", Toast.LENGTH_SHORT).show()
    }
}