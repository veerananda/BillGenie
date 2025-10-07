package com.billgenie

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.FinalBillItemsAdapter
import com.billgenie.databinding.ActivityFinalBillBinding
import com.billgenie.model.CustomerOrder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FinalBillActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFinalBillBinding
    private lateinit var finalBillItemsAdapter: FinalBillItemsAdapter
    
    private var customerOrder: CustomerOrder? = null
    private var discountAmount: Double = 0.0
    private var finalTotal: Double = 0.0
    private var paymentMethod: String = ""
    private var amountPaid: Double = 0.0
    private var changeAmount: Double = 0.0
    
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinalBillBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        loadBillData()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Bill Generated"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        finalBillItemsAdapter = FinalBillItemsAdapter()
        binding.recyclerViewBillItems.apply {
            layoutManager = LinearLayoutManager(this@FinalBillActivity)
            adapter = finalBillItemsAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.btnDone.setOnClickListener {
            // Return to main orders activity
            val intent = Intent(this, OrdersBillingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
    
    private fun loadBillData() {
        // Get data from intent
        customerOrder = intent.getSerializableExtra("customer_order") as? CustomerOrder
        discountAmount = intent.getDoubleExtra("discount_amount", 0.0)
        finalTotal = intent.getDoubleExtra("final_total", 0.0)
        paymentMethod = intent.getStringExtra("payment_method") ?: ""
        amountPaid = intent.getDoubleExtra("amount_paid", 0.0)
        changeAmount = intent.getDoubleExtra("change_amount", 0.0)
        
        customerOrder?.let { order ->
            displayBillHeader(order)
            displayBillItems(order)
            displayBillSummary(order)
            displayPaymentInfo()
        } ?: run {
            Toast.makeText(this, "Error loading bill data", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun displayBillHeader(order: CustomerOrder) {
        binding.tvBillNumber.text = "Bill #${order.customerNumber}"
        binding.tvBillDate.text = dateFormatter.format(Date())
        
        if (order.tableName.isNotEmpty()) {
            binding.tvTableInfo.text = "Table: ${order.tableName}"
        } else {
            binding.tvTableInfo.text = "Table: ${order.customerNumber}"
        }
        
        if (order.customerName.isNotEmpty()) {
            binding.tvCustomerName.text = "Customer: ${order.customerName}"
        } else {
            binding.tvCustomerName.text = "Walk-in Customer"
        }
    }
    
    private fun displayBillItems(order: CustomerOrder) {
        finalBillItemsAdapter.submitList(order.orderItems)
        
        val itemCount = order.orderItems.sumOf { it.quantity }
        binding.tvItemCount.text = "Total Items: $itemCount"
    }
    
    private fun displayBillSummary(order: CustomerOrder) {
        binding.tvSubtotalAmount.text = rupeeFormatter.format(order.total)
        
        if (discountAmount > 0) {
            binding.layoutDiscount.visibility = android.view.View.VISIBLE
            binding.tvDiscountAmount.text = "- ${rupeeFormatter.format(discountAmount)}"
        } else {
            binding.layoutDiscount.visibility = android.view.View.GONE
        }
        
        binding.tvFinalAmount.text = rupeeFormatter.format(finalTotal)
    }
    
    private fun displayPaymentInfo() {
        binding.tvPaymentMethod.text = "Payment Method: $paymentMethod"
        binding.tvAmountPaid.text = "Amount Paid: ${rupeeFormatter.format(amountPaid)}"
        
        if (changeAmount > 0) {
            binding.layoutChange.visibility = android.view.View.VISIBLE
            binding.tvChangeAmount.text = "Change Returned: ${rupeeFormatter.format(changeAmount)}"
        } else {
            binding.layoutChange.visibility = android.view.View.GONE
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_final_bill, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_bill -> {
                shareBill()
                true
            }
            R.id.action_print_bill -> {
                printBill()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun shareBill() {
        val billText = generateBillText()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, billText)
            putExtra(Intent.EXTRA_SUBJECT, "Bill from BillGenie Restaurant")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Bill"))
    }
    
    private fun printBill() {
        // For now, just show a toast. In a real app, you would integrate with a thermal printer
        Toast.makeText(this, "Print functionality will be implemented with thermal printer integration", Toast.LENGTH_LONG).show()
    }
    
    private fun generateBillText(): String {
        val order = customerOrder ?: return ""
        
        return buildString {
            appendLine("═══════════════════════════")
            appendLine("     BILLGENIE RESTAURANT")
            appendLine("═══════════════════════════")
            appendLine()
            appendLine("Bill #${order.customerNumber}")
            appendLine("Date: ${dateFormatter.format(Date())}")
            if (order.tableName.isNotEmpty()) {
                appendLine("Table: ${order.tableName}")
            }
            if (order.customerName.isNotEmpty()) {
                appendLine("Customer: ${order.customerName}")
            }
            appendLine()
            appendLine("ITEMS:")
            appendLine("─────────────────────────────")
            
            order.orderItems.forEach { item ->
                appendLine("${item.itemName}")
                appendLine("  ${item.quantity} x ${rupeeFormatter.format(item.itemPrice)} = ${rupeeFormatter.format(item.totalPrice)}")
            }
            
            appendLine("─────────────────────────────")
            appendLine("Subtotal: ${rupeeFormatter.format(order.total)}")
            
            if (discountAmount > 0) {
                appendLine("Discount: -${rupeeFormatter.format(discountAmount)}")
            }
            
            appendLine("═══════════════════════════")
            appendLine("TOTAL: ${rupeeFormatter.format(finalTotal)}")
            appendLine("═══════════════════════════")
            appendLine()
            appendLine("Payment Method: $paymentMethod")
            appendLine("Amount Paid: ${rupeeFormatter.format(amountPaid)}")
            
            if (changeAmount > 0) {
                appendLine("Change Returned: ${rupeeFormatter.format(changeAmount)}")
            }
            
            appendLine()
            appendLine("Thank you for dining with us!")
            appendLine("═══════════════════════════")
        }
    }
}